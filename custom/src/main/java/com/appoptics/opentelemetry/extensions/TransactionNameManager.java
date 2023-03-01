package com.appoptics.opentelemetry.extensions;

import com.appoptics.opentelemetry.core.CustomTransactionNameDict;
import com.appoptics.opentelemetry.core.Util;
import com.tracelytics.ext.google.common.cache.Cache;
import com.tracelytics.ext.google.common.cache.CacheBuilder;
import com.tracelytics.joboe.config.ConfigManager;
import com.tracelytics.joboe.config.ConfigProperty;
import com.tracelytics.joboe.settings.SettingsArg;
import com.tracelytics.joboe.settings.SettingsArgChangeListener;
import com.tracelytics.joboe.settings.SettingsManager;
import com.tracelytics.logging.Logger;
import com.tracelytics.logging.LoggerFactory;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class TransactionNameManager {
    private static final Logger LOGGER = LoggerFactory.getLogger();
    private static final String[] DEFAULT_TRANSACTION_NAME_PATTERN = {"p1", "p2"};
    private static final String CUSTOM_TRANSACTION_NAME_PATTERN_SEPARATOR = ".";
    private static final String DEFAULT_TRANSACTION_NAME_PATTERN_SEPARATOR = "/";
    private static final String DOMAIN_PREFIX_SEPARATOR = "/";
    public static final String OVER_LIMIT_TRANSACTION_NAME = "other";
    public static final String UNKNOWN_TRANSACTION_NAME = "unknown";
    public static final int DEFAULT_MAX_NAME_COUNT = 200;
    public static final int MAX_TRANSACTION_NAME_LENGTH = 255;
    public static final String TRANSACTION_NAME_ELLIPSIS = "...";
    public static final Pattern REPLACE_PATTERN = Pattern.compile("[^-.:_\\\\\\/\\w\\? ]");
    public static final String DEFAULT_SDK_TRANSACTION_NAME_PREFIX = "custom-";

    private static final String[] customTransactionNamePattern;
    static final Cache<String, String> URL_TRANSACTION_NAME_CACHE = CacheBuilder.newBuilder().maximumSize(1000).expireAfterAccess(1200, TimeUnit.SECONDS).<String, String>build(); //20 mins cache

    private static final Set<String> EXISTING_TRANSACTION_NAMES = new HashSet<String>();
    private static boolean limitExceeded;
    private static int maxNameCount = DEFAULT_MAX_NAME_COUNT;

    private static final boolean domainPrefixedTransactionName;

    static {
        customTransactionNamePattern = getTransactionNamePattern();
        addNameCountChangeListener();

        Boolean domainPrefixedTransactionNameObject  = (Boolean) ConfigManager.getConfig(ConfigProperty.AGENT_DOMAIN_PREFIXED_TRANSACTION_NAME);
        domainPrefixedTransactionName = domainPrefixedTransactionNameObject != null && domainPrefixedTransactionNameObject; //only set it to true if the flag present and is set to true
    }

    private TransactionNameManager() { //forbid instantiation
    }

    private static void addNameCountChangeListener() {
        SettingsManager.registerListener(new SettingsArgChangeListener<Integer>(SettingsArg.MAX_TRANSACTIONS) {
            @Override
            public void onChange(Integer newValue) {
                if (newValue != null) {
                    if (newValue > maxNameCount) {
                        limitExceeded = false; //reset the exceed flag
                    }
                    maxNameCount = newValue;
                } else { //reset to default
                    maxNameCount = DEFAULT_MAX_NAME_COUNT;
                }
            }
        });

    }

    private static String[] getTransactionNamePattern() {
        String pattern = (String) ConfigManager.getConfig(ConfigProperty.AGENT_TRANSACTION_NAME_PATTERN);
        return pattern != null ? parseTransactionNamePattern(pattern) : null;
    }

    static String[] parseTransactionNamePattern(String pattern) {
        String[] tokens = pattern.split(",");
        for (int i = 0 ; i < tokens.length; i ++) {
            tokens[i] = tokens[i].trim();
        }
        return tokens;
    }

    /**
     * Gets a transaction name based on information provided in a span and domainPrefixedTransactionName flag,
     * the result will be recorded if not null.
     *
     * If more than <code>MAX_NAME_COUNT</code> transaction name is recorded, "other" will be returned.
     * If the logic fails to extract a transaction from the given span, "unknown" will be returned.
     * @param spanData
     * @return
     */
    public static String getTransactionName(SpanData spanData) {
        String transactionName = buildTransactionName(spanData);


        if (transactionName != null) {
            if (domainPrefixedTransactionName) {
                transactionName = prefixTransactionNameWithDomainName(transactionName, spanData);
            }

            transactionName = transformTransactionName(transactionName);
            return addTransactionName(transactionName) ? transactionName : OVER_LIMIT_TRANSACTION_NAME; //check the transaction name limit;
        } else {
            return UNKNOWN_TRANSACTION_NAME; //unable to build the transaction name
        }
    }

    private static String prefixTransactionNameWithDomainName(String transactionName, SpanData spanData) {
        Object httpHostValue = spanData.getAttributes().get(SemanticAttributes.HTTP_HOST);
        if (httpHostValue instanceof String && !"".equals(httpHostValue)) {
            String domain = (String) httpHostValue;

            if (transactionName.startsWith("/")) {
                return domain + transactionName;
            } else {
                return domain + DOMAIN_PREFIX_SEPARATOR + transactionName;
            }
        }

        return transactionName;
    }

    /**
     * Transform the transaction name according to https://github.com/librato/gotv/blob/376240c5fcce883f37a5358cb30ac39ab9283c7e/collector/agentmetrics/tags.go#L41-L52 and
     * https://github.com/librato/jackdaw/blob/0930023a2d30dc42e58ed45cc05df9b46e2b7da1/src/main/java/com/librato/jackdaw/ingress/IngressMeasurement.java#L28
     *
     * @param inputTransactionName
     * @return
     */
    static String transformTransactionName(String inputTransactionName) {
        String transactionName = inputTransactionName;

        if (transactionName.length() > MAX_TRANSACTION_NAME_LENGTH) {
            transactionName = transactionName.substring(0, MAX_TRANSACTION_NAME_LENGTH - TRANSACTION_NAME_ELLIPSIS.length()) + TRANSACTION_NAME_ELLIPSIS;
        } else if ("".equals(transactionName)) {
            transactionName = " "; //ensure that it at least has 1 character
        }



        transactionName = REPLACE_PATTERN.matcher(transactionName).replaceAll("_");

        transactionName = transactionName.toLowerCase();

        if (!transactionName.equalsIgnoreCase(inputTransactionName)) {
            LOGGER.debug("Transaction name [" + inputTransactionName + "] has been transformed to [" + transactionName + "]");
        }

        return transactionName;
    }

    /**
     * Builds a transaction name based on information provided in a span
     * @param spanData
     * @return  a transaction name built based on the span, null if no transaction name can be built
     */
    static String buildTransactionName(SpanData spanData) {
        return buildTransactionName(spanData.getTraceId(), spanData.getName(), spanData.getAttributes());
    }

    static String buildTransactionName(String traceId, String spanName, Attributes spanAttributes) {

        String customName = CustomTransactionNameDict.get(traceId);
        if (customName != null) {
            LOGGER.log(Logger.Level.DEBUG, String.format("Using custom transaction name(%s)",  customName));
            return customName;
        }

        String url = spanAttributes.get(SemanticAttributes.HTTP_URL);
        String path = url != null ? Util.parsePath(url) : spanAttributes.get(SemanticAttributes.HTTP_TARGET);

        // use HandlerName which may be injected by some MVC instrumentations (currently only Spring MVC)
        String handlerName = spanAttributes.get(AttributeKey.stringKey("HandlerName"));
        if (handlerName != null) {
            LOGGER.log(Logger.Level.DEBUG, String.format("Using HandlerName(%s) as the transaction name",  handlerName));
            return handlerName;
        }

        // use "http.route"
        String httpRoute = spanAttributes.get(SemanticAttributes.HTTP_ROUTE);
        if (httpRoute != null) {
            LOGGER.log(Logger.Level.DEBUG, String.format("Using http.route (%s) as the transaction name",  httpRoute));
            return httpRoute;
        }

        // get transaction name from url
        if (customTransactionNamePattern != null) { //try forming transaction name by the custom configured pattern
            String transactionName = getTransactionNameByUrlAndPattern(path, customTransactionNamePattern, false, CUSTOM_TRANSACTION_NAME_PATTERN_SEPARATOR);

            if (transactionName != null) {
                LOGGER.log(Logger.Level.DEBUG, String.format("Using custom configure pattern to extract transaction name: (%s)",  transactionName));
                return transactionName;
            }
        }

        //try the default token name pattern
        String transactionNameByUrl = getTransactionNameByUrlAndPattern(path, DEFAULT_TRANSACTION_NAME_PATTERN, true, DEFAULT_TRANSACTION_NAME_PATTERN_SEPARATOR);
        if (transactionNameByUrl != null) {
            LOGGER.log(Logger.Level.DEBUG, String.format("Using token name pattern to extract transaction name: (%s)",  transactionNameByUrl));
            return transactionNameByUrl;
        }

        LOGGER.log(Logger.Level.DEBUG, String.format("Using span name as the transaction name: (%s)",  spanName));
        return spanName;
    }
    /**
     * Gets transaction name based on host, URL and provided name pattern. It might look up and update the urlTransactionNameCache
     *
     * @param url   url that must NOT contains query param
     * @param transactionNamePattern
     * @return
     */
    static String getTransactionNameByUrlAndPattern(String url, String[] transactionNamePattern, boolean separatorAsPrefix, String separator) {
        if (url == null) {
            return null;
        }

        String transactionName = URL_TRANSACTION_NAME_CACHE.getIfPresent(url);
        if (transactionName == null) {
            transactionName = buildTransactionNameByUrlAndPattern(url, transactionNamePattern, separatorAsPrefix, separator);
            if (transactionName != null) {
                URL_TRANSACTION_NAME_CACHE.put(url, transactionName);
            }
        }

        return transactionName;
    }

    /**
     * Generates a transaction name by concatenating matching pattern tokens with '.'
     *
     * The valid tokens are host and p1, p2, ... pn. For example a token list of ["host", "p2"] on URL http://localhost:8080/test-api/action/1, will generate transaction name "localhost.action"</li>
     *
     * @param url                       url that must NOT contains query param
     * @param transactionNamePattern    the token pattern as an array
     * @param separator
     * @param separatorAsPrefix
     * @return
     */
    static String buildTransactionNameByUrlAndPattern(String url, String[] transactionNamePattern, boolean separatorAsPrefix, String separator) {
        Map<String, String> urlTokenMap = new HashMap<String, String>();
        int counter = 1;
        for (String token : url.split("/")) {
            if (!"".equals(token)) {
                String tokenName = "p" + counter ++;
                urlTokenMap.put(tokenName, token);
            }
        }

        String transactionName = separatorAsPrefix ? separator : "";
        boolean isFirstToken = true;
        for (String patternToken : transactionNamePattern) {
            if (urlTokenMap.containsKey(patternToken)) {
                if (isFirstToken) {
                    transactionName += urlTokenMap.get(patternToken);
                    isFirstToken = false;
                } else {
                    transactionName += separator + urlTokenMap.get(patternToken);
                }
            }
        }
        return transactionName;
    }

    /**
     * Adds a transaction name to the tracking set
     * @param transactionName   the name to be added, should NOT be null
     * @return  true if transactionName is already existed or added successfully; false otherwise (limit exceeded)
     */
    public static boolean addTransactionName(String transactionName) {
        synchronized(EXISTING_TRANSACTION_NAMES) {
            if (!EXISTING_TRANSACTION_NAMES.contains(transactionName)) {
                if (EXISTING_TRANSACTION_NAMES.size() < maxNameCount) {
                    EXISTING_TRANSACTION_NAMES.add(transactionName);
                    return true;
                }
            } else { //the name already exists, so it's not over the limit
                return true;
            }
        }
        limitExceeded = true; //toggle the flag
        return false;
    }

    public static boolean isLimitExceeded() {
        return limitExceeded;
    }

    public static void clearTransactionNames() {
        synchronized(EXISTING_TRANSACTION_NAMES) {
            EXISTING_TRANSACTION_NAMES.clear();

            limitExceeded = false;
        }
    }

    /**
     * For internal testing usage only
     */
    static void reset() {
        clearTransactionNames();
        URL_TRANSACTION_NAME_CACHE.invalidateAll();
        maxNameCount = DEFAULT_MAX_NAME_COUNT;
    }
}
