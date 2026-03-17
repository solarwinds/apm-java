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

package com.solarwinds.joboe.core.rpc;

import com.solarwinds.joboe.core.rpc.Client.Callback;
import com.solarwinds.joboe.logging.Logger;
import com.solarwinds.joboe.logging.Logger.Level;
import com.solarwinds.joboe.logging.LoggerFactory;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Provides a Thrift Callback that performs general logging on various Thrift {@link Result}.
 *
 * <p>Takes into consideration of the current Logger's logging verbosity level
 *
 * @author pluk
 * @param <T>
 */
public class ClientLoggingCallback<T extends Result> implements Callback<T> {
  private final Logger logger = LoggerFactory.getLogger();
  private final String operation;

  private final Callback<T> delegation;

  /**
   * @param operation The operation string to be included in the logging message for identification
   *     purpose
   */
  public ClientLoggingCallback(String operation) {
    this.operation = operation;

    if (logger.shouldLog(Level.DEBUG)) {
      delegation = new FinerLoggingCallback();
    } else {
      delegation = new LoggingCallback();
    }
  }

  @Override
  public void complete(T result) {
    delegation.complete(result);
  }

  @Override
  public void fail(Exception e) {
    delegation.fail(e);
  }

  /**
   * Callback that logs every result and exception, with full stacktrace for exceptions
   *
   * @author pluk
   */
  private class FinerLoggingCallback implements Callback<T> {

    @Override
    public void complete(T result) {
      ResultCode resultCode = result.getResultCode();
      String arg = result.getArg();
      if (resultCode.isError()) {
        logger.warn(
            "Failed operation ["
                + operation
                + "] due to Result code ["
                + resultCode
                + "] arg ["
                + arg
                + "]");
      } else {
        logger.debug(
            "Completed operation ["
                + operation
                + "] with Result code ["
                + resultCode
                + "] arg ["
                + arg
                + "]");
      }
    }

    @Override
    public void fail(Exception e) {
      logger.warn(
          "Client operation [" + operation + "] failed with exception message : " + e.getMessage(),
          e);
    }
  }

  /**
   * Callback that logs only the first occurrence of repeating error result code/repeating
   * exception. No stack trace reported
   *
   * @author pluk
   */
  private class LoggingCallback implements Callback<T> {
    private final ConcurrentMap<ResultCode, LogInfo> logInfoByResultCode =
        new ConcurrentHashMap<ResultCode, LogInfo>();
    private static final int REPORT_INTERVAL = 60 * 1000; // 1 min

    {
      for (ResultCode resultCode : ResultCode.values()) {
        logInfoByResultCode.put(resultCode, new LogInfo());
      }
    }

    @Override
    public void complete(Result result) {
      ResultCode resultCode = result.getResultCode();

      if (resultCode.isError()) {
        long time = System.currentTimeMillis();

        synchronized (resultCode) {
          LogInfo logInfo = logInfoByResultCode.get(resultCode);
          logInfo.occurrence++;

          if (time - logInfo.lastLogTime >= REPORT_INTERVAL) {
            if (logInfo.occurrence > 1) {
              logger.warn(
                  "Failed operation ["
                      + operation
                      + "] due to Result code ["
                      + resultCode
                      + "] arg ["
                      + result.getArg()
                      + "] "
                      + logInfo.occurrence
                      + " occurrences since last error");
            } else {
              logger.warn(
                  "Failed operation ["
                      + operation
                      + "] due to Result code ["
                      + resultCode
                      + "] arg ["
                      + result.getArg()
                      + "]");
            }

            logInfo.reset(time);
          }
        }
      }
    }

    @Override
    public void fail(Exception e) {
      // do not log exception here as we only want to warn something for persisting exception, which
      // is something this logger cannot determine
    }

    private class LogInfo {
      private int occurrence;
      private long lastLogTime = 0;

      private void reset(long time) {
        occurrence = 0;
        lastLogTime = time;
      }
    }
  }
}
