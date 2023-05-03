package com.appoptics.opentelemetry.extensions.initialize.config;

public final class ConfigConstants {
    public static final int MAX_SQL_QUERY_LENGTH_LOWER_LIMIT = 2 * 1024;
    public static final int MAX_SQL_QUERY_LENGTH_UPPER_LIMIT = 128 * 1024;
    public static final int SAMPLE_RESOLUTION = 1000000;
    public static final String CONFIG_PREFIX = "sw.apm";
    public static final String SYS_PROPERTY_SERVICE_KEY = CONFIG_PREFIX + ".service.key";
    public static final String SYS_PROPERTY_CONFIG_FILE = CONFIG_PREFIX + ".config.file";

    public static final String COMPONENT_NAME = "solarwinds";
}
