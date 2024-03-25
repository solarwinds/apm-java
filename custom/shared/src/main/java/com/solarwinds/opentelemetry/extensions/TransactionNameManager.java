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
import io.opentelemetry.semconv.SemanticAttributes;
import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.Value;

public class TransactionNameManager {
  private static final Logger logger = LoggerFactory.getLogger();
  private static final String[] DEFAULT_TRANSACTION_NAME_PATTERN = {"p1", "p2"};
  private static final String CUSTOM_TRANSACTION_NAME_PATTERN_SEPARATOR = ".";
  private static final String DEFAULT_TRANSACTION_NAME_PATTERN_SEPARATOR = "/";
  private static final String DOMAIN_PREFIX_SEPARATOR = "/";
  public static final String OVER_LIMIT_TRANSACTION_NAME = "other";
  public static final String UNKNOWN_TRANSACTION_NAME = "unknown";
  public static final int DEFAULT_MAX_NAME_COUNT = 200;
  public static final int MAX_TRANSACTION_NAME_LENGTH = 255;
  public static final String TRANSACTION_NAME_ELLIPSIS = "...";
  public static final Pattern REPLACE_PATTERN = Pattern.compile("[^-.:_\\\\/\\w? ]");

  private static final String[] customTransactionNamePattern;
  static final Cache<String, String> URL_TRANSACTION_NAME_CACHE =
      Caffeine.newBuilder().maximumSize(1000).expireAfterWrite(Duration.ofMinutes(20)).build();

  private static final Set<String> EXISTING_TRANSACTION_NAMES = new HashSet<>();
  private static boolean limitExceeded;
  private static int maxNameCount = DEFAULT_MAX_NAME_COUNT;

  private static NamingScheme namingScheme = new DefaultNamingScheme(null);

  static {
    customTransactionNamePattern = getTransactionNamePattern();
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
    TransactionResult transactionResult =
        buildTransactionName(spanData.getName(), spanData.getAttributes());
    String transactionName = transactionResult.name;

    if (transactionResult.isCustom()) {
      return transactionName;
    }

    if (transactionName != null) {
      Boolean domainPrefixedTransactionName =
          ConfigManager.getConfigOptional(
              ConfigProperty.AGENT_DOMAIN_PREFIXED_TRANSACTION_NAME, false);
      if (domainPrefixedTransactionName) {
        transactionName = prefixTransactionNameWithDomainName(transactionName, spanData);
      }

      transactionName = transformTransactionName(transactionName);
      return addTransactionName(transactionName)
          ? transactionName
          : OVER_LIMIT_TRANSACTION_NAME; // check the transaction name limit;
    } else {
      return UNKNOWN_TRANSACTION_NAME; // unable to build the transaction name
    }
  }

  private static String prefixTransactionNameWithDomainName(
      String transactionName, SpanData spanData) {
    String httpHostValue = spanData.getAttributes().get(SemanticAttributes.SERVER_ADDRESS);
    if (httpHostValue != null && !httpHostValue.isEmpty()) {
      if (transactionName.startsWith("/")) {
        return httpHostValue + transactionName;
      } else {
        return httpHostValue + DOMAIN_PREFIX_SEPARATOR + transactionName;
      }
    }

    return transactionName;
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
  static String transformTransactionName(String inputTransactionName) {
    String transactionName = inputTransactionName;

    if (transactionName.length() > MAX_TRANSACTION_NAME_LENGTH) {
      transactionName =
          transactionName.substring(
                  0, MAX_TRANSACTION_NAME_LENGTH - TRANSACTION_NAME_ELLIPSIS.length())
              + TRANSACTION_NAME_ELLIPSIS;
    } else if (transactionName.isEmpty()) {
      transactionName = " "; // ensure that it at least has 1 character
    }

    transactionName = REPLACE_PATTERN.matcher(transactionName).replaceAll("_");

    transactionName = transactionName.toLowerCase();

    if (!transactionName.equalsIgnoreCase(inputTransactionName)) {
      logger.debug(
          "Transaction name ["
              + inputTransactionName
              + "] has been transformed to ["
              + transactionName
              + "]");
    }

    return transactionName;
  }

  static TransactionResult buildTransactionName(String spanName, Attributes spanAttributes) {

    String customName = spanAttributes.get(AttributeKey.stringKey("sw.transaction"));
    if (customName != null) {
      logger.trace(String.format("Using custom transaction name -> %s", customName));
      return new TransactionResult(customName, true);
    }

    String name = namingScheme.createName(spanAttributes);
    if (name != null && !name.isEmpty()) {
      logger.trace(String.format("Using scheme derived transaction name -> %s", name));
      return new TransactionResult(name, false);
    }

    String path = spanAttributes.get(SemanticAttributes.URL_PATH);
    // use HandlerName which may be injected by some MVC instrumentations (currently only Spring
    // MVC)
    String handlerName = spanAttributes.get(AttributeKey.stringKey("HandlerName"));
    if (handlerName != null) {
      logger.trace(String.format("Using HandlerName(%s) as the transaction name", handlerName));
      return new TransactionResult(handlerName, false);
    }

    // use "http.route"
    String httpRoute = spanAttributes.get(SemanticAttributes.HTTP_ROUTE);
    if (httpRoute != null) {
      logger.trace(String.format("Using http.route (%s) as the transaction name", httpRoute));
      return new TransactionResult(httpRoute, false);
    }

    // get transaction name from url
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
        return new TransactionResult(transactionName, false);
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
      return new TransactionResult(transactionNameByUrl, false);
    }

    logger.trace(String.format("Using span name as the transaction name: (%s)", spanName));
    return new TransactionResult(spanName, false);
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

  @Value
  @RequiredArgsConstructor
  static class TransactionResult {
    String name;
    boolean custom;
  }
}
