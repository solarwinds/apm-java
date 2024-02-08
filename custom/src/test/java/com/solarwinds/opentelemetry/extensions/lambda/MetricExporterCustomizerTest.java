package com.solarwinds.opentelemetry.extensions.lambda;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mockStatic;

import com.solarwinds.joboe.core.util.HostTypeDetector;
import io.opentelemetry.sdk.autoconfigure.spi.internal.DefaultConfigProperties;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MetricExporterCustomizerTest {
  @InjectMocks private MetricExporterCustomizer tested;

  @Mock private MetricExporter metricExporterMock;

  @Test
  void verifyThatDelegatingMetricExporterIsReturned() {
    try (MockedStatic<HostTypeDetector> hostTypeDetectorMockedStatic =
        mockStatic(HostTypeDetector.class)) {
      hostTypeDetectorMockedStatic.when(HostTypeDetector::isLambda).thenReturn(true);
      assertInstanceOf(
          DelegatingMetricExporter.class,
          tested.apply(
              metricExporterMock, DefaultConfigProperties.createFromMap(Collections.emptyMap())));
    }
  }

  @Test
  void verifyThatDelegatingMetricExporterIsNotReturned() {
    try (MockedStatic<HostTypeDetector> hostTypeDetectorMockedStatic =
        mockStatic(HostTypeDetector.class)) {
      hostTypeDetectorMockedStatic.when(HostTypeDetector::isLambda).thenReturn(false);
      assertEquals(
          metricExporterMock,
          tested.apply(
              metricExporterMock, DefaultConfigProperties.createFromMap(Collections.emptyMap())));
    }
  }
}
