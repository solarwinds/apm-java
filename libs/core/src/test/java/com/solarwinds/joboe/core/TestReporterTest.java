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

import com.solarwinds.joboe.config.InvalidConfigException;
import com.solarwinds.joboe.sampling.Metadata;
import com.solarwinds.joboe.sampling.SamplingConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TestReporterTest {

  @BeforeEach
  void setup() {
    Metadata.setup(SamplingConfiguration.builder().build());
  }

  @Test
  public void testReporterSameThread() throws InvalidConfigException {
    final TestReporter threadLocalReporter = ReporterFactory.getInstance().createTestReporter(true);
    final TestReporter nonThreadLocalReporter =
        ReporterFactory.getInstance().createTestReporter(false);

    Event event;

    event = startTrace();
    event.report(threadLocalReporter);

    event = startTrace();
    event.report(nonThreadLocalReporter);

    assertEquals(1, threadLocalReporter.getSentEvents().size());
    assertEquals(1, nonThreadLocalReporter.getSentEvents().size());
  }

  @Test
  public void testReporterDifferentThread() throws InvalidConfigException, InterruptedException {
    final TestReporter threadLocalReporter = ReporterFactory.getInstance().createTestReporter(true);
    final TestReporter nonThreadLocalReporter =
        ReporterFactory.getInstance().createTestReporter(false);

    Thread thread =
        new Thread(
            () -> {
              Event event;

              event = startTrace();
              event.report(threadLocalReporter);

              event = startTrace();
              event.report(nonThreadLocalReporter);
            });

    thread.start();
    thread.join();

    assertEquals(
        0,
        threadLocalReporter.getSentEvents().size()); // different thread, should not get the event
    assertEquals(1, nonThreadLocalReporter.getSentEvents().size());
  }

  private Event startTrace() {
    Metadata md = new Metadata();
    md.randomize(true);
    Context.setMetadata(md);
    return Context.createEventWithContext(md, false);
  }
}
