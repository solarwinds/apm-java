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

package com.solarwinds.joboe.core.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

public class TimeUtilsTest {
  private static final int RUN_COUNT = 10000;
  private static final int WARM_UP_COUNT = 1000;

  @Test
  public void testTimeBase() {
    List<Long> diffsInMicroSec = new ArrayList<Long>();

    for (int i = 0; i < RUN_COUNT; i++) {
      long timeUtilsMirco = TimeUtils.getTimestampMicroSeconds();
      long systemMilli = System.currentTimeMillis();

      if (i > WARM_UP_COUNT) {
        diffsInMicroSec.add(Math.abs(systemMilli * 1000 - timeUtilsMirco));
      }
    }

    TimeUtils.Statistics<Long> statistics = new TimeUtils.Statistics<Long>(diffsInMicroSec);

    long median = statistics.getMedian().longValue();
    assertTrue(
        median < 100000,
        "Found median "
            + median); // a rather generous median, this test is just to make sure value is not
    // totally incorrect as unstable system (especially windows) can sometimes
    // give bad shift
  }

  @Test
  public void testTimeDifference() throws InterruptedException {
    List<Long> durationDiscrepanciesInMicroSec;
    TimeUtils.Statistics<Long> statistics;

    durationDiscrepanciesInMicroSec = new ArrayList<Long>();
    for (int i = 0; i < RUN_COUNT; i++) {
      long startMicro = TimeUtils.getTimestampMicroSeconds();
      Thread.sleep(1);
      long endMicro = TimeUtils.getTimestampMicroSeconds();

      assertTrue(
          endMicro > startMicro,
          "end micro is smaller than startMicro: "
              + endMicro
              + " vs "
              + startMicro); // this is the most important! Time should not go backwards

      if (i > WARM_UP_COUNT) {
        durationDiscrepanciesInMicroSec.add(Math.abs((endMicro - startMicro) - 1000));
      }
    }

    statistics = new TimeUtils.Statistics<Long>(durationDiscrepanciesInMicroSec);
    long differenceMedian = statistics.getMedian().longValue();
    assertTrue(
        differenceMedian < 1000,
        "Found median " + differenceMedian); // discrepancy of p95 should be less than 1 millisec
  }

  @Test
  public void testTimeAdjustWorkerInit() throws Exception {
    Method startAdjustBaseWorkerMethod =
        TimeUtils.class.getDeclaredMethod("startAdjustBaseWorker", Integer.class);
    startAdjustBaseWorkerMethod.setAccessible(true);

    assertFalse((Boolean) startAdjustBaseWorkerMethod.invoke(null, 0));
    assertTrue((Boolean) startAdjustBaseWorkerMethod.invoke(null, 100));
  }
}
