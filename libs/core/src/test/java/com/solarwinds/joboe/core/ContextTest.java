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
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
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
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

public class ContextTest {
  private static final TestSettingsReader testSettingsReader = TestUtils.initSettingsReader();
  private static final TestReporter tracingReporter = TestUtils.initTraceReporter();

  @AfterEach
  protected void tearDown() throws Exception {

    testSettingsReader.reset();
    tracingReporter.reset();
  }

  @Test
  public void testContext() throws Exception {

    // Clear metadata: it may have been set by a previous test
    Context.clearMetadata();

    // Make sure we can get metadata from our context:
    final Metadata md = Context.getMetadata();
    assertFalse(md.isValid());

    md.randomize();
    assertTrue(md.isValid());

    Metadata md2 = Context.getMetadata();
    assertSame(md, md2);

    // Make sure we can set IDs:
    Metadata rndMd = new Metadata();
    rndMd.randomize();

    Context.setMetadata(rndMd.toHexString());
    assertEquals(Context.getMetadata().toHexString(), rndMd.toHexString());

    // Verify that the context is inherited in child thread
    final Metadata parentMD = Context.getMetadata();
    final AtomicReference<Error> assertionError = new AtomicReference<Error>();

    Thread thr =
        new Thread(
            () -> {
              try {
                Metadata childMD = Context.getMetadata();
                assertNotSame(parentMD, childMD); // different object
                assertEquals(parentMD.toHexString(), childMD.toHexString()); // but same metadata
                assertTrue(childMD.isValid());
              } catch (Error e) {
                assertionError.set(e);
              }
            });

    thr.start();
    thr.join();

    if (assertionError.get() != null) {
      throw assertionError.get();
    }
  }

  @Test
  public void testInheritContext() throws InterruptedException {
    Metadata context = Context.getMetadata();
    context.randomize(); // make a valid context

    TestThread thread;

    thread = new TestThread();
    thread.start();
    thread.join();

    assertEquals(
        context.toHexString(), thread.threadContext.toHexString()); // should have same xtrace id
    assertNotSame(
        context,
        thread
            .threadContext); // but they should not be the same object, the inherited context should
    // be a clone

    // test config flag
    Context.setSkipInheritingContext(true);
    thread = new TestThread();
    thread.start();
    thread.join();
    Context.setSkipInheritingContext(false);

    assertFalse(thread.threadContext.isValid()); // no context inherited
    // disable inherit context
    testSettingsReader.put(
        new SettingsMockupBuilder()
            .withFlags(TracingMode.ALWAYS)
            .withSampleRate(TraceDecisionUtil.SAMPLE_RESOLUTION)
            .withSettingsArg(SettingsArg.DISABLE_INHERIT_CONTEXT, true)
            .build());
    thread = new TestThread();
    thread.start();
    thread.join();

    assertFalse(thread.threadContext.isValid()); // no context inherited

    // re-enable inherit context
    testSettingsReader.put(
        new SettingsMockupBuilder()
            .withFlags(TracingMode.ALWAYS)
            .withSampleRate(TraceDecisionUtil.SAMPLE_RESOLUTION)
            .build());
    thread = new TestThread();
    thread.start();
    thread.join();

    assertEquals(
        context.toHexString(), thread.threadContext.toHexString()); // should have same xtrace id
    assertNotSame(
        context,
        thread
            .threadContext); // but they should not be the same object, the inherited context should
    // be a clone
  }

  @Test
  public void testCreateEventWithContextTtl() throws InterruptedException {
    Context.clearMetadata(); // this triggers creation of new metadata
    Context.getMetadata().randomize(true); // make it a valid context

    // warm-up run...as first call can take a few secs (for getting host info)
    Event event = Context.createEvent();
    event.report(tracingReporter);
    tracingReporter.reset();

    Context.clearMetadata(); // this triggers creation of new metadata
    Context.getMetadata().randomize(); // make it a valid context
    int newTtl = 2;
    // set to 2 secs
    testSettingsReader.put(
        new SettingsMockupBuilder()
            .withFlags(TracingMode.ALWAYS)
            .withSampleRate(TraceDecisionUtil.SAMPLE_RESOLUTION)
            .withSettingsArg(SettingsArg.MAX_CONTEXT_AGE, newTtl)
            .build());

    event = Context.createEvent();
    event.report(tracingReporter); // should be okay

    List<DeserializedEvent> deserializedEvents = tracingReporter.getSentEvents();
    assertEquals(1, deserializedEvents.size()); // ok, since it's within 2 secs
    tracingReporter.reset();

    TimeUnit.SECONDS.sleep(newTtl + 1);

    event = Context.createEvent();
    event.report(tracingReporter); // not reported as this is more than 2 secs since creation
    deserializedEvents = tracingReporter.getSentEvents();
    assertTrue(deserializedEvents.isEmpty()); // no events

    Context.clearMetadata(); // ensure reset metadata also reset ttl
    Context.getMetadata().randomize(); // make it a valid context

    event = Context.createEvent();
    event.report(tracingReporter); // this event should now be reported as this is a new metadata

    deserializedEvents = tracingReporter.getSentEvents();
    assertEquals(1, deserializedEvents.size());

    tracingReporter.reset();
  }

  @Test
  public void testCreateEventWithMaxEvents() throws InterruptedException {
    Context.clearMetadata(); // this triggers creation of new metadata
    Context.getMetadata().randomize(true); // make it a valid context

    List<DeserializedEvent> deserializedEvents;

    // now simulate a max event change
    Context.clearMetadata(); // this triggers creation of new metadata
    Context.getMetadata().randomize(true); // make it a valid context
    int newMaxEvent = 10;
    testSettingsReader.put(
        new SettingsMockupBuilder()
            .withFlags(TracingMode.ALWAYS)
            .withSampleRate(TraceDecisionUtil.SAMPLE_RESOLUTION)
            .withSettingsArg(SettingsArg.MAX_CONTEXT_EVENTS, newMaxEvent)
            .build());

    for (int i = 0; i < newMaxEvent; i++) {
      Event event = Context.createEvent();
      event.report(tracingReporter);
    }

    deserializedEvents = tracingReporter.getSentEvents();
    assertEquals(newMaxEvent, deserializedEvents.size()); // ok within limit
    tracingReporter.reset();

    Event noopEvent = Context.createEvent(); // exceeding limit
    noopEvent.report(tracingReporter);
    deserializedEvents = tracingReporter.getSentEvents();
    tracingReporter.reset();
    assertTrue(deserializedEvents.isEmpty()); // no events

    assertTrue(Context.isValid()); // context should still be valid
    assertTrue(Context.getMetadata().isSampled()); // and it should still be flagged as sampled

    Context.clearMetadata(); // this triggers creation of new metadata
    Context.getMetadata().randomize(true); // make it a valid context

    ExecutorService threadPool = Executors.newCachedThreadPool();

    for (int i = 0; i < newMaxEvent + 50; i++) {
      threadPool.submit(
          () -> {
            Event event = Context.createEvent();
            event.report(tracingReporter);
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

    noopEvent = Context.createEvent(); // exceeding limit, this should return a noop
    noopEvent.report(tracingReporter);
    deserializedEvents = tracingReporter.getSentEvents();
    assertTrue(deserializedEvents.isEmpty()); // no events
  }

  @Test
  public void testMaxBacktraces() throws InterruptedException, ExecutionException {
    Context.clearMetadata(); // this triggers creation of new metadata
    Context.getMetadata().randomize(true); // make it a valid context

    List<DeserializedEvent> deserializedEvents;

    // now simulate a max event change
    Context.clearMetadata(); // this triggers creation of new metadata
    Context.getMetadata().randomize(true); // make it a valid context
    int newBacktraces = 10;
    testSettingsReader.put(
        new SettingsMockupBuilder()
            .withFlags(TracingMode.ALWAYS)
            .withSampleRate(TraceDecisionUtil.SAMPLE_RESOLUTION)
            .withSettingsArg(SettingsArg.MAX_CONTEXT_BACKTRACES, newBacktraces)
            .build());

    for (int i = 0; i < newBacktraces; i++) {
      Event event = Context.createEvent();
      event.addInfo("Backtrace", "some backtrace");
      event.addInfo("OtherKv", "some value");
      event.report(tracingReporter);
    }

    deserializedEvents = tracingReporter.getSentEvents();
    assertEquals(10, deserializedEvents.size());
    for (DeserializedEvent deseralizedEvent : deserializedEvents) {
      assertEquals("some backtrace", deseralizedEvent.getSentEntries().get("Backtrace"));
      assertEquals("some value", deseralizedEvent.getSentEntries().get("OtherKv"));
    }
    tracingReporter.reset();

    Event event = Context.createEvent();
    event.addInfo("Backtrace", "some backtrace"); // exceeding limit
    event.addInfo("OtherKv", "some value"); // this should still get through
    event.report(tracingReporter);

    deserializedEvents = tracingReporter.getSentEvents();
    assertEquals(1, deserializedEvents.size());
    DeserializedEvent deseralizedEvent = deserializedEvents.get(0);
    assertNull(deseralizedEvent.getSentEntries().get("Backtrace"));
    assertEquals("some value", deseralizedEvent.getSentEntries().get("OtherKv"));
    tracingReporter.reset();

    Context.clearMetadata(); // this triggers creation of new metadata
    Context.getMetadata().randomize(true); // make it a valid context

    ExecutorService threadPool = Executors.newCachedThreadPool();

    List<Future<?>> futures = new ArrayList<Future<?>>();
    for (int i = 0; i < newBacktraces + 10; i++) {
      futures.add(
          threadPool.submit(
              () -> {
                Event event1 = Context.createEvent();
                event1.addInfo("Backtrace", "some backtrace"); // 10 backtraces will not be reported
                event1.addInfo("OtherKv", "some value");
                event1.report(tracingReporter);
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

  private static class TestThread extends Thread {
    private Metadata threadContext;

    @Override
    public void run() {
      threadContext = Context.getMetadata();
    }
  }
}
