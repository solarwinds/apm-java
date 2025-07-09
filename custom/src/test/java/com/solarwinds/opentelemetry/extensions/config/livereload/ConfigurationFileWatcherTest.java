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

package com.solarwinds.opentelemetry.extensions.config.livereload;

import static org.mockito.Mockito.*;

import java.io.IOException;
import java.nio.file.*;
import java.util.Collections;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConfigurationFileWatcherTest {

  @Mock private WatchService watchServiceMock;

  @Mock private WatchKey watchKeyMock;

  @Mock private WatchEvent<Path> watchEventMock;

  @Mock private ScheduledExecutorService scheduledExecutorServiceMock;

  @Mock private Runnable fileChangeListerMock;

  @Mock private ScheduledFuture<?> scheduledFutureMock;

  @Mock private Path dirMock;

  @Captor private ArgumentCaptor<Runnable> runnableArgumentCaptor;

  private final long watchPeriod = 1;

  @Test
  void verifyThatOverflowEventsAreIgnored() throws IOException {
    when(watchServiceMock.poll()).thenReturn(watchKeyMock);
    when(watchKeyMock.pollEvents()).thenReturn(Collections.singletonList(watchEventMock));
    when(watchEventMock.kind()).thenAnswer(invocation -> StandardWatchEventKinds.OVERFLOW);

    when(dirMock.register(any(), any())).thenReturn(watchKeyMock);
    when(scheduledExecutorServiceMock.scheduleAtFixedRate(
            runnableArgumentCaptor.capture(), anyLong(), anyLong(), any()))
        .thenAnswer(invocation -> scheduledFutureMock);

    ConfigurationFileWatcher.restartWatch(
        dirMock, watchPeriod, watchServiceMock, scheduledExecutorServiceMock, fileChangeListerMock);

    runnableArgumentCaptor.getValue().run();
    verify(watchServiceMock).poll();
    verify(watchKeyMock).pollEvents();
    verify(watchEventMock, never()).context();
  }

  @Test
  void verifyThatFileChangeListenerIsInvokedWhenFileIsModified() throws IOException {
    when(watchServiceMock.poll()).thenReturn(watchKeyMock);
    when(watchKeyMock.pollEvents()).thenReturn(Collections.singletonList(watchEventMock));
    when(watchEventMock.kind()).thenAnswer(invocation -> StandardWatchEventKinds.ENTRY_MODIFY);

    when(watchKeyMock.reset()).thenReturn(false);
    when(dirMock.register(any(), any())).thenReturn(watchKeyMock);

    when(scheduledExecutorServiceMock.scheduleAtFixedRate(
            runnableArgumentCaptor.capture(), anyLong(), anyLong(), any()))
        .thenAnswer(invocation -> scheduledFutureMock);

    ConfigurationFileWatcher.restartWatch(
        dirMock, watchPeriod, watchServiceMock, scheduledExecutorServiceMock, fileChangeListerMock);

    runnableArgumentCaptor.getValue().run();

    verify(watchServiceMock).poll();
    verify(watchKeyMock).pollEvents();

    verify(fileChangeListerMock).run();
  }
}
