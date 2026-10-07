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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doNothing;

import com.solarwinds.joboe.config.ConfigManager;
import com.solarwinds.joboe.config.ConfigProperty;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.DeclarativeConfigurationCustomizer;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.AttributeLimitsModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.BatchLogRecordProcessorModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.BatchSpanProcessorModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.DistributionModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.LogRecordExporterModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.LogRecordProcessorModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.LoggerProviderModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.MeterProviderModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.MetricReaderModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.OpenTelemetryConfigurationModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.PeriodicMetricReaderModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.PropagatorModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.SamplerModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.SimpleLogRecordProcessorModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.SimpleSpanProcessorModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.SpanExporterModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.SpanProcessorModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.TracerProviderModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.internal.ExperimentalInstrumentationModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.internal.ExperimentalLanguageSpecificInstrumentationModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.internal.ExperimentalLanguageSpecificInstrumentationPropertyModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.internal.OpenTelemetryConfigurationModelAccessor;
import java.util.Collections;
import java.util.HashMap;
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

@SuppressWarnings("all")
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
        withInstrumentation(
            new OpenTelemetryConfigurationModel()
                .setTracerProvider(new TracerProviderModel().setProcessors(Collections.emptyList()))
                .setLoggerProvider(
                    new LoggerProviderModel().setProcessors(Collections.emptyList())),
            new ExperimentalInstrumentationModel()
                .setJava(
                    new ExperimentalLanguageSpecificInstrumentationModel()
                        .setAdditionalProperty(
                            "solarwinds",
                            new ExperimentalLanguageSpecificInstrumentationPropertyModel()
                                .setAdditionalProperty(
                                    ConfigProperty.AGENT_SERVICE_KEY.getConfigFileKey(),
                                    "token:service")
                                .setAdditionalProperty(
                                    ConfigProperty.AGENT_COLLECTOR.getConfigFileKey(),
                                    "apm.collector.com"))));

    doNothing()
        .when(declarativeConfigurationCustomizerMock)
        .addModelCustomizer(functionArgumentCaptor.capture());

    tested.customize(declarativeConfigurationCustomizerMock);
    functionArgumentCaptor.getValue().apply(openTelemetryConfigurationModel);

    AttributeLimitsModel attributeLimits = openTelemetryConfigurationModel.getAttributeLimits();
    assertEquals(new AttributeLimitsModel().setAttributeCountLimit(128), attributeLimits);

    TracerProviderModel tracerProvider = openTelemetryConfigurationModel.getTracerProvider();
    assertNotNull(tracerProvider);

    SamplerModel sampler = tracerProvider.getSampler();
    assertNotNull(sampler.getExtensionProperties().get(SamplerComponentProvider.COMPONENT_NAME));
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
            .getExtensionProperties()
            .get(MetricExporterComponentProvider.COMPONENT_NAME));

    LoggerProviderModel loggerProvider = openTelemetryConfigurationModel.getLoggerProvider();
    LogRecordProcessorModel logRecordProcessorModel = loggerProvider.getProcessors().get(0);
    BatchLogRecordProcessorModel batch = logRecordProcessorModel.getBatch();

    assertNotNull(batch);
    LogRecordExporterModel exporter = batch.getExporter();
    Map<String, Object> logExporterProperty =
        asMap(exporter.getExtensionProperties().get(LogExporterComponentProvider.COMPONENT_NAME));

    assertNotNull(logExporterProperty);
    Map<String, Object> logConfigs = logExporterProperty;
    assertEquals("https://otel.collector.com/v1/logs", logConfigs.get("endpoint"));
    assertEquals("authorization=Bearer token", logConfigs.get("headers_list"));
    assertEquals("gzip", logConfigs.get("compression"));
  }

  @Test
  void testCustomize1() {
    OpenTelemetryConfigurationModel openTelemetryConfigurationModel =
        withInstrumentation(
            new OpenTelemetryConfigurationModel()
                .setTracerProvider(
                    new TracerProviderModel().setProcessors(Collections.emptyList())),
            new ExperimentalInstrumentationModel()
                .setJava(
                    new ExperimentalLanguageSpecificInstrumentationModel()
                        .setAdditionalProperty(
                            "solarwinds",
                            new ExperimentalLanguageSpecificInstrumentationPropertyModel()
                                .setAdditionalProperty(
                                    ConfigProperty.AGENT_SERVICE_KEY.getConfigFileKey(),
                                    "token:service")
                                .setAdditionalProperty(
                                    ConfigProperty.AGENT_COLLECTOR.getConfigFileKey(),
                                    "apm.collector.com"))));

    doNothing()
        .when(declarativeConfigurationCustomizerMock)
        .addModelCustomizer(functionArgumentCaptor.capture());

    tested.customize(declarativeConfigurationCustomizerMock);
    functionArgumentCaptor.getValue().apply(openTelemetryConfigurationModel);

    AttributeLimitsModel attributeLimits = openTelemetryConfigurationModel.getAttributeLimits();
    assertEquals(new AttributeLimitsModel().setAttributeCountLimit(128), attributeLimits);

    TracerProviderModel tracerProvider = openTelemetryConfigurationModel.getTracerProvider();
    assertNotNull(tracerProvider);

    SamplerModel sampler = tracerProvider.getSampler();
    assertNotNull(sampler.getExtensionProperties().get(SamplerComponentProvider.COMPONENT_NAME));
    assertEquals(3, tracerProvider.getProcessors().size());

    assertTrue(
        openTelemetryConfigurationModel
            .getPropagator()
            .getCompositeList()
            .contains(ContextPropagatorComponentProvider.COMPONENT_NAME));
    PeriodicMetricReaderModel periodic =
        openTelemetryConfigurationModel.getMeterProvider().getReaders().get(0).getPeriodic();

    Map<String, Object> configs =
        asMap(
            periodic
                .getExporter()
                .getExtensionProperties()
                .get(MetricExporterComponentProvider.COMPONENT_NAME));

    assertNotNull(configs);
    assertEquals(10000, configs.get("timeout"));
    assertEquals("http/protobuf", configs.get("protocol"));
    assertEquals("gzip", configs.get("compression"));

    assertEquals("https://otel.collector.com/v1/metrics", configs.get("endpoint"));
    assertEquals("delta", configs.get("temporality_preference"));
    assertEquals(
        "base2_exponential_bucket_histogram", configs.get("default_histogram_aggregation"));

    assertEquals("authorization=Bearer token", configs.get("headers_list"));
  }

  @Test
  void customizeShouldNotSetMetricReaderWhenOneIsSpecified() {
    OpenTelemetryConfigurationModel openTelemetryConfigurationModel =
        withInstrumentation(
            new OpenTelemetryConfigurationModel()
                .setMeterProvider(
                    new MeterProviderModel()
                        .setReaders(Collections.singletonList(new MetricReaderModel()))),
            new ExperimentalInstrumentationModel()
                .setJava(
                    new ExperimentalLanguageSpecificInstrumentationModel()
                        .setAdditionalProperty(
                            "solarwinds",
                            new ExperimentalLanguageSpecificInstrumentationPropertyModel()
                                .setAdditionalProperty(
                                    ConfigProperty.AGENT_SERVICE_KEY.getConfigFileKey(),
                                    "token:service")
                                .setAdditionalProperty(
                                    ConfigProperty.AGENT_COLLECTOR.getConfigFileKey(),
                                    "apm.collector.com"))));

    doNothing()
        .when(declarativeConfigurationCustomizerMock)
        .addModelCustomizer(functionArgumentCaptor.capture());

    tested.customize(declarativeConfigurationCustomizerMock);
    functionArgumentCaptor.getValue().apply(openTelemetryConfigurationModel);

    MeterProviderModel meterProvider = openTelemetryConfigurationModel.getMeterProvider();
    MetricReaderModel metricReaderModel = meterProvider.getReaders().get(0);
    assertNull(metricReaderModel.getPeriodic());
  }

  @Test
  void customizeShouldNotSetTraceExporterWhenBatchIsSpecified() {
    OpenTelemetryConfigurationModel openTelemetryConfigurationModel =
        withInstrumentation(
            new OpenTelemetryConfigurationModel()
                .setTracerProvider(
                    new TracerProviderModel()
                        .setProcessors(
                            Collections.singletonList(
                                new SpanProcessorModel()
                                    .setBatch(
                                        new BatchSpanProcessorModel()
                                            .setExporter(new SpanExporterModel()))))),
            new ExperimentalInstrumentationModel()
                .setJava(
                    new ExperimentalLanguageSpecificInstrumentationModel()
                        .setAdditionalProperty(
                            "solarwinds",
                            new ExperimentalLanguageSpecificInstrumentationPropertyModel()
                                .setAdditionalProperty(
                                    ConfigProperty.AGENT_SERVICE_KEY.getConfigFileKey(),
                                    "token:service")
                                .setAdditionalProperty(
                                    ConfigProperty.AGENT_COLLECTOR.getConfigFileKey(),
                                    "apm.collector.com"))));

    doNothing()
        .when(declarativeConfigurationCustomizerMock)
        .addModelCustomizer(functionArgumentCaptor.capture());

    tested.customize(declarativeConfigurationCustomizerMock);
    functionArgumentCaptor.getValue().apply(openTelemetryConfigurationModel);

    TracerProviderModel tracerProvider = openTelemetryConfigurationModel.getTracerProvider();
    SpanProcessorModel spanProcessorModel = tracerProvider.getProcessors().get(0);
    BatchSpanProcessorModel batch = spanProcessorModel.getBatch();

    assertNotNull(batch);
    SpanExporterModel exporter = batch.getExporter();
    Map<String, Object> spanExporterPropertyModel =
        asMap(exporter.getExtensionProperties().get(SpanExporterComponentProvider.COMPONENT_NAME));

    assertNull(spanExporterPropertyModel);
  }

  @Test
  void customizeShouldNotSetTraceExporterWhenSimpleIsSpecified() {
    OpenTelemetryConfigurationModel openTelemetryConfigurationModel =
        withInstrumentation(
            new OpenTelemetryConfigurationModel()
                .setTracerProvider(
                    new TracerProviderModel()
                        .setProcessors(
                            Collections.singletonList(
                                new SpanProcessorModel()
                                    .setSimple(
                                        new SimpleSpanProcessorModel()
                                            .setExporter(new SpanExporterModel()))))),
            new ExperimentalInstrumentationModel()
                .setJava(
                    new ExperimentalLanguageSpecificInstrumentationModel()
                        .setAdditionalProperty(
                            "solarwinds",
                            new ExperimentalLanguageSpecificInstrumentationPropertyModel()
                                .setAdditionalProperty(
                                    ConfigProperty.AGENT_SERVICE_KEY.getConfigFileKey(),
                                    "token:service")
                                .setAdditionalProperty(
                                    ConfigProperty.AGENT_COLLECTOR.getConfigFileKey(),
                                    "apm.collector.com"))));

    doNothing()
        .when(declarativeConfigurationCustomizerMock)
        .addModelCustomizer(functionArgumentCaptor.capture());

    tested.customize(declarativeConfigurationCustomizerMock);
    functionArgumentCaptor.getValue().apply(openTelemetryConfigurationModel);

    TracerProviderModel tracerProvider = openTelemetryConfigurationModel.getTracerProvider();
    SpanProcessorModel spanProcessorModel = tracerProvider.getProcessors().get(0);
    SimpleSpanProcessorModel simple = spanProcessorModel.getSimple();

    assertNotNull(simple);
    SpanExporterModel exporter = simple.getExporter();
    Map<String, Object> spanExporterPropertyModel =
        asMap(exporter.getExtensionProperties().get(SpanExporterComponentProvider.COMPONENT_NAME));

    assertNull(spanExporterPropertyModel);
  }

  @Test
  void testCustomizeSetsExperimentalStacktraceWhenNotSet() {
    OpenTelemetryConfigurationModel openTelemetryConfigurationModel =
        withInstrumentation(
            new OpenTelemetryConfigurationModel()
                .setTracerProvider(
                    new TracerProviderModel().setProcessors(Collections.emptyList())),
            new ExperimentalInstrumentationModel()
                .setJava(
                    new ExperimentalLanguageSpecificInstrumentationModel()
                        .setAdditionalProperty(
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
                    processorModel.getExtensionProperties().containsKey("stacktrace/development")));
  }

  @Test
  @SuppressWarnings("unchecked")
  void testCustomizeSetExperimentalStacktraceFilterWhenNotSet() {
    OpenTelemetryConfigurationModel openTelemetryConfigurationModel =
        withInstrumentation(
            new OpenTelemetryConfigurationModel()
                .setTracerProvider(
                    new TracerProviderModel()
                        .setProcessors(
                            Collections.singletonList(
                                new SpanProcessorModel()
                                    .setExtensionProperty(
                                        "stacktrace/development", new HashMap<String, Object>())))),
            new ExperimentalInstrumentationModel()
                .setJava(
                    new ExperimentalLanguageSpecificInstrumentationModel()
                        .setAdditionalProperty(
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
                    processorModel.getExtensionProperties().containsKey("stacktrace/development"))
            .map(
                processorModel ->
                    asMap(processorModel.getExtensionProperties().get("stacktrace/development")))
            .findFirst();

    assertTrue(map.isPresent());
    assertNotNull(map.get().get("filter"));
  }

  @Test
  void testCustomizeSetsStacktraceFilterWhenStacktraceValueIsNotAMap() {
    Map<String, Object> stacktrace = customizeWithStacktrace(null);

    assertNotNull(stacktrace.get("filter"));
  }

  @Test
  void testCustomizeKeepsUserStacktraceFilterAndOtherProperties() {
    Map<String, Object> userProperties = new HashMap<>();
    userProperties.put("filter", "com.example.UserFilter");
    userProperties.put("min_duration", 5);

    Map<String, Object> stacktrace = customizeWithStacktrace(userProperties);

    assertEquals("com.example.UserFilter", stacktrace.get("filter"));
    assertEquals(5, stacktrace.get("min_duration"));
  }

  @Test
  void testCustomizeAddsStacktraceFilterAndKeepsOtherProperties() {
    Map<String, Object> userProperties = new HashMap<>();
    userProperties.put("min_duration", 5);

    Map<String, Object> stacktrace = customizeWithStacktrace(userProperties);

    assertNotNull(stacktrace.get("filter"));
    assertEquals(5, stacktrace.get("min_duration"));
  }

  private Map<String, Object> customizeWithStacktrace(Object stacktraceValue) {
    OpenTelemetryConfigurationModel model =
        withInstrumentation(
            new OpenTelemetryConfigurationModel()
                .setTracerProvider(
                    new TracerProviderModel()
                        .setProcessors(
                            Collections.singletonList(
                                new SpanProcessorModel()
                                    .setExtensionProperty(
                                        "stacktrace/development", stacktraceValue)))),
            new ExperimentalInstrumentationModel()
                .setJava(
                    new ExperimentalLanguageSpecificInstrumentationModel()
                        .setAdditionalProperty(
                            "solarwinds",
                            new ExperimentalLanguageSpecificInstrumentationPropertyModel())));

    doNothing()
        .when(declarativeConfigurationCustomizerMock)
        .addModelCustomizer(functionArgumentCaptor.capture());

    tested.customize(declarativeConfigurationCustomizerMock);
    functionArgumentCaptor.getValue().apply(model);

    return model.getTracerProvider().getProcessors().stream()
        .filter(p -> p.getExtensionProperties().containsKey("stacktrace/development"))
        .map(p -> asMap(p.getExtensionProperties().get("stacktrace/development")))
        .findFirst()
        .orElseThrow(AssertionError::new);
  }

  @Test
  void testCustomize2() {
    OpenTelemetryConfigurationModel openTelemetryConfigurationModel =
        withInstrumentation(
            new OpenTelemetryConfigurationModel()
                .setLoggerProvider(
                    new LoggerProviderModel().setProcessors(Collections.emptyList())),
            new ExperimentalInstrumentationModel()
                .setJava(
                    new ExperimentalLanguageSpecificInstrumentationModel()
                        .setAdditionalProperty(
                            "solarwinds",
                            new ExperimentalLanguageSpecificInstrumentationPropertyModel()
                                .setAdditionalProperty(
                                    ConfigProperty.AGENT_SERVICE_KEY.getConfigFileKey(),
                                    "token:service")
                                .setAdditionalProperty(
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
    Map<String, Object> logExporterProperty =
        asMap(exporter.getExtensionProperties().get(LogExporterComponentProvider.COMPONENT_NAME));

    assertNotNull(logExporterProperty);
    Map<String, Object> logConfigs = logExporterProperty;
    assertEquals("https://otel.collector.com/v1/logs", logConfigs.get("endpoint"));
    assertEquals("authorization=Bearer token", logConfigs.get("headers_list"));
  }

  @Test
  void customizeShouldNotSetLogExporterWhenBatchIsSpecified() {
    OpenTelemetryConfigurationModel openTelemetryConfigurationModel =
        withInstrumentation(
            new OpenTelemetryConfigurationModel()
                .setLoggerProvider(
                    new LoggerProviderModel()
                        .setProcessors(
                            Collections.singletonList(
                                new LogRecordProcessorModel()
                                    .setBatch(
                                        new BatchLogRecordProcessorModel()
                                            .setExporter(new LogRecordExporterModel()))))),
            new ExperimentalInstrumentationModel()
                .setJava(
                    new ExperimentalLanguageSpecificInstrumentationModel()
                        .setAdditionalProperty(
                            "solarwinds",
                            new ExperimentalLanguageSpecificInstrumentationPropertyModel()
                                .setAdditionalProperty(
                                    ConfigProperty.AGENT_SERVICE_KEY.getConfigFileKey(),
                                    "token:service")
                                .setAdditionalProperty(
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
    Map<String, Object> logExporterProperty =
        asMap(exporter.getExtensionProperties().get(LogExporterComponentProvider.COMPONENT_NAME));

    assertNull(logExporterProperty);
  }

  @Test
  void customizeShouldNotSetLogExporterWhenSimpleIsSpecified() {
    OpenTelemetryConfigurationModel openTelemetryConfigurationModel =
        withInstrumentation(
            new OpenTelemetryConfigurationModel()
                .setLoggerProvider(
                    new LoggerProviderModel()
                        .setProcessors(
                            Collections.singletonList(
                                new LogRecordProcessorModel()
                                    .setSimple(
                                        new SimpleLogRecordProcessorModel()
                                            .setExporter(new LogRecordExporterModel()))))),
            new ExperimentalInstrumentationModel()
                .setJava(
                    new ExperimentalLanguageSpecificInstrumentationModel()
                        .setAdditionalProperty(
                            "solarwinds",
                            new ExperimentalLanguageSpecificInstrumentationPropertyModel()
                                .setAdditionalProperty(
                                    ConfigProperty.AGENT_SERVICE_KEY.getConfigFileKey(),
                                    "token:service")
                                .setAdditionalProperty(
                                    ConfigProperty.AGENT_COLLECTOR.getConfigFileKey(),
                                    "http://apm.collector.com"))));

    doNothing()
        .when(declarativeConfigurationCustomizerMock)
        .addModelCustomizer(functionArgumentCaptor.capture());

    tested.customize(declarativeConfigurationCustomizerMock);
    functionArgumentCaptor.getValue().apply(openTelemetryConfigurationModel);

    LoggerProviderModel loggerProvider = openTelemetryConfigurationModel.getLoggerProvider();
    LogRecordProcessorModel logRecordProcessorModel = loggerProvider.getProcessors().get(0);
    SimpleLogRecordProcessorModel simple = logRecordProcessorModel.getSimple();

    assertNotNull(simple);
    LogRecordExporterModel exporter = simple.getExporter();
    Map<String, Object> logExporterProperty =
        asMap(exporter.getExtensionProperties().get(LogExporterComponentProvider.COMPONENT_NAME));

    assertNull(logExporterProperty);
  }

  @Test
  void UrlShouldNotChangeWhenNotApm() {
    OpenTelemetryConfigurationModel openTelemetryConfigurationModel =
        withInstrumentation(
            new OpenTelemetryConfigurationModel()
                .setLoggerProvider(
                    new LoggerProviderModel().setProcessors(Collections.emptyList())),
            new ExperimentalInstrumentationModel()
                .setJava(
                    new ExperimentalLanguageSpecificInstrumentationModel()
                        .setAdditionalProperty(
                            "solarwinds",
                            new ExperimentalLanguageSpecificInstrumentationPropertyModel()
                                .setAdditionalProperty(
                                    ConfigProperty.AGENT_SERVICE_KEY.getConfigFileKey(),
                                    "token:service")
                                .setAdditionalProperty(
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
    Map<String, Object> logExporterProperty =
        asMap(exporter.getExtensionProperties().get(LogExporterComponentProvider.COMPONENT_NAME));

    assertNotNull(logExporterProperty);
    Map<String, Object> logConfigs = logExporterProperty;
    assertEquals("http://example.com/v1/logs", logConfigs.get("endpoint"));
    assertEquals("authorization=Bearer token", logConfigs.get("headers_list"));
  }

  @Test
  void UrlShouldNotChangeWhenNotApm2() {
    OpenTelemetryConfigurationModel openTelemetryConfigurationModel =
        withInstrumentation(
            new OpenTelemetryConfigurationModel()
                .setLoggerProvider(
                    new LoggerProviderModel().setProcessors(Collections.emptyList())),
            new ExperimentalInstrumentationModel()
                .setJava(
                    new ExperimentalLanguageSpecificInstrumentationModel()
                        .setAdditionalProperty(
                            "solarwinds",
                            new ExperimentalLanguageSpecificInstrumentationPropertyModel()
                                .setAdditionalProperty(
                                    ConfigProperty.AGENT_SERVICE_KEY.getConfigFileKey(),
                                    "token:service")
                                .setAdditionalProperty(
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
    Map<String, Object> logExporterProperty =
        asMap(exporter.getExtensionProperties().get(LogExporterComponentProvider.COMPONENT_NAME));

    assertNotNull(logExporterProperty);
    Map<String, Object> logConfigs = logExporterProperty;
    assertEquals("http://localhost:4317/v1/logs", logConfigs.get("endpoint"));
    assertEquals("authorization=Bearer token", logConfigs.get("headers_list"));
  }

  @Test
  void tracesNotConfiguredWhenTracerProviderAbsent() {
    OpenTelemetryConfigurationModel openTelemetryConfigurationModel =
        withInstrumentation(
            new OpenTelemetryConfigurationModel()
                .setLoggerProvider(
                    new LoggerProviderModel().setProcessors(Collections.emptyList())),
            new ExperimentalInstrumentationModel()
                .setJava(
                    new ExperimentalLanguageSpecificInstrumentationModel()
                        .setAdditionalProperty(
                            "solarwinds",
                            new ExperimentalLanguageSpecificInstrumentationPropertyModel()
                                .setAdditionalProperty(
                                    ConfigProperty.AGENT_SERVICE_KEY.getConfigFileKey(),
                                    "token:service")
                                .setAdditionalProperty(
                                    ConfigProperty.AGENT_COLLECTOR.getConfigFileKey(),
                                    "apm.collector.com"))));

    doNothing()
        .when(declarativeConfigurationCustomizerMock)
        .addModelCustomizer(functionArgumentCaptor.capture());

    tested.customize(declarativeConfigurationCustomizerMock);
    functionArgumentCaptor.getValue().apply(openTelemetryConfigurationModel);

    assertNull(openTelemetryConfigurationModel.getTracerProvider());
    assertNotNull(openTelemetryConfigurationModel.getLoggerProvider());
    assertNotNull(openTelemetryConfigurationModel.getMeterProvider());
    assertNotNull(openTelemetryConfigurationModel.getPropagator());
  }

  @Test
  void customizeShouldNotOverrideSamplerWhenOneIsAlreadyConfigured() {
    SamplerModel userSampler = new SamplerModel().setExtensionProperty("cel", null);

    OpenTelemetryConfigurationModel openTelemetryConfigurationModel =
        withInstrumentation(
            new OpenTelemetryConfigurationModel()
                .setTracerProvider(
                    new TracerProviderModel()
                        .setProcessors(Collections.emptyList())
                        .setSampler(userSampler)),
            new ExperimentalInstrumentationModel()
                .setJava(
                    new ExperimentalLanguageSpecificInstrumentationModel()
                        .setAdditionalProperty(
                            "solarwinds",
                            new ExperimentalLanguageSpecificInstrumentationPropertyModel()
                                .setAdditionalProperty(
                                    ConfigProperty.AGENT_SERVICE_KEY.getConfigFileKey(),
                                    "token:service")
                                .setAdditionalProperty(
                                    ConfigProperty.AGENT_COLLECTOR.getConfigFileKey(),
                                    "apm.collector.com"))));

    doNothing()
        .when(declarativeConfigurationCustomizerMock)
        .addModelCustomizer(functionArgumentCaptor.capture());

    tested.customize(declarativeConfigurationCustomizerMock);
    functionArgumentCaptor.getValue().apply(openTelemetryConfigurationModel);

    TracerProviderModel tracerProvider = openTelemetryConfigurationModel.getTracerProvider();
    SamplerModel sampler = tracerProvider.getSampler();

    assertNull(sampler.getExtensionProperties().get(SamplerComponentProvider.COMPONENT_NAME));
    assertTrue(sampler.getExtensionProperties().containsKey("cel"));
  }

  @Test
  void logsNotConfiguredWhenLoggerProviderAbsent() {
    OpenTelemetryConfigurationModel openTelemetryConfigurationModel =
        withInstrumentation(
            new OpenTelemetryConfigurationModel()
                .setTracerProvider(
                    new TracerProviderModel().setProcessors(Collections.emptyList())),
            new ExperimentalInstrumentationModel()
                .setJava(
                    new ExperimentalLanguageSpecificInstrumentationModel()
                        .setAdditionalProperty(
                            "solarwinds",
                            new ExperimentalLanguageSpecificInstrumentationPropertyModel()
                                .setAdditionalProperty(
                                    ConfigProperty.AGENT_SERVICE_KEY.getConfigFileKey(),
                                    "token:service")
                                .setAdditionalProperty(
                                    ConfigProperty.AGENT_COLLECTOR.getConfigFileKey(),
                                    "apm.collector.com"))));

    doNothing()
        .when(declarativeConfigurationCustomizerMock)
        .addModelCustomizer(functionArgumentCaptor.capture());

    tested.customize(declarativeConfigurationCustomizerMock);
    functionArgumentCaptor.getValue().apply(openTelemetryConfigurationModel);

    assertNull(openTelemetryConfigurationModel.getLoggerProvider());
    assertNotNull(openTelemetryConfigurationModel.getTracerProvider());
    assertNotNull(openTelemetryConfigurationModel.getTracerProvider().getSampler());
    assertNotNull(openTelemetryConfigurationModel.getMeterProvider());
  }

  @Test
  void propagatorsAppendedToExistingCompositeList() {
    OpenTelemetryConfigurationModel openTelemetryConfigurationModel =
        withInstrumentation(
            new OpenTelemetryConfigurationModel()
                .setPropagator(new PropagatorModel().setCompositeList("tracecontext,baggage")),
            new ExperimentalInstrumentationModel()
                .setJava(
                    new ExperimentalLanguageSpecificInstrumentationModel()
                        .setAdditionalProperty(
                            "solarwinds",
                            new ExperimentalLanguageSpecificInstrumentationPropertyModel()
                                .setAdditionalProperty(
                                    ConfigProperty.AGENT_SERVICE_KEY.getConfigFileKey(),
                                    "token:service")
                                .setAdditionalProperty(
                                    ConfigProperty.AGENT_COLLECTOR.getConfigFileKey(),
                                    "apm.collector.com"))));

    doNothing()
        .when(declarativeConfigurationCustomizerMock)
        .addModelCustomizer(functionArgumentCaptor.capture());

    tested.customize(declarativeConfigurationCustomizerMock);
    functionArgumentCaptor.getValue().apply(openTelemetryConfigurationModel);

    assertEquals(
        "tracecontext,baggage," + ContextPropagatorComponentProvider.COMPONENT_NAME,
        openTelemetryConfigurationModel.getPropagator().getCompositeList());
  }

  @Test
  void metricsExportDisabledWhenMeterProviderAbsent() {
    OpenTelemetryConfigurationModel openTelemetryConfigurationModel =
        withInstrumentation(
            new OpenTelemetryConfigurationModel(),
            new ExperimentalInstrumentationModel()
                .setJava(
                    new ExperimentalLanguageSpecificInstrumentationModel()
                        .setAdditionalProperty(
                            "solarwinds",
                            new ExperimentalLanguageSpecificInstrumentationPropertyModel()
                                .setAdditionalProperty(
                                    ConfigProperty.AGENT_SERVICE_KEY.getConfigFileKey(),
                                    "token:service")
                                .setAdditionalProperty(
                                    ConfigProperty.AGENT_COLLECTOR.getConfigFileKey(),
                                    "apm.collector.com"))));

    doNothing()
        .when(declarativeConfigurationCustomizerMock)
        .addModelCustomizer(functionArgumentCaptor.capture());

    tested.customize(declarativeConfigurationCustomizerMock);
    functionArgumentCaptor.getValue().apply(openTelemetryConfigurationModel);

    assertNotNull(openTelemetryConfigurationModel.getMeterProvider());
    assertFalse((Boolean) ConfigManager.getConfig(ConfigProperty.AGENT_EXPORT_METRICS_ENABLED));
  }

  @Test
  void readsSolarwindsConfigFromDistributionNode() {
    OpenTelemetryConfigurationModel openTelemetryConfigurationModel =
        new OpenTelemetryConfigurationModel()
            .setLoggerProvider(new LoggerProviderModel().setProcessors(Collections.emptyList()))
            .setDistribution(
                new DistributionModel()
                    .setExtensionProperty(
                        "solarwinds",
                        new DistributionProperties()
                            .setAdditionalProperty(
                                ConfigProperty.AGENT_SERVICE_KEY.getConfigFileKey(),
                                "token:service")
                            .setAdditionalProperty(
                                ConfigProperty.AGENT_COLLECTOR.getConfigFileKey(),
                                "apm.collector.com")));

    doNothing()
        .when(declarativeConfigurationCustomizerMock)
        .addModelCustomizer(functionArgumentCaptor.capture());

    tested.customize(declarativeConfigurationCustomizerMock);
    functionArgumentCaptor.getValue().apply(openTelemetryConfigurationModel);

    Map<String, Object> logConfigs = logExporterConfigs(openTelemetryConfigurationModel);
    assertEquals("https://otel.collector.com/v1/logs", logConfigs.get("endpoint"));
    assertEquals("authorization=Bearer token", logConfigs.get("headers_list"));
  }

  @Test
  void distributionNodeTakesPrecedenceOverInstrumentationNode() {
    OpenTelemetryConfigurationModel openTelemetryConfigurationModel =
        withInstrumentation(
            new OpenTelemetryConfigurationModel()
                .setLoggerProvider(new LoggerProviderModel().setProcessors(Collections.emptyList()))
                .setDistribution(
                    new DistributionModel()
                        .setExtensionProperty(
                            "solarwinds",
                            new DistributionProperties()
                                .setAdditionalProperty(
                                    ConfigProperty.AGENT_SERVICE_KEY.getConfigFileKey(),
                                    "token:service")
                                .setAdditionalProperty(
                                    ConfigProperty.AGENT_COLLECTOR.getConfigFileKey(),
                                    "apm.distribution.com"))),
            new ExperimentalInstrumentationModel()
                .setJava(
                    new ExperimentalLanguageSpecificInstrumentationModel()
                        .setAdditionalProperty(
                            "solarwinds",
                            new ExperimentalLanguageSpecificInstrumentationPropertyModel()
                                .setAdditionalProperty(
                                    ConfigProperty.AGENT_SERVICE_KEY.getConfigFileKey(),
                                    "token:service")
                                .setAdditionalProperty(
                                    ConfigProperty.AGENT_COLLECTOR.getConfigFileKey(),
                                    "apm.instrumentation.com"))));

    doNothing()
        .when(declarativeConfigurationCustomizerMock)
        .addModelCustomizer(functionArgumentCaptor.capture());

    tested.customize(declarativeConfigurationCustomizerMock);
    functionArgumentCaptor.getValue().apply(openTelemetryConfigurationModel);

    Map<String, Object> logConfigs = logExporterConfigs(openTelemetryConfigurationModel);
    assertEquals("https://otel.distribution.com/v1/logs", logConfigs.get("endpoint"));
  }

  @Test
  void emptyDistributionSolarwindsFallsBackToInstrumentationNode() {
    OpenTelemetryConfigurationModel openTelemetryConfigurationModel =
        withInstrumentation(
            new OpenTelemetryConfigurationModel()
                .setLoggerProvider(new LoggerProviderModel().setProcessors(Collections.emptyList()))
                .setDistribution(
                    new DistributionModel()
                        .setExtensionProperty("solarwinds", new DistributionProperties())),
            new ExperimentalInstrumentationModel()
                .setJava(
                    new ExperimentalLanguageSpecificInstrumentationModel()
                        .setAdditionalProperty(
                            "solarwinds",
                            new ExperimentalLanguageSpecificInstrumentationPropertyModel()
                                .setAdditionalProperty(
                                    ConfigProperty.AGENT_SERVICE_KEY.getConfigFileKey(),
                                    "token:service")
                                .setAdditionalProperty(
                                    ConfigProperty.AGENT_COLLECTOR.getConfigFileKey(),
                                    "apm.instrumentation.com"))));

    doNothing()
        .when(declarativeConfigurationCustomizerMock)
        .addModelCustomizer(functionArgumentCaptor.capture());

    tested.customize(declarativeConfigurationCustomizerMock);
    functionArgumentCaptor.getValue().apply(openTelemetryConfigurationModel);

    Map<String, Object> logConfigs = logExporterConfigs(openTelemetryConfigurationModel);
    assertEquals("https://otel.instrumentation.com/v1/logs", logConfigs.get("endpoint"));
  }

  @Test
  void partialDistributionConfigIsFilledFromInstrumentationNode() {
    // distribution supplies only the collector; the service key must still be picked up from the
    // instrumentation node so the merged config is complete.
    OpenTelemetryConfigurationModel openTelemetryConfigurationModel =
        withInstrumentation(
            new OpenTelemetryConfigurationModel()
                .setLoggerProvider(new LoggerProviderModel().setProcessors(Collections.emptyList()))
                .setDistribution(
                    new DistributionModel()
                        .setExtensionProperty(
                            "solarwinds",
                            new DistributionProperties()
                                .setAdditionalProperty(
                                    ConfigProperty.AGENT_COLLECTOR.getConfigFileKey(),
                                    "apm.distribution.com"))),
            new ExperimentalInstrumentationModel()
                .setJava(
                    new ExperimentalLanguageSpecificInstrumentationModel()
                        .setAdditionalProperty(
                            "solarwinds",
                            new ExperimentalLanguageSpecificInstrumentationPropertyModel()
                                .setAdditionalProperty(
                                    ConfigProperty.AGENT_SERVICE_KEY.getConfigFileKey(),
                                    "token:service")
                                .setAdditionalProperty(
                                    ConfigProperty.AGENT_COLLECTOR.getConfigFileKey(),
                                    "apm.instrumentation.com"))));

    doNothing()
        .when(declarativeConfigurationCustomizerMock)
        .addModelCustomizer(functionArgumentCaptor.capture());

    tested.customize(declarativeConfigurationCustomizerMock);
    functionArgumentCaptor.getValue().apply(openTelemetryConfigurationModel);

    Map<String, Object> logConfigs = logExporterConfigs(openTelemetryConfigurationModel);
    // collector comes from distribution (wins per key)...
    assertEquals("https://otel.distribution.com/v1/logs", logConfigs.get("endpoint"));
    // ...and the service key is filled in from the instrumentation node.
    assertEquals("authorization=Bearer token", logConfigs.get("headers_list"));
  }

  private static Map<String, Object> logExporterConfigs(
      OpenTelemetryConfigurationModel openTelemetryConfigurationModel) {
    LoggerProviderModel loggerProvider = openTelemetryConfigurationModel.getLoggerProvider();
    LogRecordProcessorModel logRecordProcessorModel = loggerProvider.getProcessors().get(0);
    BatchLogRecordProcessorModel batch = logRecordProcessorModel.getBatch();

    assertNotNull(batch);
    LogRecordExporterModel exporter = batch.getExporter();
    Map<String, Object> logExporterProperty =
        asMap(exporter.getExtensionProperties().get(LogExporterComponentProvider.COMPONENT_NAME));

    assertNotNull(logExporterProperty);
    return logExporterProperty;
  }

  private static OpenTelemetryConfigurationModel withInstrumentation(
      OpenTelemetryConfigurationModel model, ExperimentalInstrumentationModel instrumentation) {
    return OpenTelemetryConfigurationModelAccessor.setInstrumentation(model, instrumentation);
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Object> asMap(Object value) {
    return (Map<String, Object>) value;
  }

  @SuppressWarnings("serial")
  private static class DistributionProperties extends HashMap<String, Object> {
    DistributionProperties setAdditionalProperty(String key, Object value) {
      put(key, value);
      return this;
    }
  }
}
