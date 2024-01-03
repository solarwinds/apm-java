package com.appoptics.opentelemetry.extensions.initialize.config;

import com.solarwinds.joboe.core.TracingMode;
import com.solarwinds.joboe.core.config.ConfigParser;
import com.solarwinds.joboe.core.config.ConfigProperty;
import com.solarwinds.joboe.core.config.InvalidConfigException;

public class TracingModeParser implements ConfigParser<String, TracingMode> {

  @Override
  public TracingMode convert(String argVal) throws InvalidConfigException {
    if (argVal != null) {
      TracingMode tracingMode = TracingMode.fromString(argVal);
      if (tracingMode != null) {
        return tracingMode;
      } else {
        throw new InvalidConfigException(
            "Invalid "
                + ConfigProperty.AGENT_TRACING_MODE.getConfigFileKey()
                + " : "
                + argVal
                + ", must be \"disabled\" or \"enabled\"");
      }
    } else {
      return null;
    }
  }
}
