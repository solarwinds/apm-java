package com.appoptics.opentelemetry.extensions.initialize.config;

public final class ConfigConstants {
    public static final int MAX_SQL_QUERY_LENGTH_LOWER_LIMIT = 2 * 1024;
    public static final int MAX_SQL_QUERY_LENGTH_UPPER_LIMIT = 128 * 1024;
    public static final int SAMPLE_RESOLUTION = 1000000;
    public static final String PRODUCT_NAME = "solarwinds";
    public static final String PRODUCT_NAME_UPPERCASE = "SOLARWINDS";
    public static final String CONFIG_PREFIX = "otel." + ConfigConstants.PRODUCT_NAME;
    public static final String APPOPTICS_SERVICE_KEY = CONFIG_PREFIX + ".service.key";
    public static final String APPOPTICS_CONFIG_FILE = CONFIG_PREFIX + ".configfile";
}
