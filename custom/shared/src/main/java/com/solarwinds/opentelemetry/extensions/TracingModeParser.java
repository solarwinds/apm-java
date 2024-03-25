package com.solarwinds.opentelemetry.extensions;

import com.solarwinds.joboe.config.ConfigParser;
import com.solarwinds.joboe.config.ConfigProperty;
import com.solarwinds.joboe.config.InvalidConfigException;
import com.solarwinds.joboe.sampling.TracingMode;

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
