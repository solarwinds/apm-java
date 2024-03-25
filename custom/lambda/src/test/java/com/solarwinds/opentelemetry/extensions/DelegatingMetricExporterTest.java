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
