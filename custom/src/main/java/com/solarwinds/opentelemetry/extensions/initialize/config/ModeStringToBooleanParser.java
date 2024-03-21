package com.solarwinds.opentelemetry.extensions.initialize.config;

import com.solarwinds.joboe.config.ConfigParser;
import com.solarwinds.joboe.config.InvalidConfigException;

public class ModeStringToBooleanParser implements ConfigParser<String, Boolean> {
  public static final ModeStringToBooleanParser INSTANCE = new ModeStringToBooleanParser();

  private ModeStringToBooleanParser() {}

  @Override
  public Boolean convert(String input) throws InvalidConfigException {
    if ("enabled".equals(input)) {
      return true;
    } else if ("disabled".equals(input)) {
      return false;
    } else {
      throw new InvalidConfigException(
          "Expected value [enabled] or [disabled] but found [" + input + "]");
    }
  }
}
