package com.solarwinds.opentelemetry.extensions;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import io.opentelemetry.sdk.autoconfigure.spi.internal.DefaultConfigProperties;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MetricExporterCustomizerTest {
  @InjectMocks private MetricExporterCustomizer tested;

  @Mock private MetricExporter metricExporterMock;

  @Test
  void verifyThatDelegatingMetricExporterIsReturned() {
    assertInstanceOf(
        DelegatingMetricExporter.class,
        tested.apply(
            metricExporterMock, DefaultConfigProperties.createFromMap(Collections.emptyMap())));
  }
}
