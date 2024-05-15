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

package com.solarwinds.opentelemetry.extensions;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.opentelemetry.api.metrics.DoubleGaugeBuilder;
import io.opentelemetry.api.metrics.LongGaugeBuilder;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.metrics.ObservableLongMeasurement;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TraceDecisionMetricCollectorTest {
  private final TraceDecisionMetricCollector tested = new TraceDecisionMetricCollector();

  @Mock private ObservableLongMeasurement observableLongMeasurementMock;

  @Mock private Meter meterMock;

  @Mock private DoubleGaugeBuilder doubleGaugeBuilderMock;

  @Mock private LongGaugeBuilder longGaugeBuilderMock;

  @Mock private AutoConfiguredOpenTelemetrySdk autoConfiguredOpenTelemetrySdkMock;

  @Captor private ArgumentCaptor<Consumer<ObservableLongMeasurement>> consumerArgumentCaptor;

  @Test
  void verifyThatAllGaugesCallbackIsExecutedExcludingQueueBasedOnes() {
    when(meterMock.gaugeBuilder(anyString())).thenReturn(doubleGaugeBuilderMock);
    when(doubleGaugeBuilderMock.ofLongs()).thenReturn(longGaugeBuilderMock);

    tested.collect(meterMock);
    verify(longGaugeBuilderMock, atLeastOnce()).buildWithCallback(consumerArgumentCaptor.capture());

    consumerArgumentCaptor
        .getAllValues()
        .forEach(consumer -> consumer.accept(observableLongMeasurementMock));
    verify(observableLongMeasurementMock, times(6)).record(anyLong());
  }

  @Test
  void verifyMetricsActivated() {
    try (MockedStatic<MeterProvider> meterProviderMock = mockStatic(MeterProvider.class)) {

      meterProviderMock.when(MeterProvider::getSamplingMetricsMeter).thenReturn(meterMock);
      when(meterMock.gaugeBuilder(anyString())).thenReturn(doubleGaugeBuilderMock);
      when(doubleGaugeBuilderMock.ofLongs()).thenReturn(longGaugeBuilderMock);

      tested.afterAgent(autoConfiguredOpenTelemetrySdkMock);
      verify(meterMock, atMost(10)).gaugeBuilder(anyString());
    }
  }
}
