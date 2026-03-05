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

import static com.solarwinds.joboe.logging.Logger.STD_STREAM_LOGGER;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Logger stream that writes to a File via a FileChannel
 *
 * <p>This implements a rolling behavior which rolls the log file and create backup files up to
 * `maxBackup` if the current log file size exceeds `maxSize`.
 *
 * <p>Take note that multiple java processes might run this logger and the rolling operation should
 * be blocking across processes so that the log file should only be rolled once.
 *
 * <p>If another process has rolled the file, the current logger might still write to the renamed
 * (rolled) file for a short while but should be switching to the new log file eventually. However,
 * it is guaranteed that no log should be lost if the rolling is done by another process.
 */
class FileLoggerStream implements LoggerStream, Closeable {
  private static final int MAX_LOCK_TRY = 5;
  private static final int LOCK_RETRY_SLEEP = 100; // in millseconds
  private static final int CLOSE_TIMEOUT = 5;
  private static final int MESSAGE_QUEUE_SIZE = 100000;
  private static final int RETRY_INTERVAL = 60000; // only retry every 60 seconds
  private final Path logFilePath;
  private final Path
      logFileLockPath; // need a separate lock file, cannot use the existing log file as it could be
  // moved by other processes
  FileChannel fileChannel;
  private PrintWriter filePrintWriter;
  private Long lastRollFailure; // last time of file rolling failure
  private Long lastChannelReset; // last time the file channel was reset
  private final long maxSize; // in bytes
  private final int maxBackup;
  private final ExecutorService service =
      new ThreadPoolExecutor(
          1,
          1,
          0L,
          TimeUnit.MILLISECONDS,
          new LinkedBlockingQueue<Runnable>(MESSAGE_QUEUE_SIZE),
          LoggerThreadFactory.newInstance("file-logger"));

  FileLoggerStream(Path logFilePath, int maxSizeInBytes, int maxBackup) throws IOException {
    resetChannel(logFilePath);
    this.logFilePath = logFilePath;
    String tempDir = System.getProperty("java.io.tmpdir");
    if (tempDir != null && Files.isWritable(Paths.get(tempDir))) {
      this.logFileLockPath = Paths.get(tempDir, logFilePath.getFileName() + ".lock");
    } else {
      this.logFileLockPath = Paths.get(logFilePath.toString() + ".lock");
    }
    Logger.INSTANCE.debug("Using " + logFileLockPath + " for log file lock for file rolling");

    this.maxSize = maxSizeInBytes;
    this.maxBackup = maxBackup;
  }

  Path getLogFilePath() {
    return logFilePath;
  }

  private void resetChannel(Path logFilePath) throws IOException {
    resetChannel(logFilePath, true);
  }

  /**
   * Resets the file channel and print writer
   *
   * <p>If forced, then it always resets it, otherwise reset would only happen if it has been less
   * than RETRY_INTERVAL since last attempt
   *
   * @param logFilePath
   * @param forced
   * @throws IOException
   */
  private void resetChannel(Path logFilePath, boolean forced) throws IOException {
    boolean resetChannel;
    long time = System.currentTimeMillis();
    if (forced) {
      resetChannel = true;
    } else {
      resetChannel = lastChannelReset == null || time - lastChannelReset >= RETRY_INTERVAL;
    }

    if (resetChannel) {
      closeIo();

      this.fileChannel =
          FileChannel.open(
              logFilePath,
              StandardOpenOption.APPEND,
              StandardOpenOption.CREATE,
              StandardOpenOption.WRITE);
      this.filePrintWriter =
          new PrintWriter(
              new BufferedWriter(
                  Channels.newWriter(fileChannel, Charset.defaultCharset().newEncoder(), -1)));

      lastChannelReset = time;
    }
  }

  @Override
  public void println(final String value) {
    try {
      service.submit(
          new LogTask() {
            @Override
            protected void log() {
              filePrintWriter.println(value);
              filePrintWriter.flush();
            }
          });
    } catch (RejectedExecutionException e) {
      // use STD_STREAM_LOGGER as we do not want to submit more messages to this file logger
      STD_STREAM_LOGGER.debug(
          "Failed to log message to file as queue is full " + e.getMessage(), e);
    }
  }

  @Override
  public void printStackTrace(final Throwable throwable) {
    try {
      service.submit(
          new LogTask() {
            @Override
            protected void log() {
              throwable.printStackTrace(filePrintWriter);
              filePrintWriter.flush();
            }
          });
    } catch (RejectedExecutionException e) {
      // use STD_STREAM_LOGGER as we do not want to submit more messages to this file logger
      STD_STREAM_LOGGER.debug(
          "Failed to log message to file as queue is full " + e.getMessage(), e);
    }
  }

  @Override
  public void close() {
    close(CLOSE_TIMEOUT);
  }

  public void close(long closeTimeoutInSec) {
    service.shutdown();
    try {
      service.awaitTermination(closeTimeoutInSec, TimeUnit.SECONDS);
      closeIo();
    } catch (Exception e) {
      // ok, it's closing anyway
    }
  }

  private void closeIo() throws IOException {
    if (filePrintWriter != null) {
      filePrintWriter.flush();
      filePrintWriter.close();
    }
    if (fileChannel != null) {
      fileChannel.close();
    }
  }

  private abstract class LogTask implements Callable<Boolean> {
    @Override
    public Boolean call() {
      try {
        checkFile();
      } catch (Throwable e) {
        // use STD_STREAM_LOGGER as we do not want to submit more messages to this file logger which
        // might trigger more error
        STD_STREAM_LOGGER.warn(
            "Failed to check the log file for size limit : " + e.getMessage(), e);
      }
      log();
      return true;
    }

    protected abstract void log();
  }

  /**
   * Checks and resets if the current log file channel exceeds the max size and perform file rolling
   * if necessary
   *
   * <p>Take note that this needs to take into consideration of: 1. 2 threads within the same JVM
   * can possibly call this concurrently, even though current implementation runs on a single
   * threaded pool, it could change in the future. 2. 2 JVM processes can possibly call this
   * concurrently (for example the shutdown process of tomcat is a separate process from the running
   * tomcat), and it should only roll once.
   */
  private boolean checkFile() throws IOException {
    if (isChannelExceedingSizeLimit()) {
      boolean forcedReset = false;
      try {
        if (shouldRollFile()) { // then see if files should be rolled
          Logger.INSTANCE.debug(
              "Log file ["
                  + logFilePath
                  + "] exceeds "
                  + (maxSize / 1024 / 1024)
                  + " MB, attempt to roll the log files.");
          boolean rolled = lockAndRollFile();
          forcedReset = rolled; // always reset the channel if the rolling was successful
        }
      } finally {
        // if the channel exceeds size limit and the file rolling was successful, then it's required
        // to
        // reset the channel. Otherwise, we will try to reset the channel but w/o a forced reset,
        // this is
        // to avoid resetting the channel too rapidly if file rolling fails repeatedly due to
        // persistent
        // problem such as external file locking. Take note that it still makes sense to try
        // resetting even
        // if file rolling failed, for example another process might have rolled the file so this
        // process
        // should attempt to reset file channel to point to the new log file
        resetChannel(logFilePath, forcedReset);
      }
    }
    return true;
  }

  private boolean isChannelExceedingSizeLimit() throws IOException {
    return fileChannel.size() > maxSize;
  }

  private boolean shouldRollFile() throws IOException {
    return Files.size(logFilePath) > maxSize
        && (lastRollFailure == null
            || System.currentTimeMillis() - lastRollFailure >= RETRY_INTERVAL);
  }

  /**
   * Attempts to acquire the lock for rolling the file.
   *
   * <p>If the lock is acquired, then perform file rolling
   *
   * @return whether the file was rolled successfully
   * @throws IOException
   */
  boolean lockAndRollFile() throws IOException {
    FileLockAndChannel fileLockAndChannel = null;

    try {
      fileLockAndChannel = tryLock();
      boolean rolled = false;
      if (fileLockAndChannel == null) {
        // ok to log to the file logger as we know this would not trigger rapid file locking error
        // as it's guarded by RETRY_INTERVAL
        Logger.INSTANCE.warn(
            "Failed to acquire lock on log file after "
                + MAX_LOCK_TRY
                + " tries during the attempt to roll.");
      } else { // locked acquired, check again to ensure the files are not rolled while waiting for
        // the lock
        if (shouldRollFile()) {
          try {
            doRollFile();
            rolled = true;
            Logger.INSTANCE.debug("Successfully rolled the log files");
            lastRollFailure = null; // reset failure timestamp
          } catch (IOException e) {
            // ok to log to the file logger as we know this would not trigger rapid file locking
            // error as it's guarded by RETRY_INTERVAL
            Logger.INSTANCE.warn(
                "Failed to roll the log files, exception message: " + e.getMessage(), e);
            lastRollFailure = System.currentTimeMillis();
            throw e;
          }
        }
      }
      return rolled;
    } finally {
      if (fileLockAndChannel != null) {
        fileLockAndChannel.fileLock
            .release(); // release the lock first before closing the file channel
        fileLockAndChannel.lockChannel.close();
      }
    }
  }

  private void doRollFile() throws IOException {
    // delete the one with highest index if exist
    Path lastBackupFilePath = Paths.get(logFilePath.toString() + "." + maxBackup);
    Files.deleteIfExists(lastBackupFilePath);

    // traverse the backup file and bump the index up by one, from the highest index - 1 first
    for (int i = maxBackup - 1; i > 0; i--) {
      Path backupFilePath = Paths.get(logFilePath + "." + i);
      Path newBackupFilePath = Paths.get(logFilePath + "." + (i + 1));
      if (Files.exists(backupFilePath)) {
        Files.move(backupFilePath, newBackupFilePath, StandardCopyOption.ATOMIC_MOVE);
      }
    }
    // move the current log file
    Files.move(logFilePath, Paths.get(logFilePath + ".1"), StandardCopyOption.ATOMIC_MOVE);
  }

  FileLockAndChannel tryLock() {
    int tryCount = 0;
    FileLock lock;
    while (++tryCount <= MAX_LOCK_TRY) {
      try {
        FileChannel lockChannel =
            FileChannel.open(
                logFileLockPath,
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE); // do not use DELETE_ON_EXIT here. Not working sometimes
        lock = lockChannel.tryLock();
        if (lock != null) {
          return new FileLockAndChannel(lock, lockChannel);
        }
      } catch (IOException e) {
        Logger.INSTANCE.debug("Failed to obtain lockChannel : " + e.getMessage(), e);
      }
      try {
        TimeUnit.MILLISECONDS.sleep(LOCK_RETRY_SLEEP);
      } catch (InterruptedException e) {
        Logger.INSTANCE.debug("Retry lock sleep interrupted", e);
      }
    }
    return null;
  }

  private static class FileLockAndChannel {
    private final FileLock fileLock;
    private final FileChannel lockChannel;

    private FileLockAndChannel(FileLock fileLock, FileChannel lockChannel) {
      this.fileLock = fileLock;
      this.lockChannel = lockChannel;
    }
  }
}
