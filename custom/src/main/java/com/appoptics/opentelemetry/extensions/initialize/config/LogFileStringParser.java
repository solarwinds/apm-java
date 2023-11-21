package com.appoptics.opentelemetry.extensions.initialize.config;

import com.tracelytics.joboe.config.ConfigParser;
import com.tracelytics.joboe.config.InvalidConfigException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class LogFileStringParser implements ConfigParser<String, Path> {

  @Override
  public Path convert(String pathString) throws InvalidConfigException {
    try {
      return Paths.get(pathString);
    } catch (InvalidPathException e) {
      throw new InvalidConfigException(
          "Log file path [" + pathString + "] is invalid : " + e.getMessage(), e);
    }
  }
}
