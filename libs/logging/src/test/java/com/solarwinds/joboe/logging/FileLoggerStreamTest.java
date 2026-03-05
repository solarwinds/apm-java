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
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

public class FileLoggerStreamTest {
  @Test
  public void testBasicOperation() throws IOException {
    Path logFilePath = getTestLogFilePath("basic-test.log");
    Files.deleteIfExists(logFilePath);

    // test creating new log file
    try (FileLoggerStream loggerStream = new FileLoggerStream(logFilePath, 1024 * 1024, 1)) {
      loggerStream.println("test1");
    }

    List<String> lines = Files.readAllLines(logFilePath);
    assertEquals(1, lines.size());
    assertEquals("test1", lines.get(0));

    // test appending an existing log file
    try (FileLoggerStream loggerStream = new FileLoggerStream(logFilePath, 1024 * 1024, 1)) {
      loggerStream.println("test2");
    }

    lines = Files.readAllLines(logFilePath);
    assertEquals(2, lines.size());
    assertEquals("test1", lines.get(0));
    assertEquals("test2", lines.get(1));

    try (FileLoggerStream loggerStream = new FileLoggerStream(logFilePath, 1024 * 1024, 1)) {
      loggerStream.printStackTrace(new RuntimeException("Test Exception"));
    }
    lines = Files.readAllLines(logFilePath);
    assert (lines.get(2).contains(RuntimeException.class.getName()));
    assert (lines.get(2).contains("Test Exception"));
    assert (lines.get(3).contains(FileLoggerStreamTest.class.getName()));
  }

  @Test
  public void testFileRolling() throws IOException, InterruptedException {
    Path logFilePath = getTestLogFilePath("file-rolling.log");
    Path backupPath = Paths.get(logFilePath.toString() + ".1");
    Files.deleteIfExists(logFilePath);
    Files.deleteIfExists(backupPath);

    int targetSizePerCall = 10;
    int stringArgumentSize = targetSizePerCall - System.lineSeparator().length();

    // test creating new log file
    try (FileLoggerStream loggerStream = new FileLoggerStream(logFilePath, 10, 1)) {
      loggerStream.println(
          getTestString(1, stringArgumentSize)); // 10 bytes, current log file size 0
      loggerStream.println(
          getTestString(2, stringArgumentSize)); // 10 bytes, current log file size 10, no rolling
      loggerStream.println(
          getTestString(3, stringArgumentSize)); // 10 bytes, current log file size 20, roll
    }

    List<String> lines = Files.readAllLines(backupPath);
    assertEquals(2, lines.size());
    assertEquals(getTestString(1, stringArgumentSize), lines.get(0));
    assertEquals(getTestString(2, stringArgumentSize), lines.get(1));

    lines = Files.readAllLines(logFilePath);
    assertEquals(1, lines.size());
    assertEquals(getTestString(3, stringArgumentSize), lines.get(0));

    try (FileLoggerStream loggerStream = new FileLoggerStream(logFilePath, 10, 1)) {
      // continue with the same log file and backups
      loggerStream.println(
          getTestString(4, stringArgumentSize)); // 10 bytes, current log file size 10, no rolling
      loggerStream.println(
          getTestString(
              5, stringArgumentSize)); // 10 bytes, current log file size 20, roll, the existing .1
      // backup will be replaced with the new one
    }

    // there should not be a .2 backup
    assertFalse(Files.exists(Paths.get(logFilePath + ".2")));

    System.out.println(backupPath.toAbsolutePath());
    lines = Files.readAllLines(backupPath);
    assertEquals(2, lines.size());
    assertEquals(getTestString(3, stringArgumentSize), lines.get(0));
    assertEquals(getTestString(4, stringArgumentSize), lines.get(1));

    lines = Files.readAllLines(logFilePath);
    assertEquals(1, lines.size());
    assertEquals(getTestString(5, stringArgumentSize), lines.get(0));
  }

  /** Test rolling by concurrent java processes */
  @Test
  public void testConcurrentFileRolling() throws Exception {
    String testClassDirectory =
        getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
    String classDirectory =
        Logger.class.getProtectionDomain().getCodeSource().getLocation().getPath();

    String classPathString;
    String osName = System.getProperty("os.name");
    if ("windows".equals(osName)) {
      classPathString = '"' + testClassDirectory + ';' + classDirectory + '"';
    } else {
      classPathString = testClassDirectory + ':' + classDirectory;
    }

    Path logFilePath = getTestLogFilePath("concurrent-test.log");

    int maxSize = 1024 * 1024; // 1MB
    int maxBackup = LogSetting.DEFAULT_FILE_MAX_BACKUP;
    int iterationCount = 1024;
    int processCount = 4;
    int lineSize = 1024;
    int printStringSize = lineSize - System.lineSeparator().length();

    List<Process> processes = new ArrayList<Process>();
    cleanup(logFilePath);
    for (int i = 0; i < processCount; i++) {
      String printString = getTestString(i, printStringSize);
      String[] command =
          new String[] {
            "java",
            "-cp",
            classPathString,
            TestLoggerProcess.class.getName(),
            String.valueOf(logFilePath.toAbsolutePath()),
            String.valueOf(maxSize),
            String.valueOf(maxBackup),
            String.valueOf(iterationCount),
            printString
          };
      Process process = Runtime.getRuntime().exec(command);
      System.out.println("Executing command " + Arrays.toString(command) + " process " + process);
      processes.add(process);
    }

    Map<Process, ReadProcessThread> readProcessThreads = new HashMap<>();
    for (Process process : processes) {
      final BufferedReader inputReader =
          new BufferedReader(new InputStreamReader(process.getInputStream()));
      final BufferedReader errorReader =
          new BufferedReader(new InputStreamReader(process.getErrorStream()));
      ReadProcessThread readProcessThread =
          new ReadProcessThread(process.toString(), inputReader, errorReader);
      readProcessThreads.put(process, readProcessThread);
      readProcessThread.start();
    }

    for (Process process : processes) {
      process.waitFor();
      readProcessThreads.get(process).stopReading();
    }

    int expectedLogFileCount =
        (processCount * iterationCount * lineSize) / maxSize
            + 1; // + 1 for the overflow as "divide by maxSize" truncates
    List<String> allLogLines = new ArrayList<String>();
    allLogLines.addAll(Files.readAllLines(logFilePath));

    for (int i = 1; i <= expectedLogFileCount - 1; i++) { // -1 will be the backups
      try {
        List<String> lines = Files.readAllLines(Paths.get(logFilePath + "." + i));
        allLogLines.addAll(lines);
        System.out.println(i + " => " + lines.size());
      } catch (NoSuchFileException e) {
        // it's okay, sometimes it might fit in just expectedLogFileCount - 1 files
      }
    }

    for (String logLine : allLogLines) {
      assertEquals(printStringSize, logLine.length());
    }

    assertEquals(processCount * iterationCount, allLogLines.size());
    for (int i = 0; i < processCount; i++) {
      int matchingStringCount = 0;
      String expectedLogLine = getTestString(i, printStringSize);
      for (String logLine : allLogLines) {
        if (expectedLogLine.equals(logLine)) {
          matchingStringCount++;
        }
      }
      assertEquals(iterationCount, matchingStringCount, "Failed at index [" + i + "]");
    }
  }

  private void cleanup(Path logFilePath) throws IOException {
    Files.deleteIfExists(logFilePath);

    int i = 1;
    while (Files.deleteIfExists(Paths.get(logFilePath + "." + (i++)))) {}
  }

  private String getTestString(int index, int targetByteSize) {
    StringBuilder result = new StringBuilder(String.valueOf(index));
    while (result.length() < targetByteSize) {
      result.append('x');
    }
    return result.toString();
  }

  private Path getTestLogFilePath(String fileName) {
    try {
      return Paths.get(
          URI.create(
              getClass().getProtectionDomain().getCodeSource().getLocation().toURI() + fileName));
    } catch (URISyntaxException e) {
      e.printStackTrace();
      return null;
    }
  }

  static class ReadProcessThread extends Thread {
    private final BufferedReader inputReader, errorReader;
    private final String prefix;
    private boolean shouldRun = true;

    ReadProcessThread(String prefix, BufferedReader inputReader, BufferedReader errorReader) {
      this.inputReader = inputReader;
      this.errorReader = errorReader;
      this.prefix = prefix;
    }

    public void stopReading() {
      shouldRun = false;
    }

    @Override
    public void run() {
      while (shouldRun) {
        try {
          String line;
          while ((line = errorReader.readLine()) != null) {
            System.out.println(prefix + " : " + line);
          }
          while ((line = inputReader.readLine()) != null) {
            System.out.println(prefix + " : " + line);
          }
          TimeUnit.SECONDS.sleep(1);
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }
  }
}
