/*
 * Copyright SolarWinds Worldwide, LLC.
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

package com.solarwinds.opentelemetry.extensions.initialize.config.livereload;

import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

import com.solarwinds.joboe.logging.LoggerFactory;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public final class ConfigurationFileWatcher {
  private final Path directory;

  private final String filename;

  private final Consumer<Path> fileChangeListener;

  private final WatchService watchService;

  private final ScheduledExecutorService executorService;

  private final long watchPeriod;

  private static ConfigurationFileWatcher INSTANCE;

  private ScheduledFuture<?> scheduledWatch;

  private ConfigurationFileWatcher(
      Path directory,
      String filename,
      long watchPeriod,
      WatchService watchService,
      ScheduledExecutorService executorService,
      Consumer<Path> fileChangeListener) {
    this.directory = directory;
    this.filename = filename;
    this.watchPeriod = watchPeriod;
    this.executorService = executorService;
    this.fileChangeListener = fileChangeListener;
    this.watchService = watchService;
  }

  public static void restartWatch(
      Path directory,
      String filename,
      long watchPeriod,
      WatchService watchService,
      ScheduledExecutorService scheduledExecutorService,
      Consumer<Path> fileChangeListener) {
    if (INSTANCE != null) {
      INSTANCE.cancelWatch();
    }
    INSTANCE =
        new ConfigurationFileWatcher(
            directory,
            filename,
            watchPeriod,
            watchService,
            scheduledExecutorService,
            fileChangeListener);
    INSTANCE.startWatch();
  }

  private void watch() {
    WatchKey key = watchService.poll();
    if (key != null) {
      for (WatchEvent<?> event : key.pollEvents()) {
        WatchEvent.Kind<?> kind = event.kind();
        if (kind == OVERFLOW) {
          continue;
        }

        WatchEvent<Path> ev = (WatchEvent<Path>) event;
        Path filename = ev.context();
        if (filename.endsWith(this.filename)) {
          fileChangeListener.accept(filename);
        }
      }
      boolean valid = key.reset();
      if (!valid) {
        LoggerFactory.getLogger()
            .info(
                String.format(
                    "Watch ended for directory(%s) and file(%s) because the directory became inaccessible",
                    directory, filename));
      }
    }
  }

  private void startWatch() {
    try {
      directory.register(watchService, ENTRY_MODIFY);
      scheduledWatch =
          executorService.scheduleAtFixedRate(this::watch, 0, watchPeriod, TimeUnit.SECONDS);
      LoggerFactory.getLogger()
          .info(
              String.format(
                  "Started watch for directory(%s), watch period(%d seconds)",
                  directory, watchPeriod));
    } catch (Exception exception) {
      LoggerFactory.getLogger()
          .info(
              String.format(
                  "Failed to start watch for directory(%s) and file(%s) due to error - %s",
                  directory, filename, exception));
    }
  }

  private void cancelWatch() {
    if (scheduledWatch != null) {
      scheduledWatch.cancel(true);
    }
  }
}
