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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import lombok.Getter;
import org.junit.jupiter.api.Test;

public class LoggerTest {

  @Getter
  private static class ProxyLoggerStream implements LoggerStream {
    private final ByteArrayOutputStream proxyOutput = new ByteArrayOutputStream();
    private final PrintStream proxyStream = new PrintStream(proxyOutput);

    @Override
    public void println(String value) {
      proxyStream.println(value);
    }

    @Override
    public void printStackTrace(Throwable throwable) {
      throwable.printStackTrace(proxyStream);
    }
  }

  private void setProxyStreams(Logger logger, ProxyLoggerStream out, ProxyLoggerStream err)
      throws Exception {
    Field outStreamField = Logger.class.getDeclaredField("infoStream");
    outStreamField.setAccessible(true);
    outStreamField.set(logger, out);

    Field errorStreamField = Logger.class.getDeclaredField("errorStream");
    errorStreamField.setAccessible(true);
    errorStreamField.set(logger, err);
  }

  @Test
  public void testTrace() throws Exception {
    Logger logger = new Logger();

    logger.configure(
        LoggerConfiguration.builder().logSetting(getLogSetting(Logger.Level.TRACE)).build());

    ProxyLoggerStream proxyOutStream = new ProxyLoggerStream();
    ProxyLoggerStream proxyErrStream = new ProxyLoggerStream();
    ByteArrayOutputStream proxyOut = proxyOutStream.getProxyOutput();
    ByteArrayOutputStream proxyErr = proxyErrStream.getProxyOutput();
    setProxyStreams(logger, proxyOutStream, proxyErrStream);

    sendTestMessages(logger);

    String outMessage = proxyOut.toString();
    String errMessage = proxyErr.toString();

    assertEquals(2, countOccurrence(outMessage, "trace-message"));
    assertEquals(1, countOccurrence(outMessage, "trace-exception"));
    assertEquals(2, countOccurrence(outMessage, "debug-message"));
    assertEquals(1, countOccurrence(outMessage, "debug-exception"));
    assertEquals(2, countOccurrence(outMessage, "info-message"));
    assertEquals(1, countOccurrence(outMessage, "info-exception"));
    assertEquals(2, countOccurrence(errMessage, "warn-message"));
    assertEquals(1, countOccurrence(errMessage, "warn-exception"));
    assertEquals(2, countOccurrence(errMessage, "error-message"));
    assertEquals(1, countOccurrence(errMessage, "error-exception"));
    assertEquals(2, countOccurrence(errMessage, "fatal-message"));
    assertEquals(1, countOccurrence(errMessage, "fatal-exception"));
  }

  @Test
  public void testDebug() throws Exception {
    Logger logger = new Logger();

    logger.configure(
        LoggerConfiguration.builder().logSetting(getLogSetting(Logger.Level.DEBUG)).build());

    ProxyLoggerStream proxyOutStream = new ProxyLoggerStream();
    ProxyLoggerStream proxyErrStream = new ProxyLoggerStream();
    ByteArrayOutputStream proxyOut = proxyOutStream.getProxyOutput();
    ByteArrayOutputStream proxyErr = proxyErrStream.getProxyOutput();
    setProxyStreams(logger, proxyOutStream, proxyErrStream);

    sendTestMessages(logger);

    String outMessage = proxyOut.toString();
    String errMessage = proxyErr.toString();

    assertEquals(0, countOccurrence(outMessage, "trace-message"));
    assertEquals(0, countOccurrence(outMessage, "trace-exception"));
    assertEquals(2, countOccurrence(outMessage, "debug-message"));
    assertEquals(1, countOccurrence(outMessage, "debug-exception"));
    assertEquals(2, countOccurrence(outMessage, "info-message"));
    assertEquals(1, countOccurrence(outMessage, "info-exception"));
    assertEquals(2, countOccurrence(errMessage, "warn-message"));
    assertEquals(1, countOccurrence(errMessage, "warn-exception"));
    assertEquals(2, countOccurrence(errMessage, "error-message"));
    assertEquals(1, countOccurrence(errMessage, "error-exception"));
    assertEquals(2, countOccurrence(errMessage, "fatal-message"));
    assertEquals(1, countOccurrence(errMessage, "fatal-exception"));
  }

  @Test
  public void testInfo() throws Exception {
    Logger logger = new Logger();

    logger.configure(
        LoggerConfiguration.builder().logSetting(getLogSetting(Logger.Level.INFO)).build());

    ProxyLoggerStream proxyOutStream = new ProxyLoggerStream();
    ProxyLoggerStream proxyErrStream = new ProxyLoggerStream();
    ByteArrayOutputStream proxyOut = proxyOutStream.getProxyOutput();
    ByteArrayOutputStream proxyErr = proxyErrStream.getProxyOutput();
    setProxyStreams(logger, proxyOutStream, proxyErrStream);

    sendTestMessages(logger);

    String outMessage = proxyOut.toString();
    String errMessage = proxyErr.toString();

    assertEquals(0, countOccurrence(outMessage, "trace-message"));
    assertEquals(0, countOccurrence(outMessage, "trace-exception"));
    assertEquals(0, countOccurrence(outMessage, "debug-message"));
    assertEquals(0, countOccurrence(outMessage, "debug-exception"));
    assertEquals(2, countOccurrence(outMessage, "info-message"));
    assertEquals(1, countOccurrence(outMessage, "info-exception"));
    assertEquals(2, countOccurrence(errMessage, "warn-message"));
    assertEquals(1, countOccurrence(errMessage, "warn-exception"));
    assertEquals(2, countOccurrence(errMessage, "error-message"));
    assertEquals(1, countOccurrence(errMessage, "error-exception"));
    assertEquals(2, countOccurrence(errMessage, "fatal-message"));
    assertEquals(1, countOccurrence(errMessage, "fatal-exception"));
  }

  @Test
  public void testWarn() throws Exception {
    Logger logger = new Logger();

    logger.configure(
        LoggerConfiguration.builder().logSetting(getLogSetting(Logger.Level.WARNING)).build());

    ProxyLoggerStream proxyOutStream = new ProxyLoggerStream();
    ProxyLoggerStream proxyErrStream = new ProxyLoggerStream();
    ByteArrayOutputStream proxyOut = proxyOutStream.getProxyOutput();
    ByteArrayOutputStream proxyErr = proxyErrStream.getProxyOutput();
    setProxyStreams(logger, proxyOutStream, proxyErrStream);

    sendTestMessages(logger);

    String outMessage = proxyOut.toString();
    String errMessage = proxyErr.toString();

    assertEquals(0, countOccurrence(outMessage, "trace-message"));
    assertEquals(0, countOccurrence(outMessage, "trace-exception"));
    assertEquals(0, countOccurrence(outMessage, "debug-message"));
    assertEquals(0, countOccurrence(outMessage, "debug-exception"));
    assertEquals(0, countOccurrence(outMessage, "info-message"));
    assertEquals(0, countOccurrence(outMessage, "info-exception"));
    assertEquals(2, countOccurrence(errMessage, "warn-message"));
    assertEquals(1, countOccurrence(errMessage, "warn-exception"));
    assertEquals(2, countOccurrence(errMessage, "error-message"));
    assertEquals(1, countOccurrence(errMessage, "error-exception"));
    assertEquals(2, countOccurrence(errMessage, "fatal-message"));
    assertEquals(1, countOccurrence(errMessage, "fatal-exception"));
  }

  @Test
  public void testError() throws Exception {
    Logger logger = new Logger();
    logger.configure(
        LoggerConfiguration.builder().logSetting(getLogSetting(Logger.Level.ERROR)).build());

    ProxyLoggerStream proxyOutStream = new ProxyLoggerStream();
    ProxyLoggerStream proxyErrStream = new ProxyLoggerStream();
    ByteArrayOutputStream proxyOut = proxyOutStream.getProxyOutput();
    ByteArrayOutputStream proxyErr = proxyErrStream.getProxyOutput();
    setProxyStreams(logger, proxyOutStream, proxyErrStream);

    sendTestMessages(logger);

    String outMessage = proxyOut.toString();
    String errMessage = proxyErr.toString();

    assertEquals(0, countOccurrence(outMessage, "trace-message"));
    assertEquals(0, countOccurrence(outMessage, "trace-exception"));
    assertEquals(0, countOccurrence(outMessage, "debug-message"));
    assertEquals(0, countOccurrence(outMessage, "debug-exception"));
    assertEquals(0, countOccurrence(outMessage, "info-message"));
    assertEquals(0, countOccurrence(outMessage, "info-exception"));
    assertEquals(0, countOccurrence(errMessage, "warn-message"));
    assertEquals(0, countOccurrence(errMessage, "warn-exception"));
    assertEquals(2, countOccurrence(errMessage, "error-message"));
    assertEquals(1, countOccurrence(errMessage, "error-exception"));
    assertEquals(2, countOccurrence(errMessage, "fatal-message"));
    assertEquals(1, countOccurrence(errMessage, "fatal-exception"));
  }

  @Test
  public void testFatal() throws Exception {
    Logger logger = new Logger();
    logger.configure(
        LoggerConfiguration.builder().logSetting(getLogSetting(Logger.Level.FATAL)).build());

    ProxyLoggerStream proxyOutStream = new ProxyLoggerStream();
    ProxyLoggerStream proxyErrStream = new ProxyLoggerStream();
    ByteArrayOutputStream proxyOut = proxyOutStream.getProxyOutput();
    ByteArrayOutputStream proxyErr = proxyErrStream.getProxyOutput();
    setProxyStreams(logger, proxyOutStream, proxyErrStream);

    sendTestMessages(logger);

    String outMessage = proxyOut.toString();
    String errMessage = proxyErr.toString();

    assertEquals(0, countOccurrence(outMessage, "trace-message"));
    assertEquals(0, countOccurrence(outMessage, "trace-exception"));
    assertEquals(0, countOccurrence(outMessage, "debug-message"));
    assertEquals(0, countOccurrence(outMessage, "debug-exception"));
    assertEquals(0, countOccurrence(outMessage, "info-message"));
    assertEquals(0, countOccurrence(outMessage, "info-exception"));
    assertEquals(0, countOccurrence(errMessage, "warn-message"));
    assertEquals(0, countOccurrence(errMessage, "warn-exception"));
    assertEquals(0, countOccurrence(errMessage, "error-message"));
    assertEquals(0, countOccurrence(errMessage, "error-exception"));
    assertEquals(2, countOccurrence(errMessage, "fatal-message"));
    assertEquals(1, countOccurrence(errMessage, "fatal-exception"));
  }

  @Test
  public void testOff() throws Exception {
    Logger logger = new Logger();
    logger.configure(
        LoggerConfiguration.builder().logSetting(getLogSetting(Logger.Level.OFF)).build());

    ProxyLoggerStream proxyOutStream = new ProxyLoggerStream();
    ProxyLoggerStream proxyErrStream = new ProxyLoggerStream();
    ByteArrayOutputStream proxyOut = proxyOutStream.getProxyOutput();
    ByteArrayOutputStream proxyErr = proxyErrStream.getProxyOutput();
    setProxyStreams(logger, proxyOutStream, proxyErrStream);

    sendTestMessages(logger);

    String outMessage = proxyOut.toString();
    String errMessage = proxyErr.toString();

    assertEquals(0, countOccurrence(outMessage, "trace-message"));
    assertEquals(0, countOccurrence(outMessage, "trace-exception"));
    assertEquals(0, countOccurrence(outMessage, "debug-message"));
    assertEquals(0, countOccurrence(outMessage, "debug-exception"));
    assertEquals(0, countOccurrence(outMessage, "info-message"));
    assertEquals(0, countOccurrence(outMessage, "info-exception"));
    assertEquals(0, countOccurrence(errMessage, "warn-message"));
    assertEquals(0, countOccurrence(errMessage, "warn-exception"));
    assertEquals(0, countOccurrence(errMessage, "error-message"));
    assertEquals(0, countOccurrence(errMessage, "error-exception"));
    assertEquals(0, countOccurrence(errMessage, "fatal-message"));
    assertEquals(0, countOccurrence(errMessage, "fatal-exception"));
  }

  private void sendTestMessages(Logger logger) {
    logger.trace("trace-message");
    logger.trace("trace-message", new Exception("trace-exception"));

    logger.debug("debug-message");
    logger.debug("debug-message", new Exception("debug-exception"));

    logger.info("info-message");
    logger.info("info-message", new Exception("info-exception"));

    logger.warn("warn-message");
    logger.warn("warn-message", new Exception("warn-exception"));

    logger.error("error-message");
    logger.error("error-message", new Exception("error-exception"));

    logger.fatal("fatal-message");
    logger.fatal("fatal-message", new Exception("fatal-exception"));
  }

  private static int countOccurrence(String input, String phase) {
    int occurence = 0;

    int index;

    while ((index = input.indexOf(phase)) != -1) {
      occurence++;
      input = input.substring(index + phase.length());
    }

    return occurence;
  }

  private static LogSetting getLogSetting(Logger.Level logLevel) {
    return new LogSetting(logLevel, true, true, null, null, null);
  }
}
