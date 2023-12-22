package com.appoptics.opentelemetry.extensions.initialize.config;

import com.solarwinds.joboe.config.ResourceMatcher;
import java.util.regex.Pattern;

class StringPatternMatcher implements ResourceMatcher {
  private final Pattern pattern;

  StringPatternMatcher(Pattern pattern) {
    this.pattern = pattern;
  }

  @Override
  public boolean matches(String resource) {
    return pattern.matcher(resource).matches();
  }
}
