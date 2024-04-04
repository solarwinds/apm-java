package com.solarwinds.opentelemetry.extensions;

public final class Constants {
  private Constants() {}

  public static final int MAX_SQL_QUERY_LENGTH_LOWER_LIMIT = 2048;
  public static final int MAX_SQL_QUERY_LENGTH_UPPER_LIMIT = 128 * 1024;
  public static final int SAMPLE_RESOLUTION = 1000000;
}
