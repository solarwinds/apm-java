package com.solarwinds.opentelemetry.extensions;

import com.solarwinds.joboe.config.ConfigParser;
import com.solarwinds.joboe.config.InvalidConfigException;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class SqlTagDatabasesParser implements ConfigParser<String, Set<String>> {
  @Override
  public Set<String> convert(String databases) throws InvalidConfigException {
    return Arrays.stream(databases.split(",")).map(String::toLowerCase).collect(Collectors.toSet());
  }
}
