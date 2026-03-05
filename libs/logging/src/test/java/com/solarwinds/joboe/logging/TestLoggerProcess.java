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
import java.nio.file.Paths;

public class TestLoggerProcess {
  // args: logFileName, maxSize, maxBackup, iterationCount, printString
  public static void main(String[] args) throws IOException, InterruptedException {

    String logFileName = args[0];
    int maxSize = Integer.parseInt(args[1]); // in bytes
    int maxBackup = Integer.parseInt(args[2]);
    int iterationCount = Integer.parseInt(args[3]);
    String printString = args[4];

    long start = System.currentTimeMillis();
    FileLoggerStream loggerStream =
        new FileLoggerStream(Paths.get(logFileName), maxSize, maxBackup);
    try {
      for (int i = 0; i < iterationCount; i++) {
        loggerStream.println(printString);
      }
    } catch (Throwable e) {
      e.printStackTrace();
    } finally {
      loggerStream.close(10);
    }
    long end = System.currentTimeMillis();
    System.out.println("Per operation : " + (end - start) * 1.0 / iterationCount);
  }
}
