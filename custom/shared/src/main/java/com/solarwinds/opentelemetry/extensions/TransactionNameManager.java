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

package com.solarwinds.opentelemetry.extensions;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.solarwinds.joboe.config.ConfigManager;
import com.solarwinds.joboe.config.ConfigProperty;
import com.solarwinds.joboe.logging.Logger;
import com.solarwinds.joboe.logging.LoggerFactory;
import com.solarwinds.joboe.sampling.SettingsArg;
import com.solarwinds.joboe.sampling.SettingsArgChangeListener;
import com.solarwinds.joboe.sampling.SettingsManager;
import com.solarwinds.opentelemetry.core.CustomTransactionNameDict;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.semconv.HttpAttributes;
import io.opentelemetry.semconv.UrlAttributes;
import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TransactionNameManager {
  private static final Logger logger = LoggerFactory.getLogger();
  private static final String[] DEFAULT_TRANSACTION_NAME_PATTERN = {"p1", "p2"};
  private static final String CUSTOM_TRANSACTION_NAME_PATTERN_SEPARATOR = ".";
  private static final String DEFAULT_TRANSACTION_NAME_PATTERN_SEPARATOR = "/";
  public static final String OVER_LIMIT_TRANSACTION_NAME = "other";
  public static final String UNKNOWN_TRANSACTION_NAME = "unknown";
  public static final int DEFAULT_MAX_NAME_COUNT = 200;
  public static final int MAX_TRANSACTION_NAME_LENGTH = 255;
  public static final String TRANSACTION_NAME_ELLIPSIS = "...";

  private static String[] customTransactionNamePattern = null;
  static final Cache<String, String> URL_TRANSACTION_NAME_CACHE =
      Caffeine.newBuilder().maximumSize(1000).expireAfterWrite(Duration.ofMinutes(20)).build();

  private static final Set<String> EXISTING_TRANSACTION_NAMES = new HashSet<>();
  private static boolean limitExceeded;
  private static int maxNameCount = DEFAULT_MAX_NAME_COUNT;

  private static NamingScheme namingScheme = new DefaultNamingScheme(null);

  static {
    addNameCountChangeListener();
  }

  private TransactionNameManager() { // forbid instantiation
  }

  public static void setNamingScheme(NamingScheme namingScheme) {
    TransactionNameManager.namingScheme = namingScheme;
  }

  public static NamingScheme getNamingScheme() {
    return namingScheme;
  }

  private static void addNameCountChangeListener() {
    SettingsManager.registerListener(
        new SettingsArgChangeListener<Integer>(SettingsArg.MAX_TRANSACTIONS) {
          @Override
          public void onChange(Integer newValue) {
            if (newValue != null) {
              if (newValue > maxNameCount) {
                limitExceeded = false; // reset the exceed flag
              }
              maxNameCount = newValue;
            } else { // reset to default
              maxNameCount = DEFAULT_MAX_NAME_COUNT;
            }
          }
        });
  }

  private static String[] getTransactionNamePattern() {
    String pattern =
        ConfigManager.getConfigOptional(ConfigProperty.AGENT_TRANSACTION_NAME_PATTERN, null);
    return pattern != null ? parseTransactionNamePattern(pattern) : null;
  }

  static String[] parseTransactionNamePattern(String pattern) {
    String[] tokens = pattern.split(",");
    for (int i = 0; i < tokens.length; i++) {
      tokens[i] = tokens[i].trim();
    }
    return tokens;
  }

  /**
   * Gets a transaction name based on information provided in a span and
   * domainPrefixedTransactionName flag, the result will be recorded if not null.
   *
   * <p>If more than <code>MAX_NAME_COUNT</code> transaction name is recorded, "other" will be
   * returned. If the logic fails to extract a transaction from the given span, "unknown" will be
   * returned.
   *
   * @param spanData {@link SpanData}
   * @return transaction name
   */
  public static String getTransactionName(SpanData spanData) {
    String transactionName = buildTransactionName(spanData);

    if (transactionName != null) {
      transactionName = truncateTransactionName(transactionName);
      return addTransactionName(transactionName)
          ? transactionName
          : OVER_LIMIT_TRANSACTION_NAME; // check the transaction name limit;
    } else {
      return UNKNOWN_TRANSACTION_NAME; // unable to build the transaction name
    }
  }

  /**
   * Transform the transaction name according to <a
   * href="https://github.com/librato/gotv/blob/376240c5fcce883f37a5358cb30ac39ab9283c7e/collector/agentmetrics/tags.go#L41-L52">...</a>
   * and <a
   * href="https://github.com/librato/jackdaw/blob/0930023a2d30dc42e58ed45cc05df9b46e2b7da1/src/main/java/com/librato/jackdaw/ingress/IngressMeasurement.java#L28">...</a>
   *
   * @param inputTransactionName raw transaction name
   * @return refined transaction name
   */
  static String truncateTransactionName(String inputTransactionName) {
    String transactionName = inputTransactionName;
    if (transactionName.length() > MAX_TRANSACTION_NAME_LENGTH) {
      transactionName =
          transactionName.substring(
                  0, MAX_TRANSACTION_NAME_LENGTH - TRANSACTION_NAME_ELLIPSIS.length())
              + TRANSACTION_NAME_ELLIPSIS;
    }

    if (!transactionName.equalsIgnoreCase(inputTransactionName)) {
      logger.debug(
          "Transaction name ["
              + inputTransactionName
              + "] was truncated to ["
              + transactionName
              + "] because it exceeds "
              + MAX_TRANSACTION_NAME_LENGTH
              + " characters.");
    }

    return transactionName;
  }

  static String buildTransactionName(SpanData spanData) {
    Attributes spanAttributes = spanData.getAttributes();
    String custName = CustomTransactionNameDict.get(spanData.getTraceId());

    if (custName != null) {
      logger.trace(String.format("Using custom transaction name -> %s", custName));
      return custName;
    }

    String name = namingScheme.createName(spanAttributes);
    if (name != null && !name.isEmpty()) {
      logger.trace(String.format("Using scheme derived transaction name -> %s", name));
      return name;
    }

    // use HandlerName which may be injected by some MVC instrumentations (currently only Spring
    // MVC)
    String handlerName = spanAttributes.get(AttributeKey.stringKey("HandlerName"));
    if (handlerName != null) {
      logger.trace(String.format("Using HandlerName(%s) as the transaction name", handlerName));
      return handlerName;
    }

    // use "http.route"
    String httpRoute = spanAttributes.get(HttpAttributes.HTTP_ROUTE);
    if (httpRoute != null) {
      logger.trace(String.format("Using http.route (%s) as the transaction name", httpRoute));
      return httpRoute;
    }

    // get transaction name from url
    String path = spanAttributes.get(UrlAttributes.URL_PATH);

    if (customTransactionNamePattern == null) {
      customTransactionNamePattern = getTransactionNamePattern();
    }

    if (customTransactionNamePattern
        != null) { // try forming transaction name by the custom configured pattern
      String transactionName =
          getTransactionNameByUrlAndPattern(
              path, customTransactionNamePattern, false, CUSTOM_TRANSACTION_NAME_PATTERN_SEPARATOR);

      if (transactionName != null) {
        logger.trace(
            String.format(
                "Using custom configure pattern to extract transaction name: (%s)",
                transactionName));
        return transactionName;
      }
    }

    // try the default token name pattern
    String transactionNameByUrl =
        getTransactionNameByUrlAndPattern(
            path,
            DEFAULT_TRANSACTION_NAME_PATTERN,
            true,
            DEFAULT_TRANSACTION_NAME_PATTERN_SEPARATOR);
    if (transactionNameByUrl != null) {
      logger.trace(
          String.format(
              "Using token name pattern to extract transaction name: (%s)", transactionNameByUrl));
      return transactionNameByUrl;
    }

    String spanName = spanData.getName();
    logger.trace(String.format("Using span name as the transaction name: (%s)", spanName));
    return spanName;
  }

  /**
   * Gets transaction name based on host, URL and provided name pattern. It might look up and update
   * the urlTransactionNameCache
   *
   * @param url url that must NOT contains query param
   * @param transactionNamePattern pattern used for extracting transaction name
   * @return extracted transaction name
   */
  static String getTransactionNameByUrlAndPattern(
      String url, String[] transactionNamePattern, boolean separatorAsPrefix, String separator) {
    if (url == null) {
      return null;
    }

    String transactionName = URL_TRANSACTION_NAME_CACHE.getIfPresent(url);
    if (transactionName == null) {
      transactionName =
          buildTransactionNameByUrlAndPattern(
              url, transactionNamePattern, separatorAsPrefix, separator);
      URL_TRANSACTION_NAME_CACHE.put(url, transactionName);
    }

    return transactionName;
  }

  /**
   * Generates a transaction name by concatenating matching pattern tokens with '.'
   *
   * <p>The valid tokens are host and p1, p2, ... pn. For example a token list of ["host", "p2"] on
   * URL <a href="http://localhost:8080/test-api/action/1">...</a>, will generate transaction name
   * "localhost.action"
   *
   * @param url url that must NOT contains query param
   * @param transactionNamePattern the token pattern as an array
   * @param separator separator token
   * @param separatorAsPrefix whether to use separator as prefix
   * @return formed transaction name
   */
  static String buildTransactionNameByUrlAndPattern(
      String url, String[] transactionNamePattern, boolean separatorAsPrefix, String separator) {
    Map<String, String> urlTokenMap = new HashMap<>();
    int counter = 1;
    for (String token : url.split("/")) {
      if (!token.isEmpty()) {
        String tokenName = "p" + counter++;
        urlTokenMap.put(tokenName, token);
      }
    }

    StringBuilder transactionName = new StringBuilder(separatorAsPrefix ? separator : "");
    boolean isFirstToken = true;
    for (String patternToken : transactionNamePattern) {
      if (urlTokenMap.containsKey(patternToken)) {
        if (isFirstToken) {
          transactionName.append(urlTokenMap.get(patternToken));
          isFirstToken = false;
        } else {
          transactionName.append(separator).append(urlTokenMap.get(patternToken));
        }
      }
    }
    return transactionName.toString();
  }

  /**
   * Adds a transaction name to the tracking set
   *
   * @param transactionName the name to be added, should NOT be null
   * @return true if transactionName is already existed or added successfully; false otherwise
   *     (limit exceeded)
   */
  public static boolean addTransactionName(String transactionName) {
    synchronized (EXISTING_TRANSACTION_NAMES) {
      if (!EXISTING_TRANSACTION_NAMES.contains(transactionName)) {
        if (EXISTING_TRANSACTION_NAMES.size() < maxNameCount) {
          EXISTING_TRANSACTION_NAMES.add(transactionName);
          return true;
        }
      } else { // the name already exists, so it's not over the limit
        return true;
      }
    }
    limitExceeded = true; // toggle the flag
    return false;
  }

  @SuppressWarnings("unused")
  public static boolean isLimitExceeded() {
    return limitExceeded;
  }

  public static void clearTransactionNames() {
    logger.trace(
        String.format(
            "Clearing transaction name buffer. Unique transaction count: %d. Note: This log line is used for validation",
            EXISTING_TRANSACTION_NAMES.size()));
    synchronized (EXISTING_TRANSACTION_NAMES) {
      EXISTING_TRANSACTION_NAMES.clear();

      limitExceeded = false;
    }
  }

  /** For internal testing usage only */
  @SuppressWarnings("unused")
  static void reset() {
    clearTransactionNames();
    URL_TRANSACTION_NAME_CACHE.invalidateAll();
    maxNameCount = DEFAULT_MAX_NAME_COUNT;
  }
}
