package com.solarwinds.opentelemetry.extensions.initialize.config.livereload;

import static org.mockito.Mockito.*;

import java.io.IOException;
import java.nio.file.*;
import java.util.Collections;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.function.Consumer;
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

  @Mock private Consumer<Path> fileChangeListerMock;

  @Mock private ScheduledFuture<?> scheduledFutureMock;

  @Mock private Path dirMock;

  @Captor private ArgumentCaptor<Runnable> runnableArgumentCaptor;

  private final String filename = "file.json";

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
        dirMock,
        filename,
        watchPeriod,
        watchServiceMock,
        scheduledExecutorServiceMock,
        fileChangeListerMock);

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

    when(watchEventMock.context()).thenReturn(Paths.get(filename));
    when(watchKeyMock.reset()).thenReturn(false);
    when(dirMock.register(any(), any())).thenReturn(watchKeyMock);

    when(scheduledExecutorServiceMock.scheduleAtFixedRate(
            runnableArgumentCaptor.capture(), anyLong(), anyLong(), any()))
        .thenAnswer(invocation -> scheduledFutureMock);

    ConfigurationFileWatcher.restartWatch(
        dirMock,
        filename,
        watchPeriod,
        watchServiceMock,
        scheduledExecutorServiceMock,
        fileChangeListerMock);

    runnableArgumentCaptor.getValue().run();

    verify(watchServiceMock).poll();
    verify(watchKeyMock).pollEvents();
    verify(watchEventMock).context();

    verify(fileChangeListerMock).accept(any());
  }
}
