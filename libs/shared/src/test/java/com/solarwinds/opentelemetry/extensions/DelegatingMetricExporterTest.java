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

package com.solarwinds.opentelemetry.extensions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.solarwinds.joboe.config.ConfigManager;
import com.solarwinds.joboe.config.ConfigProperty;
import com.solarwinds.joboe.config.InvalidConfigException;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.common.InstrumentationScopeInfo;
import io.opentelemetry.sdk.common.export.MemoryMode;
import io.opentelemetry.sdk.metrics.Aggregation;
import io.opentelemetry.sdk.metrics.InstrumentType;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DelegatingMetricExporterTest {

  @InjectMocks private DelegatingMetricExporter tested;

  @Mock private MetricExporter metricExporterMock;

  @Mock private MetricData metricDataMock;

  @Mock private MetricData metricDataMock0;

  @Mock private MetricData metricDataMock1;

  @Captor private ArgumentCaptor<List<MetricData>> metricData;

  @Mock private Aggregation aggregationMock;

  @Test
  void verifyGetDefaultAggregationIsDelegated() {
    when(metricExporterMock.getDefaultAggregation(any())).thenReturn(aggregationMock);

    assertEquals(aggregationMock, tested.getDefaultAggregation(InstrumentType.COUNTER));
    verify(metricExporterMock).getDefaultAggregation(InstrumentType.COUNTER);
  }

  @Test
  void verifyGetMemoryModeIsDelegated() {
    when(metricExporterMock.getMemoryMode()).thenReturn(MemoryMode.REUSABLE_DATA);

    assertEquals(MemoryMode.REUSABLE_DATA, tested.getMemoryMode());
    verify(metricExporterMock).getMemoryMode();
  }

  @Test
  void verifyCloseIsDelegated() {
    tested.close();
    verify(metricExporterMock).close();
  }

  @Test
  void verifyWithIsDelegated() {
    when(metricExporterMock.with(any(), any())).thenReturn(metricExporterMock);

    assertEquals(metricExporterMock, tested.with(InstrumentType.COUNTER, aggregationMock));
    verify(metricExporterMock).with(InstrumentType.COUNTER, aggregationMock);
  }

  @Test
  void verifyThatAllMetricDataAreExported() {
    when(metricExporterMock.export(any())).thenReturn(CompletableResultCode.ofSuccess());
    tested.export(Collections.singleton(metricDataMock));

    verify(metricExporterMock).export(any());
  }

  @Test
  void verifyThatOnlySwoMetricDataAreExported() throws InvalidConfigException {
    ConfigManager.setConfig(ConfigProperty.AGENT_EXPORT_METRICS_ENABLED, false);
    when(metricExporterMock.export(any())).thenReturn(CompletableResultCode.ofFailure());

    when(metricDataMock.getInstrumentationScopeInfo())
        .thenReturn(InstrumentationScopeInfo.create(MeterProvider.requestMeterScopeName));
    when(metricDataMock0.getInstrumentationScopeInfo())
        .thenReturn(InstrumentationScopeInfo.create(MeterProvider.samplingMeterScopeName));
    when(metricDataMock1.getInstrumentationScopeInfo())
        .thenReturn(InstrumentationScopeInfo.create("test-scope"));

    tested.export(Arrays.asList(metricDataMock, metricDataMock0, metricDataMock1));
    verify(metricExporterMock).export(metricData.capture());
    assertEquals(Arrays.asList(metricDataMock, metricDataMock0), metricData.getValue());

    ConfigManager.removeConfig(ConfigProperty.AGENT_EXPORT_METRICS_ENABLED);
  }

  @Test
  void verifyThatFlushIsDelegated() {
    tested.flush();
    verify(metricExporterMock).flush();
  }

  @Test
  void verifyShutdownIsDelegated() {
    tested.shutdown();
    verify(metricExporterMock).shutdown();
  }

  @Test
  void verifyGetAggregationTemporalityIsDelegated() {
    tested.getAggregationTemporality(InstrumentType.COUNTER);
    verify(metricExporterMock).getAggregationTemporality(InstrumentType.COUNTER);
  }
}
