package com.solarwinds.opentelemetry.extensions.lambda;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.solarwinds.opentelemetry.extensions.lambda.MeterProvider;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.metrics.MeterBuilder;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MeterProviderTest {

  @Mock private MeterBuilder meterBuilderMock;

  @Captor private ArgumentCaptor<String> meterNameCaptor;

  @Test
  void ensureThatConfigureMetersHaveUniqueIdentity() {
    try (MockedStatic<GlobalOpenTelemetry> otelMock = mockStatic(GlobalOpenTelemetry.class)) {
      otelMock
          .when(() -> GlobalOpenTelemetry.meterBuilder(anyString()))
          .thenReturn(meterBuilderMock);
      when(meterBuilderMock.setInstrumentationVersion(anyString())).thenReturn(meterBuilderMock);
      MeterProvider.getRequestMetricsMeter();

      MeterProvider.getSamplingMetricsMeter();
      otelMock.verify(
          () -> GlobalOpenTelemetry.meterBuilder(meterNameCaptor.capture()), atLeast(1));
      List<String> meterNames = meterNameCaptor.getAllValues();

      Set<String> uniqueNames = new HashSet<>(meterNames);
      assertEquals(meterNames.size(), uniqueNames.size());
    }
  }
}
