/*
 * © SolarWinds Worldwide, LLC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.solarwinds.joboe.logging;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.Getter;

/**
 * A wrapper class around the java.util.logger. Take note that this is very similar to other logging
 * frameworks (commons.logging, log4j etc)'s Logger and should be replaced by those implementations
 * if we later on switch to those frameworks
 *
 * @author Patson Luk
 */
@Getter
public class Logger {
  public static final String SOLARWINDSS_TAG = "[SolarWinds APM]";

  private static final Level DEFAULT_LOGGING = Level.INFO;
  private static final ThreadLocal<DateFormat> DATE_FORMAT =
      new ThreadLocal<DateFormat>() {
        @Override
        protected SimpleDateFormat initialValue() {
          return new SimpleDateFormat("MMM dd, yyyy hh:mm:ss.SSS aa");
        }
      };
  private static final LogSetting DEFAULT_LOG_SETTING =
      new LogSetting(DEFAULT_LOGGING, true, true, null, null, null);

  static final Logger INSTANCE = new Logger();
  static final Logger STD_STREAM_LOGGER =
      new Logger(); // an internal logger that uses stderr and stdout streams only

  private Level loggerLevel = DEFAULT_LOGGING;
  private LoggerStream errorStream = new SystemErrStream();
  private LoggerStream infoStream = new SystemOutStream();

  private static final String INSTANCE_ID_ENV_VARIABLE = "WEBSITE_INSTANCE_ID";

  Logger() {}

  LoggerStream getLogFileStream(Path logFilePath, int logFileMaxSize, int logFileMaxBackup) {
    // special case for azure, prefix the instance id to the log file name
    String azureInstanceId = System.getenv(INSTANCE_ID_ENV_VARIABLE);
    ;
    if (azureInstanceId != null) {
      Path fileName = logFilePath.getFileName();
      String prefixedFileName = azureInstanceId + "-" + fileName.toString();
      logFilePath =
          logFilePath.getParent() != null
              ? Paths.get(logFilePath.getParent().toString(), prefixedFileName)
              : Paths.get(prefixedFileName);
    }

    try {
      info("Java agent log location: " + logFilePath.toAbsolutePath());
      return new FileLoggerStream(logFilePath, logFileMaxSize * 1024 * 1024, logFileMaxBackup);
    } catch (IOException e) {
      warn(
          "Failed to redirect logs to [" + logFilePath.toAbsolutePath() + "] : " + e.getMessage(),
          e);
      return null;
    }
  }

  LoggerStream getErrorStream() {
    return errorStream;
  }

  LoggerStream getInfoStream() {
    return infoStream;
  }

  public void fatal(String message) {
    this.log(Level.FATAL, message);
  }

  public void fatal(String message, Throwable throwable) {
    this.log(Level.FATAL, message, throwable);
  }

  public void error(String message) {
    this.log(Level.ERROR, message);
  }

  public void error(String message, Throwable throwable) {
    this.log(Level.ERROR, message, throwable);
  }

  public void warn(String message) {
    this.log(Level.WARNING, message);
  }

  public void warn(String message, Throwable throwable) {
    this.log(Level.WARNING, message, throwable);
  }

  public void info(String message) {
    this.log(Level.INFO, message);
  }

  public void info(String message, Throwable throwable) {
    this.log(Level.INFO, message, throwable);
  }

  public void debug(String message) {
    this.log(Level.DEBUG, message);
  }

  public void debug(String message, Throwable throwable) {
    this.log(Level.DEBUG, message, throwable);
  }

  public void trace(String message) {
    this.log(Level.TRACE, message);
  }

  public void trace(String message, Throwable throwable) {
    this.log(Level.TRACE, message, throwable);
  }

  public void log(Level level, String message) {
    log(level, message, null);
  }

  public void log(Level level, String message, Throwable t) {
    if (level == null) {
      if (shouldLog(Level.ERROR)) { // missing level in the input has severity level of Level.ERROR
        print(Level.ERROR, "Missing log Level for this log message!");
      }
      if (shouldLog(
          DEFAULT_LOGGING)) { // the logging message itself takes the default logging level
        print(DEFAULT_LOGGING, message, t);
      }
    } else if (shouldLog(level)) {
      print(level, message, t);
    }
  }

  private void print(Level level, String message) {
    print(level, message, null);
  }

  private void print(Level level, String message, Throwable t) {
    LoggerStream output =
        level.compareTo(Level.INFO) < 0
            ? errorStream
            : infoStream; // most severe level declared first
    output.println(getFormattedMessage(level, message));

    if (t != null) {
      output.printStackTrace(t);
    }
  }

  private static String getFormattedMessage(Level level, String message) {
    String timestamp = DATE_FORMAT.get().format(Calendar.getInstance().getTime());
    String label = timestamp + " " + level.toString() + " " + SOLARWINDSS_TAG + " ";

    return message != null ? label + message : label;
  }

  public boolean shouldLog(Level messageLevel) {
    return messageLevel.compareTo(loggerLevel) <= 0; // most severe level is declared first
  }

  void configure(LoggerConfiguration config) {
    LogSetting logSetting = config.getLogSetting();
    if (logSetting == null) {
      logSetting = DEFAULT_LOG_SETTING;
    }

    // level
    if (config
        .isDebug()) { // backward compatibility, use loggingLevel=DEBUG if the argument is set to
      // false
      loggerLevel = Level.DEBUG;
    } else {
      loggerLevel = logSetting.getLevel() != null ? logSetting.getLevel() : DEFAULT_LOGGING;
    }

    // log file location
    Path path = config.getLogFile();
    if (path == null) {
      path = logSetting.getLogFilePath();
    }

    // other log file parameters
    LoggerStream logFileStream = null;
    if (path != null) {
      String javaVersion = System.getProperty("java.version");

      if (javaVersion.startsWith("1.6")) {

        info("Cannot set up log file at [" + path + "] as it is only supported for JDK 7 or later");
      } else {
        logFileStream =
            getLogFileStream(
                path, logSetting.getLogFileMaxSize(), logSetting.getLogFileMaxBackup());
      }
    }

    List<LoggerStream> errorStreams = new ArrayList<LoggerStream>();
    List<LoggerStream> infoStreams = new ArrayList<LoggerStream>();

    if (logSetting.isStderrEnabled()) {
      errorStreams.add(SystemErrStream.INSTANCE);
    }
    if (logSetting.isStdoutEnabled()) {
      infoStreams.add(SystemOutStream.INSTANCE);
    }
    if (logFileStream != null) {
      errorStreams.add(logFileStream);
      infoStreams.add(logFileStream);
    }

    if (errorStreams.size() == 1) {
      this.errorStream = errorStreams.get(0);
    } else {
      this.errorStream =
          new CompositeStream(errorStreams.toArray(new LoggerStream[errorStreams.size()]));
    }

    if (infoStreams.size() == 1) {
      this.infoStream = infoStreams.get(0);
    } else {
      this.infoStream =
          new CompositeStream(infoStreams.toArray(new LoggerStream[infoStreams.size()]));
    }
  }

  @Getter
  public enum Level {
    OFF("off"),
    FATAL("fatal"),
    ERROR("error"),
    WARNING("warn"),
    INFO("info"),
    DEBUG("debug"),
    TRACE("trace");

    private static final Map<String, Level> map = new HashMap<String, Level>();

    static {
      for (Level level : Level.values()) {
        map.put(level.label, level);
      }
    }

    private final String label;

    Level(String label) {
      this.label = label;
    }

    public static Level fromLabel(String label) {
      return map.get(label);
    }

    public static Set<String> getAllLabels() {
      return Collections.unmodifiableSet(map.keySet());
    }
  }
}
