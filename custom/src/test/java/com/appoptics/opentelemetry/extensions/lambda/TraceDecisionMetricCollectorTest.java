package com.appoptics.opentelemetry.extensions.lambda;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

import com.solarwinds.joboe.SampleRateSource;
import com.solarwinds.joboe.TraceConfig;
import com.solarwinds.joboe.TraceDecisionUtil;
import com.solarwinds.metrics.measurement.SimpleMeasurementMetricsEntry;
import com.solarwinds.util.HostTypeDetector;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.DoubleGaugeBuilder;
import io.opentelemetry.api.metrics.LongGaugeBuilder;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.metrics.ObservableLongMeasurement;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import java.util.ArrayDeque;
import java.util.HashMap;
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

  @Captor private ArgumentCaptor<Attributes> attributesArgumentCaptor;

  @Captor private ArgumentCaptor<Long> longArgumentCaptor;

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
  void verifyThatEmptyQueueIsNotQueried() {
    tested.record(observableLongMeasurementMock, new ArrayDeque<>());
    verify(observableLongMeasurementMock, never()).record(anyLong());
    verify(observableLongMeasurementMock, never()).record(anyLong(), any());
  }

  @Test
  void verifyThatNonEmptyQueueIsQueried() {
    HashMap<String, String> tags =
        new HashMap<>() {
          {
            put("tag", "value");
          }
        };
    ArrayDeque<SimpleMeasurementMetricsEntry> deque = new ArrayDeque<>();
    deque.add(new SimpleMeasurementMetricsEntry("test", tags, 9));

    tested.record(observableLongMeasurementMock, deque);
    verify(observableLongMeasurementMock)
        .record(longArgumentCaptor.capture(), attributesArgumentCaptor.capture());

    assertEquals(9, longArgumentCaptor.getValue());
    Attributes attributes = attributesArgumentCaptor.getValue();

    String tagValue = attributes.get(AttributeKey.stringKey("tag"));
    assertEquals("value", tagValue);
  }

  @Test
  void verifyThatQueueIsPopulated() {
    try (MockedStatic<TraceDecisionUtil> mocked = mockStatic(TraceDecisionUtil.class)) {
      mocked
          .when(TraceDecisionUtil::consumeLastTraceConfigs)
          .thenReturn(
              new HashMap<String, TraceConfig>() {
                {
                  put("one", new TraceConfig(90, SampleRateSource.FILE, (short) 1));
                }
              });
      tested.consumeTraceConfigs(observableLongMeasurementMock);
      assertFalse(tested.getSampleRateQueue().isEmpty());
      assertFalse(tested.getSampleSourceQueue().isEmpty());
    }
  }

  @Test
  void verifyMetricsActivatedInLambda() {
    try (MockedStatic<HostTypeDetector> mockedHD = mockStatic(HostTypeDetector.class);
        MockedStatic<MeterProvider> meterProviderMock = mockStatic(MeterProvider.class)) {

      meterProviderMock.when(MeterProvider::getSamplingMetricsMeter).thenReturn(meterMock);
      mockedHD.when(HostTypeDetector::isLambda).thenReturn(true);

      when(meterMock.gaugeBuilder(anyString())).thenReturn(doubleGaugeBuilderMock);
      when(doubleGaugeBuilderMock.ofLongs()).thenReturn(longGaugeBuilderMock);

      tested.afterAgent(autoConfiguredOpenTelemetrySdkMock);
      verify(meterMock, atMost(10)).gaugeBuilder(anyString());
    }
  }

  @Test
  void verifyMetricsNotActivatedWhenNotLambda() {
    try (MockedStatic<HostTypeDetector> mockedHD = mockStatic(HostTypeDetector.class);
        MockedStatic<MeterProvider> meterProviderMock = mockStatic(MeterProvider.class)) {

      meterProviderMock.when(MeterProvider::getSamplingMetricsMeter).thenReturn(meterMock);
      mockedHD.when(HostTypeDetector::isLambda).thenReturn(false);

      tested.afterAgent(autoConfiguredOpenTelemetrySdkMock);
      verify(meterMock, never()).gaugeBuilder(anyString());
    }
  }
}
