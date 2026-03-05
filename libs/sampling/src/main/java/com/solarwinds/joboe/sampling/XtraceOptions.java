/*
 * © SolarWinds Worldwide, LLC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.solarwinds.joboe.sampling;

import com.solarwinds.joboe.logging.Logger;
import com.solarwinds.joboe.logging.LoggerFactory;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.Getter;

/**
 * Represents the data model from the `X-Trace-Options` request header.
 *
 * <p>Provides a static method to construct the {@link XtraceOptions} instance by parsing and
 * authenticating the `X-Trace-Options` with signature check {@link SignatureAuthenticator}(Current
 * authentication uses HMAC-SHA1). Authentication status and exceptions associated with the
 * operation are also exposed on the result instance.
 *
 * <p>Provides a method to look up {@link XtraceOption} with the corresponding typed value.
 */
public class XtraceOptions {
  private static final Logger logger = LoggerFactory.getLogger();
  static final long TIMESTAMP_MAX_DELTA = 5 * 60; // 5 minutes in seconds
  private static SignatureAuthenticator authenticator;

  private final Map<XtraceOption<?>, ?> options;
  static final String ENTRY_SEPARATOR = ";";
  static final String KEY_VALUE_SEPARATOR = "=";

  /**
   * -- GETTER -- Gets the exceptions occurred during the construction of XTraceOptions by
   *
   * @return
   */
  @Getter private final List<XTraceOptionException> exceptions;

  /**
   * -- GETTER -- Gets the authentication status after invocation of
   *
   * @return
   */
  @Getter private final AuthenticationStatus authenticationStatus;

  static {
    SettingsManager.registerListener(
        new SettingsArgChangeListener<byte[]>(SettingsArg.TRACE_OPTIONS_SECRET) {
          @Override
          public void onChange(byte[] newValue) {
            if (newValue != null) {
              authenticator = new HmacSignatureAuthenticator(newValue);
            } else {
              authenticator = null; // remove the existing authenticator
            }
          }
        });
  }

  XtraceOptions(
      Map<XtraceOption<?>, ?> options,
      List<XTraceOptionException> exceptions,
      AuthenticationStatus authenticationStatus) {
    this.options = options;
    this.exceptions = exceptions;
    this.authenticationStatus = authenticationStatus;
  }

  /**
   * Extracts XTraceOptions by parsing the `traceOptionString`.
   *
   * <p>If `traceOptionsSignature` is provided, then authenticates the options with the
   * authenticator
   *
   * @param traceOptionsString
   * @param traceOptionsSignature
   * @return An XTraceOptions instance after the parsing and authentication; null if
   *     `traceOptionsString` is null. Take note that any parsing or authentication failure will be
   *     recorded in the returning instance and can be extracted by {@link
   *     XtraceOptions#getAuthenticationStatus()} and {@link XtraceOptions#getExceptions()} methods.
   */
  public static XtraceOptions getXTraceOptions(
      String traceOptionsString, String traceOptionsSignature) {
    return getXTraceOptions(traceOptionsString, traceOptionsSignature, authenticator);
  }

  static XtraceOptions getXTraceOptions(
      String traceOptionsString,
      String traceOptionsSignature,
      SignatureAuthenticator authenticator) {
    if (traceOptionsString == null) {
      return null;
    }

    List<XTraceOptionException> exceptions = new ArrayList<XTraceOptionException>();

    Map<XtraceOption<?>, Object> options = new LinkedHashMap<XtraceOption<?>, Object>();
    for (String optionEntry : traceOptionsString.split(ENTRY_SEPARATOR)) {
      optionEntry = optionEntry.trim();
      int separatorIndex = optionEntry.indexOf(KEY_VALUE_SEPARATOR);
      String optionKey;
      if (separatorIndex >= 0) {
        optionKey = optionEntry.substring(0, separatorIndex);
      } else { // check whether it is key only option
        optionKey = optionEntry;
      }

      optionKey = optionKey.trim();

      if (optionKey.isEmpty()) { // skip empty key
        if (!optionEntry.isEmpty()) {
          logger.debug(
              "Skipped entry [" + optionEntry + "] in X-Trace-Options as the key is empty");
        }
        continue;
      }

      XtraceOption<?> option = XtraceOption.fromKey(optionKey);
      if (option != null) {
        if (option.isKeyOnlyOption()) {
          if (separatorIndex > 0) { // do not allow key only option with a value
            exceptions.add(new InvalidFormatXTraceOptionException(option, optionEntry));
          } else {
            if (!options.containsKey(option)) {
              options.put(option, true);
            } else {
              logger.debug(
                  "Duplicated option ["
                      + option.getKey()
                      + "] found in X-Trace-Options, ignoring...");
            }
          }
        } else {
          if (separatorIndex < 0) {
            exceptions.add(new InvalidFormatXTraceOptionException(option, optionEntry));
          }
          String optionValueString = optionEntry.substring(separatorIndex + 1).trim();
          try {
            if (!options.containsKey(option)) {
              options.put(option, option.parseValueFromString(optionValueString));
            } else {
              logger.debug(
                  "Duplicated option ["
                      + option.getKey()
                      + "] with value ["
                      + optionValueString
                      + "] found in X-Trace-Options, ignoring...");
            }
          } catch (InvalidValueXTraceOptionException e) {
            exceptions.add(e);
          }
        }
      } else {
        exceptions.add(new UnknownXTraceOptionException(optionKey));
      }
    }

    // authenticate
    AuthenticationStatus authenticationStatus =
        authenticate(
            traceOptionsString,
            (Long) options.get(XtraceOption.TS),
            traceOptionsSignature,
            authenticator);

    if (authenticationStatus.isFailure()) { // if authentication failed, ignore all xtrace options
      return new XtraceOptions(
          Collections.emptyMap(), Collections.emptyList(), authenticationStatus);
    } else {
      for (XTraceOptionException exception : exceptions) {
        logger.debug(exception.getMessage());
      }
      return new XtraceOptions(options, exceptions, authenticationStatus);
    }
  }

  /**
   * Authenticates the `optionString` with:
   *
   * <ol>
   *   <li>Check if the `timestamp` provided is within the accepted time range (iff
   *       `traceOptionsSignature` is non null
   *   <li>Authenticate the `optionString` and `traceOptionsSignature` with the provided signature
   *       `authenticator`
   * </ol>
   *
   * Returns the authentication status based on the provided parameters
   *
   * @param optionsString
   * @param timestamp
   * @param traceOptionsSignature
   * @param authenticator
   * @return {@link AuthenticationStatus#NOT_AUTHENTICATED} if `traceOptionsSignature` is null;
   *     otherwise the result of the authentication
   */
  static AuthenticationStatus authenticate(
      String optionsString,
      Long timestamp,
      String traceOptionsSignature,
      SignatureAuthenticator authenticator) {
    if (traceOptionsSignature == null) {
      return AuthenticationStatus.NOT_AUTHENTICATED;
    }

    if (timestamp == null) {
      return AuthenticationStatus.failure("bad-timestamp");
    }
    if (!isTimestampWithRange(timestamp)) {
      return AuthenticationStatus.failure("bad-timestamp");
    }

    if (authenticator == null) {
      return AuthenticationStatus.failure("authenticator-unavailable");
    } else {
      if (authenticator.authenticate(optionsString, traceOptionsSignature)) {
        return AuthenticationStatus.OK;
      } else {
        return AuthenticationStatus.failure("bad-signature");
      }
    }
  }

  private static boolean isTimestampWithRange(long timestamp) {
    long currentTimeInSeconds = System.currentTimeMillis() / 1000;
    return timestamp >= currentTimeInSeconds - TIMESTAMP_MAX_DELTA
        && timestamp <= currentTimeInSeconds + TIMESTAMP_MAX_DELTA;
  }

  /**
   * Gets the value of the option. If it's not defined then the default value of the option will be
   * returned.
   *
   * <p>Take note that default value could be null too.
   *
   * @param option
   * @param <T>
   * @return
   */
  @SuppressWarnings("unchecked")
  public <T> T getOptionValue(XtraceOption<T> option) {
    return options.containsKey(option) ? (T) options.get(option) : option.getDefaultValue();
  }

  /**
   * Gets the custom KVs X-Trace-Options with key that starts with {@link
   * XtraceOption#CUSTOM_KV_PREFIX}
   *
   * @return
   */
  @SuppressWarnings("unchecked")
  public Map<XtraceOption<String>, String> getCustomKvs() {
    Map<XtraceOption<String>, String> customKvs = new LinkedHashMap<XtraceOption<String>, String>();
    for (Map.Entry<XtraceOption<?>, ?> entry : options.entrySet()) {
      XtraceOption<?> option = entry.getKey();
      if (option.isCustomKv()) {
        customKvs.put((XtraceOption<String>) option, (String) entry.getValue());
      }
    }
    return customKvs;
  }

  public abstract static class XTraceOptionException extends Exception {
    private static final long serialVersionUID = 1L;

    XTraceOptionException(String message, Exception cause) {
      super(message, cause);
    }

    XTraceOptionException(String message) {
      super(message);
    }

    abstract void appendToResponse(XTraceOptionsResponse response);
  }

  @Getter
  public abstract static class InvalidXTraceOptionException extends XTraceOptionException {
    private static final long serialVersionUID = 1L;
    protected String invalidOptionKey;
    private static final String RESPONSE_KEY = "ignored";

    protected InvalidXTraceOptionException(String invalidOptionKey, String message) {
      super(message);
      this.invalidOptionKey = invalidOptionKey;
    }

    @Override
    void appendToResponse(XTraceOptionsResponse response) {
      String existingIgnoredOptions = response.getValue(RESPONSE_KEY);
      String newIgnoredOptions;
      if (existingIgnoredOptions == null) {
        newIgnoredOptions = invalidOptionKey;
      } else {
        newIgnoredOptions = existingIgnoredOptions + "," + invalidOptionKey;
      }

      response.setValue(RESPONSE_KEY, newIgnoredOptions);
    }
  }

  /** The X-Trace-Options key is unknown */
  static class UnknownXTraceOptionException extends InvalidXTraceOptionException {
    private static final long serialVersionUID = 1L;

    UnknownXTraceOptionException(String unknownOptionkey) {
      super(unknownOptionkey, "Unknown key " + unknownOptionkey + " in X-Trace-Options header");
    }
  }

  /** The x-trace-option value is not the expected type/value */
  public static class InvalidValueXTraceOptionException extends InvalidXTraceOptionException {
    private static final long serialVersionUID = 1L;

    InvalidValueXTraceOptionException(XtraceOption<?> optionKey, String invalidValue) {
      super(
          optionKey.getKey(),
          "Invalid value ["
              + invalidValue
              + "] for option ["
              + optionKey.getKey()
              + "] in X-Trace-Options header");
    }
  }

  /**
   * The x-trace-option entry is in invalid format, for example it expects a key/value but the
   * separator cannot be found
   */
  static class InvalidFormatXTraceOptionException extends InvalidXTraceOptionException {
    private static final long serialVersionUID = 1L;

    InvalidFormatXTraceOptionException(XtraceOption<?> optionKey, String entry) {
      super(
          optionKey.getKey(),
          "Invalid format for option entry [" + entry + "] in X-Trace-Options header");
    }
  }

  @Getter
  public static class AuthenticationStatus {
    public static final AuthenticationStatus OK = new AuthenticationStatus(false, true, null);
    public static final AuthenticationStatus NOT_AUTHENTICATED =
        new AuthenticationStatus(false, false, null);

    /**
     * -- GETTER -- Gets the reason of the authentication failure. If the authentication was
     * successfully or no authentication is done, then this will be null
     *
     * @return
     */
    private final String reason;

    /**
     * -- GETTER -- Whether there is failure during the authentication. Take note that if no
     * authentication was taken place (for example no signature), then this will be `false`
     *
     * @return
     */
    private final boolean failure;

    /**
     * -- GETTER -- Whether the request is authenticated
     *
     * @return
     */
    private final boolean authenticated;

    private AuthenticationStatus(boolean failure, boolean authenticated, String reason) {
      this.failure = failure;
      this.authenticated = authenticated;
      this.reason = reason;
    }

    public static AuthenticationStatus failure(String reason) {
      return new AuthenticationStatus(true, false, reason);
    }

    @Override
    public String toString() {
      return "AuthenticationStatus{"
          + "reason='"
          + reason
          + '\''
          + ", failure="
          + failure
          + ", authenticated="
          + authenticated
          + '}';
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      AuthenticationStatus that = (AuthenticationStatus) o;

      if (failure != that.failure) return false;
      if (authenticated != that.authenticated) return false;
      return Objects.equals(reason, that.reason);
    }

    @Override
    public int hashCode() {
      int result = reason != null ? reason.hashCode() : 0;
      result = 31 * result + (failure ? 1 : 0);
      result = 31 * result + (authenticated ? 1 : 0);
      return result;
    }
  }

  interface SignatureAuthenticator {
    boolean authenticate(String optionsString, String signature);
  }

  static class HmacSignatureAuthenticator implements SignatureAuthenticator {
    private final Mac mac;

    HmacSignatureAuthenticator(byte[] secret) {
      mac = getMac(secret);
    }

    private Mac getMac(byte[] secret) {
      SecretKeySpec signingKey = new SecretKeySpec(secret, "HMACSHA1");
      try {
        Mac mac = Mac.getInstance("HMACSHA1");
        mac.init(signingKey);

        return mac;
      } catch (GeneralSecurityException e) {
        logger.warn("Failed to initialize HMAC for x-trace options: " + e.getMessage(), e);
        return null;
      }
    }

    @Override
    public boolean authenticate(String optionsString, String signature) {
      byte[] rawHmac = mac.doFinal(optionsString.getBytes());
      String expectedSignature = HexUtils.bytesToHex(rawHmac).toLowerCase();
      return expectedSignature.equals(signature);
    }
  }
}
