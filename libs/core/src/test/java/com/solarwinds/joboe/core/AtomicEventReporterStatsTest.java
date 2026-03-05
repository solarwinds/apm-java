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

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class AtomicEventReporterStatsTest {

  private final AtomicEventReporterStats tested = new AtomicEventReporterStats(() -> 10);

  @Test
  void testConsumeStats() {
    tested.incrementFailedCount(1);
    tested.incrementProcessedCount(1);
    tested.incrementOverflowedCount(1);

    tested.incrementSentCount(1);
    tested.setQueueCount(1);

    EventReporterStats expected = new EventReporterStats(1, 1, 1, 1, 1);
    EventReporterStats actual = tested.consumeStats();
    assertEquals(expected, actual);

    expected = new EventReporterStats(0, 0, 0, 10, 0);
    actual = tested.consumeStats();
    assertEquals(expected, actual);
  }
}
