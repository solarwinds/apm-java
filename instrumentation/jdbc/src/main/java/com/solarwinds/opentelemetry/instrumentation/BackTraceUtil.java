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

package com.solarwinds.opentelemetry.instrumentation;

import com.solarwinds.joboe.logging.Logger;
import com.solarwinds.joboe.logging.LoggerFactory;
import java.util.Arrays;
import java.util.List;

public class BackTraceUtil {
  private static final int MAX_BACK_TRACE_TOP_LINE_COUNT = 100;

  private static final int MAX_BACK_TRACE_BOTTOM_LINE_COUNT = 20;

  private static final int MAX_BACK_TRACE_LINE_COUNT =
      MAX_BACK_TRACE_TOP_LINE_COUNT + MAX_BACK_TRACE_BOTTOM_LINE_COUNT;

  private static final Logger logger = LoggerFactory.getLogger();

  public static StackTraceElement[] getBackTrace(int skipElements) {
    StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();

    int startPosition =
        2 + skipElements; // Starts with 2: To exclude the getStackTrace() and addBackTrace calls
    // themselves. Also adds the number of skipElements provided in the
    // argument to skip elements

    if (startPosition >= stackTrace.length) {
      logger.debug(
          "Attempt to skip ["
              + skipElements
              + "] elements in addBackTrace is invalid, no stack trace element is left!");
      return new StackTraceElement[0];
    }

    int targetStackTraceLength = stackTrace.length - startPosition;
    StackTraceElement[] targetStackTrace = new StackTraceElement[targetStackTraceLength];
    System.arraycopy(stackTrace, startPosition, targetStackTrace, 0, targetStackTraceLength);

    return targetStackTrace;
  }

  public static String backTraceToString(StackTraceElement[] stackTrace) {
    List<StackTraceElement> wrappedStackTrace =
        Arrays.asList(stackTrace); // wrap it so hashCode and equals work

    String cachedValue = BackTraceCache.getBackTraceString(wrappedStackTrace);
    if (cachedValue != null) {
      return cachedValue;
    }

    StringBuffer stringBuffer = new StringBuffer();
    if (stackTrace.length > MAX_BACK_TRACE_LINE_COUNT) { // then we will have to skip some lines
      appendStackTrace(
          stackTrace, 0, MAX_BACK_TRACE_TOP_LINE_COUNT, stringBuffer); // add the top lines

      stringBuffer
          .append("...Skipped ")
          .append(stackTrace.length - MAX_BACK_TRACE_LINE_COUNT)
          .append(" line(s)\n");

      appendStackTrace(
          stackTrace,
          stackTrace.length - MAX_BACK_TRACE_BOTTOM_LINE_COUNT,
          MAX_BACK_TRACE_BOTTOM_LINE_COUNT,
          stringBuffer); // add the bottom lines

    } else {
      appendStackTrace(stackTrace, 0, stackTrace.length, stringBuffer); // add everything
    }

    String value = stringBuffer.toString();
    BackTraceCache.putBackTraceString(wrappedStackTrace, value);
    return value;
  }

  /**
   * Build the stackTrace output and append the result to the buffer provided
   *
   * @param stackTrace The source of the stack trace array
   * @param buffer The buffer that stores the result
   */
  private static void appendStackTrace(
      StackTraceElement[] stackTrace, int startPosition, int lineCount, StringBuffer buffer) {
    for (int i = startPosition; i < startPosition + lineCount && i < stackTrace.length; i++) {
      buffer.append(stackTrace[i].toString()).append("\n");
    }
  }
}
