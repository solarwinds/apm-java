package com.appoptics.opentelemetry.extensions.initialize.config;

import com.tracelytics.joboe.TracingMode;
import com.tracelytics.joboe.config.ConfigParser;
import com.tracelytics.joboe.config.ConfigProperty;
import com.tracelytics.joboe.config.InvalidConfigException;
import com.tracelytics.logging.Logger;
import com.tracelytics.logging.LoggerFactory;

public class TracingModeParser implements ConfigParser<String, TracingMode> {
  private Logger logger = LoggerFactory.getLogger();

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
