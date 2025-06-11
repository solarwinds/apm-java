package com.solarwinds.opentelemetry.extensions.config.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doNothing;

import com.solarwinds.joboe.config.ConfigProperty;
import io.opentelemetry.sdk.extension.incubator.fileconfig.DeclarativeConfigurationCustomizer;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.AttributeLimitsModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.BatchLogRecordProcessorModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.ExperimentalLanguageSpecificInstrumentationModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.InstrumentationModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.LogRecordExporterModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.LogRecordProcessorModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.LoggerProviderModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.OpenTelemetryConfigurationModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.OtlpGrpcExporterModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.PeriodicMetricReaderModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.SamplerModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.TracerProviderModel;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SharedConfigCustomizerProviderTest {

  @InjectMocks private SharedConfigCustomizerProvider tested;

  @Mock private DeclarativeConfigurationCustomizer declarativeConfigurationCustomizerMock;

  @Captor
  private ArgumentCaptor<Function<OpenTelemetryConfigurationModel, OpenTelemetryConfigurationModel>>
      functionArgumentCaptor;

  @Test
  void testCustomize() {
    OpenTelemetryConfigurationModel openTelemetryConfigurationModel =
        new OpenTelemetryConfigurationModel()
            .withInstrumentationDevelopment(
                new InstrumentationModel()
                    .withJava(
                        new ExperimentalLanguageSpecificInstrumentationModel()
                            .withAdditionalProperty(
                                "solarwinds",
                                new HashMap<String, String>() {
                                  {
                                    put(
                                        ConfigProperty.AGENT_SERVICE_KEY.getConfigFileKey(),
                                        "token:service");
                                    put(
                                        ConfigProperty.AGENT_COLLECTOR.getConfigFileKey(),
                                        "apm.collector.com");
                                  }
                                })));

    doNothing()
        .when(declarativeConfigurationCustomizerMock)
        .addModelCustomizer(functionArgumentCaptor.capture());

    tested.customize(declarativeConfigurationCustomizerMock);
    functionArgumentCaptor.getValue().apply(openTelemetryConfigurationModel);

    AttributeLimitsModel attributeLimits = openTelemetryConfigurationModel.getAttributeLimits();
    assertEquals(new AttributeLimitsModel().withAttributeCountLimit(128), attributeLimits);

    TracerProviderModel tracerProvider = openTelemetryConfigurationModel.getTracerProvider();
    assertNotNull(tracerProvider);

    SamplerModel sampler = tracerProvider.getSampler();
    assertNotNull(sampler.getAdditionalProperties().get(SamplerComponentProvider.COMPONENT_NAME));
    assertEquals(3, tracerProvider.getProcessors().size());

    assertTrue(
        openTelemetryConfigurationModel
            .getPropagator()
            .getCompositeList()
            .contains(ContextPropagatorComponentProvider.COMPONENT_NAME));
    PeriodicMetricReaderModel periodic =
        openTelemetryConfigurationModel.getMeterProvider().getReaders().get(0).getPeriodic();

    assertNotNull(periodic);
    assertNotNull(
        periodic
            .getExporter()
            .getAdditionalProperties()
            .get(MetricExporterComponentProvider.COMPONENT_NAME));

    LoggerProviderModel loggerProvider = openTelemetryConfigurationModel.getLoggerProvider();
    LogRecordProcessorModel logRecordProcessorModel = loggerProvider.getProcessors().get(0);
    BatchLogRecordProcessorModel batch = logRecordProcessorModel.getBatch();

    assertNotNull(batch);
    LogRecordExporterModel exporter = batch.getExporter();
    OtlpGrpcExporterModel otlp = exporter.getOtlpGrpc();

    assertNotNull(otlp);
    assertEquals("https://otel.collector.com", otlp.getEndpoint());
    assertEquals("authorization=Bearer token", otlp.getHeadersList());

    assertEquals("gzip", otlp.getCompression());
  }

  @Test
  void testCustomize1() {
    OpenTelemetryConfigurationModel openTelemetryConfigurationModel =
        new OpenTelemetryConfigurationModel()
            .withInstrumentationDevelopment(
                new InstrumentationModel()
                    .withJava(
                        new ExperimentalLanguageSpecificInstrumentationModel()
                            .withAdditionalProperty(
                                "solarwinds",
                                new HashMap<String, String>() {
                                  {
                                    put(
                                        ConfigProperty.AGENT_SERVICE_KEY.getConfigFileKey(),
                                        "token:service");
                                    put(
                                        ConfigProperty.AGENT_COLLECTOR.getConfigFileKey(),
                                        "apm.collector.com");
                                  }
                                })));

    doNothing()
        .when(declarativeConfigurationCustomizerMock)
        .addModelCustomizer(functionArgumentCaptor.capture());

    tested.customize(declarativeConfigurationCustomizerMock);
    functionArgumentCaptor.getValue().apply(openTelemetryConfigurationModel);

    AttributeLimitsModel attributeLimits = openTelemetryConfigurationModel.getAttributeLimits();
    assertEquals(new AttributeLimitsModel().withAttributeCountLimit(128), attributeLimits);

    TracerProviderModel tracerProvider = openTelemetryConfigurationModel.getTracerProvider();
    assertNotNull(tracerProvider);

    SamplerModel sampler = tracerProvider.getSampler();
    assertNotNull(sampler.getAdditionalProperties().get(SamplerComponentProvider.COMPONENT_NAME));
    assertEquals(3, tracerProvider.getProcessors().size());

    assertTrue(
        openTelemetryConfigurationModel
            .getPropagator()
            .getCompositeList()
            .contains(ContextPropagatorComponentProvider.COMPONENT_NAME));
    PeriodicMetricReaderModel periodic =
        openTelemetryConfigurationModel.getMeterProvider().getReaders().get(0).getPeriodic();

    @SuppressWarnings("unchecked")
    Map<String, Object> configs =
        (Map<String, Object>)
            periodic
                .getExporter()
                .getAdditionalProperties()
                .get(MetricExporterComponentProvider.COMPONENT_NAME);
    assertNotNull(configs);

    assertEquals(10000, configs.get("timeout"));
    assertEquals("grpc", configs.get("protocol"));
    assertEquals("gzip", configs.get("compression"));

    assertEquals("https://otel.collector.com", configs.get("endpoint"));
    assertEquals("cumulative", configs.get("temporality_preference"));
    assertEquals("explicit_bucket_histogram", configs.get("default_histogram_aggregation"));

    assertEquals("authorization=Bearer token", configs.get("headers_list"));
  }

  @Test
  void testCustomize2() {
    OpenTelemetryConfigurationModel openTelemetryConfigurationModel =
        new OpenTelemetryConfigurationModel()
            .withInstrumentationDevelopment(
                new InstrumentationModel()
                    .withJava(
                        new ExperimentalLanguageSpecificInstrumentationModel()
                            .withAdditionalProperty(
                                "solarwinds",
                                new HashMap<String, String>() {
                                  {
                                    put(
                                        ConfigProperty.AGENT_SERVICE_KEY.getConfigFileKey(),
                                        "token:service");
                                    put(
                                        ConfigProperty.AGENT_COLLECTOR.getConfigFileKey(),
                                        "http://apm.collector.com");
                                  }
                                })));

    doNothing()
        .when(declarativeConfigurationCustomizerMock)
        .addModelCustomizer(functionArgumentCaptor.capture());

    tested.customize(declarativeConfigurationCustomizerMock);
    functionArgumentCaptor.getValue().apply(openTelemetryConfigurationModel);

    LoggerProviderModel loggerProvider = openTelemetryConfigurationModel.getLoggerProvider();
    LogRecordProcessorModel logRecordProcessorModel = loggerProvider.getProcessors().get(0);
    BatchLogRecordProcessorModel batch = logRecordProcessorModel.getBatch();

    assertNotNull(batch);
    LogRecordExporterModel exporter = batch.getExporter();
    OtlpGrpcExporterModel otlp = exporter.getOtlpGrpc();

    assertNotNull(otlp);
    assertEquals("https://otel.collector.com", otlp.getEndpoint());
    assertEquals("authorization=Bearer token", otlp.getHeadersList());
  }
}
