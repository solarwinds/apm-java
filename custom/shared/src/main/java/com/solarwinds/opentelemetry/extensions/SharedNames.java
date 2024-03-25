package com.solarwinds.opentelemetry.extensions;

public final class SharedNames {
  private SharedNames() {}

  public static String COMPONENT_NAME = "solarwinds";

  public static String TRANSACTION_NAME_KEY = "sw.transaction";

  // This is visible to customer via span layer and can be used to configure transaction
  // filtering setting.
  public static final String LAYER_NAME_PLACEHOLDER = "%s:%s";
}
