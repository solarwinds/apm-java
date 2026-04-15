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

package com.solarwinds.joboe.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.solarwinds.joboe.core.TestReporter.DeserializedEvent;
import com.solarwinds.joboe.core.settings.TestSettingsReader;
import com.solarwinds.joboe.core.settings.TestSettingsReader.SettingsMockupBuilder;
import com.solarwinds.joboe.core.util.TestUtils;
import com.solarwinds.joboe.sampling.Metadata;
import com.solarwinds.joboe.sampling.SettingsArg;
import com.solarwinds.joboe.sampling.TraceDecisionUtil;
import com.solarwinds.joboe.sampling.TracingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

public class ContextTest {
  private static final TestSettingsReader testSettingsReader = TestUtils.initSettingsReader();
  private static final TestReporter tracingReporter = TestUtils.initTraceReporter();

  @AfterEach
  protected void tearDown() {

    testSettingsReader.reset();
    tracingReporter.reset();
  }

  @Test
  public void testContext() {
    // Make sure we can get metadata from our context:
    final Metadata md = Context.getMetadata();
    assertFalse(md.isValid());

    md.randomize();
    assertTrue(md.isValid());
  }

  @Test
  public void testCreateEventWithContextTtl() throws InterruptedException {
    Metadata metadata = Context.getMetadata();
    metadata.randomize(true); // make it a valid context

    // warm-up run...as first call can take a few secs (for getting host info)
    Event event = Context.createEventWithContext(metadata);
    event.report(metadata, tracingReporter);
    tracingReporter.reset();

    metadata.randomize(); // make it a valid context
    int newTtl = 2;
    // set to 2 secs
    testSettingsReader.put(
        new SettingsMockupBuilder()
            .withFlags(TracingMode.ALWAYS)
            .withSampleRate(TraceDecisionUtil.SAMPLE_RESOLUTION)
            .withSettingsArg(SettingsArg.MAX_CONTEXT_AGE, newTtl)
            .build());

    event = Context.createEventWithContext(metadata);
    event.report(metadata, tracingReporter); // should be okay

    List<DeserializedEvent> deserializedEvents = tracingReporter.getSentEvents();
    assertEquals(1, deserializedEvents.size()); // ok, since it's within 2 secs
    tracingReporter.reset();

    TimeUnit.SECONDS.sleep(newTtl + 1);

    event = Context.createEventWithContext(metadata);
    event.report(
        metadata, tracingReporter); // not reported as this is more than 2 secs since creation
    deserializedEvents = tracingReporter.getSentEvents();
    assertTrue(deserializedEvents.isEmpty()); // no events

    metadata.randomize(); // make it a valid context

    event = Context.createEventWithContext(metadata);
    event.report(
        metadata, tracingReporter); // this event should now be reported as this is a new metadata

    deserializedEvents = tracingReporter.getSentEvents();
    assertEquals(1, deserializedEvents.size());

    tracingReporter.reset();
  }

  @Test
  public void testCreateEventWithMaxEvents() throws InterruptedException {
    List<DeserializedEvent> deserializedEvents;

    // now simulate a max event change
    Metadata metadata = Context.getMetadata();
    metadata.randomize(true); // make it a valid context
    int newMaxEvent = 10;
    testSettingsReader.put(
        new SettingsMockupBuilder()
            .withFlags(TracingMode.ALWAYS)
            .withSampleRate(TraceDecisionUtil.SAMPLE_RESOLUTION)
            .withSettingsArg(SettingsArg.MAX_CONTEXT_EVENTS, newMaxEvent)
            .build());

    for (int i = 0; i < newMaxEvent; i++) {
      Event event = Context.createEventWithContext(metadata);
      event.report(metadata, tracingReporter);
    }

    deserializedEvents = tracingReporter.getSentEvents();
    assertEquals(newMaxEvent, deserializedEvents.size()); // ok within limit
    tracingReporter.reset();

    Event noopEvent = Context.createEventWithContext(metadata); // exceeding limit
    noopEvent.report(metadata, tracingReporter);
    deserializedEvents = tracingReporter.getSentEvents();
    tracingReporter.reset();
    assertTrue(deserializedEvents.isEmpty()); // no events

    assertTrue(metadata.isSampled()); // and it should still be flagged as sampled
    metadata.randomize(true); // make it a valid context

    ExecutorService threadPool = Executors.newCachedThreadPool();

    for (int i = 0; i < newMaxEvent + 50; i++) {
      threadPool.submit(
          () -> {
            Event event = Context.createEventWithContext(metadata);
            event.report(metadata, tracingReporter);
          });
    }

    threadPool.shutdown();
    threadPool.awaitTermination(10, TimeUnit.SECONDS);

    assertEquals(
        newMaxEvent,
        tracingReporter
            .getSentEvents()
            .size()); // collected event count should be newMaxEvent (not +1)
    tracingReporter.reset();

    noopEvent =
        Context.createEventWithContext(metadata); // exceeding limit, this should return a noop
    noopEvent.report(metadata, tracingReporter);
    deserializedEvents = tracingReporter.getSentEvents();
    assertTrue(deserializedEvents.isEmpty()); // no events
  }

  @Test
  public void testMaxBacktraces() throws InterruptedException, ExecutionException {
    List<DeserializedEvent> deserializedEvents;

    // now simulate a max event change
    Metadata metadata = Context.getMetadata();
    metadata.randomize(true); // make it a valid context
    int newBacktraces = 10;
    testSettingsReader.put(
        new SettingsMockupBuilder()
            .withFlags(TracingMode.ALWAYS)
            .withSampleRate(TraceDecisionUtil.SAMPLE_RESOLUTION)
            .withSettingsArg(SettingsArg.MAX_CONTEXT_BACKTRACES, newBacktraces)
            .build());

    for (int i = 0; i < newBacktraces; i++) {
      Event event = Context.createEventWithContext(metadata);
      event.addInfo("Backtrace", "some backtrace");
      event.addInfo("OtherKv", "some value");
      event.report(metadata, tracingReporter);
    }

    deserializedEvents = tracingReporter.getSentEvents();
    assertEquals(10, deserializedEvents.size());
    for (DeserializedEvent deseralizedEvent : deserializedEvents) {
      assertEquals("some backtrace", deseralizedEvent.getSentEntries().get("Backtrace"));
      assertEquals("some value", deseralizedEvent.getSentEntries().get("OtherKv"));
    }
    tracingReporter.reset();

    Event event = Context.createEventWithContext(metadata);
    event.addInfo("Backtrace", "some backtrace"); // exceeding limit
    event.addInfo("OtherKv", "some value"); // this should still get through
    event.report(metadata, tracingReporter);

    deserializedEvents = tracingReporter.getSentEvents();
    assertEquals(1, deserializedEvents.size());
    DeserializedEvent deseralizedEvent = deserializedEvents.get(0);
    assertNull(deseralizedEvent.getSentEntries().get("Backtrace"));
    assertEquals("some value", deseralizedEvent.getSentEntries().get("OtherKv"));
    tracingReporter.reset();

    metadata.randomize(true); // make it a valid context
    ExecutorService threadPool = Executors.newCachedThreadPool();

    List<Future<?>> futures = new ArrayList<>();
    for (int i = 0; i < newBacktraces + 10; i++) {
      futures.add(
          threadPool.submit(
              () -> {
                Event event1 = Context.createEventWithContext(metadata);
                event1.addInfo("Backtrace", "some backtrace"); // 10 backtraces will not be reported
                event1.addInfo("OtherKv", "some value");
                event1.report(metadata, tracingReporter);
              }));
    }

    threadPool.shutdown();
    threadPool.awaitTermination(10, TimeUnit.SECONDS);

    for (Future<?> future : futures) {
      future.get(); // check no assertion exception thrown in the runnable
    }

    int backTraceCollected = 0;
    for (DeserializedEvent deserializedEvent : tracingReporter.getSentEvents()) {
      Map<String, Object> sentEntries = deserializedEvent.getSentEntries();
      if (sentEntries.containsKey("Backtrace")) {
        backTraceCollected++;
      }
      assertEquals("some value", sentEntries.get("OtherKv"));
    }

    tracingReporter.reset();
    assertEquals(newBacktraces, backTraceCollected);
  }
}
