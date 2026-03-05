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

package com.solarwinds.joboe.sampling;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SettingsManagerTest {

  @Captor private ArgumentCaptor<SettingsListener> settingsListenerArgumentCaptor;

  @Mock private SettingsFetcher settingsFetcherMock;

  @Test
  public void testArgChangeListener() {
    TestArgChangeListener<Double> bucketCapacityListener =
        new TestArgChangeListener<Double>(SettingsArg.BUCKET_CAPACITY);
    TestArgChangeListener<Integer> metricsFlushIntervalListener =
        new TestArgChangeListener<Integer>(SettingsArg.METRIC_FLUSH_INTERVAL);
    SettingsManager.registerListener(bucketCapacityListener);

    SettingsManager.registerListener(metricsFlushIntervalListener);
    SettingsManager.initializeFetcher(settingsFetcherMock);
    verify(settingsFetcherMock).registerListener(settingsListenerArgumentCaptor.capture());

    // test settings args
    Map<SettingsArg<?>, Object> args = new HashMap<SettingsArg<?>, Object>();
    args.put(SettingsArg.BUCKET_CAPACITY, 1.0);
    args.put(SettingsArg.METRIC_FLUSH_INTERVAL, 5);

    settingsListenerArgumentCaptor
        .getValue()
        .onSettingsRetrieved(
            SettingsStub.builder()
                .withFlags(TracingMode.ALWAYS)
                .withSampleRate(TraceDecisionUtil.SAMPLE_RESOLUTION)
                .withSettingsArgs(args)
                .build());
    assertEquals(1.0, bucketCapacityListener.newValue);
    assertEquals((Integer) 5, metricsFlushIntervalListener.newValue);

    // test settings args changes
    args.clear();
    args.put(SettingsArg.BUCKET_CAPACITY, 2.0);
    args.put(SettingsArg.METRIC_FLUSH_INTERVAL, 6);

    settingsListenerArgumentCaptor
        .getValue()
        .onSettingsRetrieved(
            SettingsStub.builder()
                .withFlags(TracingMode.ALWAYS)
                .withSampleRate(TraceDecisionUtil.SAMPLE_RESOLUTION)
                .withSettingsArgs(args)
                .build());

    assertEquals(2.0, bucketCapacityListener.newValue);
    assertEquals((Integer) 6, metricsFlushIntervalListener.newValue);

    // test settings args set to null from non null
    settingsListenerArgumentCaptor
        .getValue()
        .onSettingsRetrieved(
            SettingsStub.builder()
                .withFlags(TracingMode.ALWAYS)
                .withSampleRate(TraceDecisionUtil.SAMPLE_RESOLUTION)
                .withSettingsArgs(Collections.emptyMap())
                .build());

    assertNull(bucketCapacityListener.newValue);
    assertNull(metricsFlushIntervalListener.newValue);

    // one of the test args changed from null to some value
    args.clear();
    args.put(SettingsArg.BUCKET_CAPACITY, 3.0);
    settingsListenerArgumentCaptor
        .getValue()
        .onSettingsRetrieved(
            SettingsStub.builder()
                .withFlags(TracingMode.ALWAYS)
                .withSampleRate(TraceDecisionUtil.SAMPLE_RESOLUTION)
                .withSettingsArgs(args)
                .build());

    assertEquals(3.0, bucketCapacityListener.newValue); // the value has not been changed
    assertNull(metricsFlushIntervalListener.newValue); // value has been changed
    bucketCapacityListener.newValue = null; // reset listener

    // test values unchanged
    assertNull(bucketCapacityListener.newValue); // the value has not been changed
    assertNull(metricsFlushIntervalListener.newValue); // the value has not been changed

    SettingsManager.removeListener(bucketCapacityListener);
    SettingsManager.removeListener(metricsFlushIntervalListener);
  }

  @Test
  public void testGetSettings() {
    final CountDownLatch countDownLatch = new CountDownLatch(1);
    when(settingsFetcherMock.getSettings())
        .thenReturn(
            SettingsStub.builder()
                .withFlags(TracingMode.ALWAYS)
                .withSampleRate(TraceDecisionUtil.SAMPLE_RESOLUTION)
                .build());
    when(settingsFetcherMock.isSettingsAvailableLatch()).thenReturn(countDownLatch);

    // simulate a 5 secs delay for fetching
    new Thread() {
      @Override
      public void run() {
        try {
          TimeUnit.SECONDS.sleep(5);
          countDownLatch.countDown();
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    }.start();

    SettingsManager.initializeFetcher(settingsFetcherMock);

    assert (SettingsManager.getSettings(2, TimeUnit.SECONDS)
        == null); // should give null, as 2 sec < 5 sec
    assert (SettingsManager.getSettings(4, TimeUnit.SECONDS)
        != null); // should not be null as 6 sec > 5 sec
  }

  private static class TestArgChangeListener<T> extends SettingsArgChangeListener<T> {
    private T newValue;

    public TestArgChangeListener(SettingsArg<T> type) {
      super(type);
    }

    @Override
    public void onChange(T newValue) {
      this.newValue = newValue;
    }
  }
}
