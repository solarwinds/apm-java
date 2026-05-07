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

package com.solarwinds.joboe.core.profiler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.solarwinds.joboe.core.TestReporter;
import com.solarwinds.joboe.core.TestReporter.DeserializedEvent;
import com.solarwinds.joboe.core.profiler.Profiler.Profile;
import com.solarwinds.joboe.core.util.TestUtils;
import com.solarwinds.joboe.sampling.Metadata;
import com.solarwinds.joboe.sampling.SamplingConfiguration;
import com.solarwinds.joboe.sampling.SamplingException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class ProfileTest {

  private static final ProfilerSetting profilerSetting =
      new ProfilerSetting(
          true,
          Collections.emptySet(),
          ProfilerSetting.DEFAULT_INTERVAL,
          ProfilerSetting.DEFAULT_CIRCUIT_BREAKER_DURATION_THRESHOLD,
          ProfilerSetting.DEFAULT_CIRCUIT_BREAKER_COUNT_THRESHOLD);
  private static TestReporter profilingReporter;

  @BeforeAll
  static /*static because reporter is static in Profiler*/ void setup() {
    profilingReporter = TestUtils.initProfilingReporter(profilerSetting);
  }

  @AfterEach
  void tearDown() {
    profilingReporter.reset();
  }

  @Test
  void partialValidateEntryEventShapeOnStartProfilingOnThread() throws SamplingException {
    Thread thread = Thread.currentThread();
    Profile profile = new Profile(profilerSetting);
    Metadata.setup(SamplingConfiguration.builder().build());

    profile.startProfilingOnThread(
        thread, new Metadata("00-970026c88092a447d3b2bba3be3be2fc-0c8fc43138df813a-01"));
    simulateStackChange(profile);
    List<DeserializedEvent> events = profilingReporter.getSentEvents();
    Map<String, Object> entryEvent = events.get(0).getSentEntries();

    assertEquals("entry", entryEvent.get("Label"));
    assertEquals("profiling", entryEvent.get("Spec"));

    assertEquals("java", entryEvent.get("Language"));
    assertEquals("0c8fc43138df813a", entryEvent.get("SpanRef"));
    assertEquals(thread.getId(), entryEvent.get("TID"));
  }

  @Test
  void doNotCreateTerminalEventsWhenSampleIsNotCollected() throws SamplingException {
    Thread thread = Thread.currentThread();
    Profile profile = new Profile(profilerSetting);
    Metadata.setup(SamplingConfiguration.builder().build());

    profile.startProfilingOnThread(
        thread, new Metadata("00-970026c88092a447d3b2bba3be3be2fc-0c8fc43138df813a-01"));
    profile.stopProfilingOnThread(thread);
    List<DeserializedEvent> events = profilingReporter.getSentEvents();

    assertTrue(events.isEmpty());
  }

  @Test
  void createTerminalEventsWhenSampleIsCollected() throws SamplingException {
    Thread thread = Thread.currentThread();
    Profile profile = new Profile(profilerSetting);
    Metadata.setup(SamplingConfiguration.builder().build());

    profile.startProfilingOnThread(
        thread, new Metadata("00-970026c88092a447d3b2bba3be3be2fc-0c8fc43138df813a-01"));
    simulateStackChange(profile);
    profile.stopProfilingOnThread(thread);

    List<DeserializedEvent> events = profilingReporter.getSentEvents();
    assertEquals(3, events.size());
  }

  @Test
  @SuppressWarnings("unchecked")
  void partialValidateInfoEventOnRecord() throws SamplingException {
    Thread thread = Thread.currentThread();
    StackTraceElement[] stackTrace = thread.getStackTrace();
    Profile profile = new Profile(profilerSetting);
    Metadata.setup(SamplingConfiguration.builder().build());

    profile.startProfilingOnThread(
        thread, new Metadata("00-970026c88092a447d3b2bba3be3be2fc-0c8fc43138df813a-01"));
    profile.record(thread, stackTrace, 2);
    List<DeserializedEvent> infoEvents = profilingReporter.getSentEvents();

    Map<String, Object> snapshotEvent = infoEvents.get(1).getSentEntries();
    assertEquals("info", snapshotEvent.get("Label"));
    assertEquals("profiling", snapshotEvent.get("Spec"));

    assertEquals(stackTrace.length, snapshotEvent.get("FramesCount"));
    assertEquals(thread.getId(), snapshotEvent.get("TID"));
    assertNewFrames(
        Arrays.copyOfRange(stackTrace, 0, 3),
        (Map<String, Map<String, Object>>) snapshotEvent.get("NewFrames"));
  }

  @Test
  @SuppressWarnings("unchecked")
  void partialValidateInfoEventOnRecordWithSkippedFrame() throws SamplingException {
    Thread thread = Thread.currentThread();
    StackTraceElement[] stackTrace = thread.getStackTrace();
    Profile profile = new Profile(profilerSetting);
    Metadata.setup(SamplingConfiguration.builder().build());

    profile.startProfilingOnThread(
        thread, new Metadata("00-970026c88092a447d3b2bba3be3be2fc-0c8fc43138df813a-01"));
    profile.record(thread, stackTrace, 2);
    profile.record(thread, stackTrace, 2);

    StackTraceElement[] newFrames = simulateStackChange(profile);
    List<DeserializedEvent> infoEvents = profilingReporter.getSentEvents();

    Map<String, Object> snapshotEvent = infoEvents.get(2).getSentEntries();
    assertEquals("info", snapshotEvent.get("Label"));
    assertEquals("profiling", snapshotEvent.get("Spec"));

    assertFalse(((Map<?, ?>) snapshotEvent.get("SnapshotsOmitted")).isEmpty());
    assertEquals("profiling", snapshotEvent.get("Spec"));
    assertEquals(2, snapshotEvent.get("FramesExited"));

    assertEquals(newFrames.length, snapshotEvent.get("FramesCount"));
    assertEquals(thread.getId(), snapshotEvent.get("TID"));
    assertNewFrames(
        Arrays.copyOfRange(newFrames, 0, 3),
        (Map<String, Map<String, Object>>) snapshotEvent.get("NewFrames"));
  }

  @Test
  @SuppressWarnings("unchecked")
  void partialValidateInfoEventOnRecordWithFrameChange() throws SamplingException {
    Thread thread = Thread.currentThread();
    StackTraceElement[] stackTrace = thread.getStackTrace();
    Profile profile = new Profile(profilerSetting);
    Metadata.setup(SamplingConfiguration.builder().build());

    profile.startProfilingOnThread(
        thread, new Metadata("00-970026c88092a447d3b2bba3be3be2fc-0c8fc43138df813a-01"));
    profile.record(thread, stackTrace, 2);
    StackTraceElement[] newFrames = simulateStackChange(profile);

    profile.record(thread, stackTrace, 2);
    List<DeserializedEvent> infoEvents = profilingReporter.getSentEvents();

    Map<String, Object> snapshotEvent = infoEvents.get(2).getSentEntries();
    assertEquals("info", snapshotEvent.get("Label"));
    assertEquals("profiling", snapshotEvent.get("Spec"));

    assertTrue(((Map<?, ?>) snapshotEvent.get("SnapshotsOmitted")).isEmpty());
    assertEquals("profiling", snapshotEvent.get("Spec"));
    assertEquals(2, snapshotEvent.get("FramesExited"));

    assertEquals(newFrames.length, snapshotEvent.get("FramesCount"));
    assertEquals(thread.getId(), snapshotEvent.get("TID"));
    assertNewFrames(
        Arrays.copyOfRange(newFrames, 0, 3),
        (Map<String, Map<String, Object>>) snapshotEvent.get("NewFrames"));
  }

  @Test
  void verifyThatProfilingIsStoppedWhenMetadataExpires() throws SamplingException {
    Thread thread = Thread.currentThread();
    StackTraceElement[] stackTrace = thread.getStackTrace();
    Profile profile = new Profile(profilerSetting);

    profile.startProfilingOnThread(
        thread, new Metadata("00-970026c88092a447d3b2bba3be3be2fc-0c8fc43138df813a-01"));
    Profiler.SnapshotTracker snapshotTracker = profile.getSnapshotTracker(thread);
    profile.record(thread, stackTrace, 2);

    profile.record(thread, stackTrace, 2);
    profile.record(thread, stackTrace, 2);
    Metadata.setup(SamplingConfiguration.builder().ttl(0).build()); // set ttl 0 to force expiration

    profile.record(thread, stackTrace, (System.currentTimeMillis() * 1000) << 1);
    /*
       we expect 2 because the two calls to profile.record(..) above should be skipped, and the stack didn't change;
       the last call should trigger a stop due to expiration.
    */
    assertEquals(2, snapshotTracker.getSnapshotsOmitted().size());
    // we expect null because profiling should be stopped by now
    assertNull(profile.getSnapshotTracker(thread));
  }

  private void assertNewFrames(
      StackTraceElement[] expectedNewFrames, Map<String, Map<String, Object>> actualNewFrames) {
    for (int i = 0; i < expectedNewFrames.length; i++) {
      StackTraceElement expectedFrame = expectedNewFrames[i];
      Map<String, Object> actualFrame = actualNewFrames.get(String.valueOf(i));
      assertEquals(expectedFrame.getClassName(), actualFrame.get("C"));

      assertEquals(expectedFrame.getMethodName(), actualFrame.get("M"));
      assertEquals(expectedFrame.getFileName(), actualFrame.get("F"));
      if (expectedFrame.getLineNumber() >= 0) {
        assertEquals(expectedFrame.getLineNumber(), actualFrame.get("L"));
      }
    }
  }

  @Test
  void hasActiveProfilesReturnsFalseWhenNoProfilesExist() {
    assertFalse(Profiler.hasActiveProfiles());
  }

  @Test
  void hasActiveProfilesReturnsTrueAfterAddingProfiledThread() throws SamplingException {
    Metadata.setup(SamplingConfiguration.builder().build());
    Thread thread = Thread.currentThread();
    String traceId = "970026c88092a447d3b2bba3be3be2fc";

    Profiler.addProfiledThread(
        thread, new Metadata("00-970026c88092a447d3b2bba3be3be2fc-0c8fc43138df813a-01"), traceId);
    assertTrue(Profiler.hasActiveProfiles());

    Profiler.stopProfile(traceId);
    assertFalse(Profiler.hasActiveProfiles());
  }

  private StackTraceElement[] simulateStackChange(Profile profile) {
    Thread thread = Thread.currentThread();
    StackTraceElement[] stackTrace = thread.getStackTrace();
    profile.record(thread, stackTrace, 2);

    return stackTrace;
  }
}
