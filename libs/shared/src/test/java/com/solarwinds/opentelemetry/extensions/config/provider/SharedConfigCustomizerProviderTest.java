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

package com.solarwinds.opentelemetry.extensions.config.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doNothing;

import com.solarwinds.joboe.config.ConfigProperty;
import io.opentelemetry.sdk.extension.incubator.fileconfig.DeclarativeConfigurationCustomizer;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.AttributeLimitsModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.BatchLogRecordProcessorModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.ExperimentalInstrumentationModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.ExperimentalLanguageSpecificInstrumentationModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.ExperimentalLanguageSpecificInstrumentationPropertyModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.LogRecordExporterModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.LogRecordProcessorModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.LoggerProviderModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.OpenTelemetryConfigurationModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.OtlpGrpcExporterModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.PeriodicMetricReaderModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.SamplerModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.SpanProcessorModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.SpanProcessorPropertyModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.TracerProviderModel;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
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
                new ExperimentalInstrumentationModel()
                    .withJava(
                        new ExperimentalLanguageSpecificInstrumentationModel()
                            .withAdditionalProperty(
                                "solarwinds",
                                new ExperimentalLanguageSpecificInstrumentationPropertyModel()
                                    .withAdditionalProperty(
                                        ConfigProperty.AGENT_SERVICE_KEY.getConfigFileKey(),
                                        "token:service")
                                    .withAdditionalProperty(
                                        ConfigProperty.AGENT_COLLECTOR.getConfigFileKey(),
                                        "apm.collector.com"))));

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
                new ExperimentalInstrumentationModel()
                    .withJava(
                        new ExperimentalLanguageSpecificInstrumentationModel()
                            .withAdditionalProperty(
                                "solarwinds",
                                new ExperimentalLanguageSpecificInstrumentationPropertyModel()
                                    .withAdditionalProperty(
                                        ConfigProperty.AGENT_SERVICE_KEY.getConfigFileKey(),
                                        "token:service")
                                    .withAdditionalProperty(
                                        ConfigProperty.AGENT_COLLECTOR.getConfigFileKey(),
                                        "apm.collector.com"))));

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

    Map<String, Object> configs =
        periodic
            .getExporter()
            .getAdditionalProperties()
            .get(MetricExporterComponentProvider.COMPONENT_NAME)
            .getAdditionalProperties();

    assertNotNull(configs);
    assertEquals(10000, configs.get("timeout"));
    assertEquals("grpc", configs.get("protocol"));
    assertEquals("gzip", configs.get("compression"));

    assertEquals("https://otel.collector.com", configs.get("endpoint"));
    assertEquals("delta", configs.get("temporality_preference"));
    assertEquals(
        "base2_exponential_bucket_histogram", configs.get("default_histogram_aggregation"));

    assertEquals("authorization=Bearer token", configs.get("headers_list"));
  }

  @Test
  void testCustomizeSetsExperimentalStacktraceWhenNotSet() {
    OpenTelemetryConfigurationModel openTelemetryConfigurationModel =
        new OpenTelemetryConfigurationModel()
            .withInstrumentationDevelopment(
                new ExperimentalInstrumentationModel()
                    .withJava(
                        new ExperimentalLanguageSpecificInstrumentationModel()
                            .withAdditionalProperty(
                                "solarwinds",
                                new ExperimentalLanguageSpecificInstrumentationPropertyModel())));

    doNothing()
        .when(declarativeConfigurationCustomizerMock)
        .addModelCustomizer(functionArgumentCaptor.capture());

    tested.customize(declarativeConfigurationCustomizerMock);
    functionArgumentCaptor.getValue().apply(openTelemetryConfigurationModel);
    TracerProviderModel tracerProvider = openTelemetryConfigurationModel.getTracerProvider();

    assertNotNull(tracerProvider);
    assertTrue(
        tracerProvider.getProcessors().stream()
            .anyMatch(
                processorModel ->
                    processorModel
                        .getAdditionalProperties()
                        .containsKey("stacktrace/development")));
  }

  @Test
  @SuppressWarnings("unchecked")
  void testCustomizeSetExperimentalStacktraceFilterWhenNotSet() {
    OpenTelemetryConfigurationModel openTelemetryConfigurationModel =
        new OpenTelemetryConfigurationModel()
            .withTracerProvider(
                new TracerProviderModel()
                    .withProcessors(
                        Collections.singletonList(
                            new SpanProcessorModel()
                                .withAdditionalProperty(
                                    "stacktrace/development", new SpanProcessorPropertyModel()))))
            .withInstrumentationDevelopment(
                new ExperimentalInstrumentationModel()
                    .withJava(
                        new ExperimentalLanguageSpecificInstrumentationModel()
                            .withAdditionalProperty(
                                "solarwinds",
                                new ExperimentalLanguageSpecificInstrumentationPropertyModel())));

    doNothing()
        .when(declarativeConfigurationCustomizerMock)
        .addModelCustomizer(functionArgumentCaptor.capture());

    tested.customize(declarativeConfigurationCustomizerMock);
    functionArgumentCaptor.getValue().apply(openTelemetryConfigurationModel);
    TracerProviderModel tracerProvider = openTelemetryConfigurationModel.getTracerProvider();

    assertNotNull(tracerProvider);
    Optional<Map<String, Object>> map =
        tracerProvider.getProcessors().stream()
            .filter(
                processorModel ->
                    processorModel.getAdditionalProperties().containsKey("stacktrace/development"))
            .map(
                processorModel ->
                    processorModel
                        .getAdditionalProperties()
                        .get("stacktrace/development")
                        .getAdditionalProperties())
            .findFirst();

    assertTrue(map.isPresent());
    assertNotNull(map.get().get("filter"));
  }

  @Test
  void testCustomize2() {
    OpenTelemetryConfigurationModel openTelemetryConfigurationModel =
        new OpenTelemetryConfigurationModel()
            .withInstrumentationDevelopment(
                new ExperimentalInstrumentationModel()
                    .withJava(
                        new ExperimentalLanguageSpecificInstrumentationModel()
                            .withAdditionalProperty(
                                "solarwinds",
                                new ExperimentalLanguageSpecificInstrumentationPropertyModel()
                                    .withAdditionalProperty(
                                        ConfigProperty.AGENT_SERVICE_KEY.getConfigFileKey(),
                                        "token:service")
                                    .withAdditionalProperty(
                                        ConfigProperty.AGENT_COLLECTOR.getConfigFileKey(),
                                        "http://apm.collector.com"))));

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

  @Test
  void UrlShouldNotChangeWhenNotApm() {
    OpenTelemetryConfigurationModel openTelemetryConfigurationModel =
        new OpenTelemetryConfigurationModel()
            .withInstrumentationDevelopment(
                new ExperimentalInstrumentationModel()
                    .withJava(
                        new ExperimentalLanguageSpecificInstrumentationModel()
                            .withAdditionalProperty(
                                "solarwinds",
                                new ExperimentalLanguageSpecificInstrumentationPropertyModel()
                                    .withAdditionalProperty(
                                        ConfigProperty.AGENT_SERVICE_KEY.getConfigFileKey(),
                                        "token:service")
                                    .withAdditionalProperty(
                                        ConfigProperty.AGENT_COLLECTOR.getConfigFileKey(),
                                        "http://example.com"))));

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
    assertEquals("http://example.com", otlp.getEndpoint());
    assertEquals("authorization=Bearer token", otlp.getHeadersList());
  }

  @Test
  void UrlShouldNotChangeWhenNotApm2() {
    OpenTelemetryConfigurationModel openTelemetryConfigurationModel =
        new OpenTelemetryConfigurationModel()
            .withInstrumentationDevelopment(
                new ExperimentalInstrumentationModel()
                    .withJava(
                        new ExperimentalLanguageSpecificInstrumentationModel()
                            .withAdditionalProperty(
                                "solarwinds",
                                new ExperimentalLanguageSpecificInstrumentationPropertyModel()
                                    .withAdditionalProperty(
                                        ConfigProperty.AGENT_SERVICE_KEY.getConfigFileKey(),
                                        "token:service")
                                    .withAdditionalProperty(
                                        ConfigProperty.AGENT_COLLECTOR.getConfigFileKey(),
                                        "http://localhost:4317"))));

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
    assertEquals("http://localhost:4317", otlp.getEndpoint());
    assertEquals("authorization=Bearer token", otlp.getHeadersList());
  }
}
