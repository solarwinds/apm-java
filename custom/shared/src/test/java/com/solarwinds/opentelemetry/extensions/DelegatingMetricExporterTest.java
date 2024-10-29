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

import io.opentelemetry.sdk.metrics.InstrumentType;
import io.opentelemetry.sdk.metrics.data.AggregationTemporality;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DelegatingMetricExporterTest {

  @InjectMocks private DelegatingMetricExporter tested;

  @Mock private MetricExporter metricExporterMock;

  @Test
  void verifyThatExportIsDelegated() {
    tested.export(Collections.emptyList());
    verify(metricExporterMock).export(any());
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
  void verifyThatDeltaAggregationTemporalityIsReturnedForHistogram() {
    AggregationTemporality actual = tested.getAggregationTemporality(InstrumentType.HISTOGRAM);
    assertEquals(AggregationTemporality.DELTA, actual);
  }

  @Test
  void verifyGetAggregationTemporalityForNonHistogramIsDelegated() {
    tested.getAggregationTemporality(InstrumentType.COUNTER);
    verify(metricExporterMock).getAggregationTemporality(InstrumentType.COUNTER);
  }
}
