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

import static com.solarwinds.opentelemetry.extensions.SharedNames.SPAN_STACKTRACE_FILTER_CLASS;

import com.google.auto.service.AutoService;
import com.solarwinds.joboe.config.ConfigManager;
import com.solarwinds.joboe.config.ConfigProperty;
import com.solarwinds.joboe.config.InvalidConfigException;
import com.solarwinds.joboe.config.ProxyConfig;
import com.solarwinds.joboe.config.ServiceKeyUtils;
import com.solarwinds.opentelemetry.extensions.config.SolarwindsConfigResolver;
import com.solarwinds.opentelemetry.extensions.config.parser.yaml.ProxyParser;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.DeclarativeConfiguration;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.DeclarativeConfigurationCustomizer;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.DeclarativeConfigurationCustomizerProvider;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.AttributeLimitsModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.BatchLogRecordProcessorModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.BatchSpanProcessorModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.LogRecordExporterModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.LogRecordProcessorModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.LoggerProviderModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.MeterProviderModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.MetricReaderModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.OpenTelemetryConfigurationModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.PeriodicMetricReaderModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.PropagatorModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.PushMetricExporterModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.SamplerModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.SpanExporterModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.SpanProcessorModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.TracerProviderModel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@AutoService(DeclarativeConfigurationCustomizerProvider.class)
public class SharedConfigCustomizerProvider implements DeclarativeConfigurationCustomizerProvider {

  private final String[] serviceKeyAndEndpoint = new String[2];

  private final ProxyParser parser = new ProxyParser();

  @Override
  public void customize(DeclarativeConfigurationCustomizer customizer) {
    customizer.addModelCustomizer(
        configurationModel -> {
          setServiceKeyAndEndpoint(configurationModel);
          parseProxyConfig(configurationModel);

          configurationModel.setAttributeLimits(
              new AttributeLimitsModel().setAttributeCountLimit(128));

          MeterProviderModel meterProvider = configurationModel.getMeterProvider();
          TracerProviderModel tracerProvider = configurationModel.getTracerProvider();
          LoggerProviderModel loggerProvider = configurationModel.getLoggerProvider();

          if (meterProvider == null) {
            meterProvider = new MeterProviderModel().setReaders(Collections.emptyList());
            configurationModel.setMeterProvider(meterProvider);

            try {
              ConfigManager.setConfig(ConfigProperty.AGENT_EXPORT_METRICS_ENABLED, false);
            } catch (InvalidConfigException ignored) {
            }
          }

          addMetricExporter(configurationModel);
          if (tracerProvider != null) {
            addSampler(tracerProvider);
            addProcessors(tracerProvider);

            addSpanExporter(configurationModel);
          }

          if (loggerProvider != null) {
            addLogExporter(configurationModel);
          }

          PropagatorModel propagatorModel = configurationModel.getPropagator();
          if (propagatorModel == null) {
            propagatorModel = new PropagatorModel();
            configurationModel.setPropagator(propagatorModel);
          }

          addContextPropagators(propagatorModel);
          return configurationModel;
        });
  }

  private static Map<String, Object> properties(Object... keyValues) {
    Map<String, Object> properties = new LinkedHashMap<>();
    for (int i = 0; i < keyValues.length; i += 2) {
      properties.put((String) keyValues[i], keyValues[i + 1]);
    }

    return properties;
  }

  private void addProcessors(TracerProviderModel model) {
    List<SpanProcessorModel> spanProcessorModels = model.getProcessors();
    if (spanProcessorModels == null) {
      spanProcessorModels = new ArrayList<>();
    }

    ArrayList<SpanProcessorModel> allProcessors = new ArrayList<>(spanProcessorModels);
    allProcessors.add(
        new SpanProcessorModel()
            .setExtensionProperty(
                InboundMeasurementMetricsComponentProvider.COMPONENT_NAME,
                new HashMap<String, Object>()));

    String experimentalStacktrace = "stacktrace/development";
    Optional<SpanProcessorModel> modelOptional =
        spanProcessorModels.stream()
            .filter(
                processorModel ->
                    processorModel.getExtensionProperties().containsKey(experimentalStacktrace))
            .findFirst();

    if (!modelOptional.isPresent()) {
      allProcessors.add(
          new SpanProcessorModel()
              .setExtensionProperty(
                  experimentalStacktrace, properties("filter", SPAN_STACKTRACE_FILTER_CLASS)));
    } else {
      SpanProcessorModel spanProcessorModel = modelOptional.get();
      Object existing = spanProcessorModel.getExtensionProperties().get(experimentalStacktrace);
      Map<String, Object> stacktraceProperties = new LinkedHashMap<>();

      if (existing instanceof Map) {
        ((Map<?, ?>) existing).forEach((k, v) -> stacktraceProperties.put(String.valueOf(k), v));
      }

      if (!stacktraceProperties.containsKey("filter")) {
        stacktraceProperties.put("filter", SPAN_STACKTRACE_FILTER_CLASS);
        spanProcessorModel.setExtensionProperty(experimentalStacktrace, stacktraceProperties);
      }
    }

    model.setProcessors(allProcessors);
  }

  private void addSampler(TracerProviderModel model) {
    SamplerModel sampler = model.getSampler();
    if (sampler == null) {
      model.setSampler(
          new SamplerModel()
              .setExtensionProperty(
                  SamplerComponentProvider.COMPONENT_NAME, new HashMap<String, Object>()));
    }
  }

  private void addContextPropagators(PropagatorModel model) {
    String compositeList = model.getCompositeList();
    if (compositeList != null) {
      model.setCompositeList(
          String.format("%s,%s", compositeList, ContextPropagatorComponentProvider.COMPONENT_NAME));
    } else {
      model.setCompositeList(
          String.format(
              "tracecontext,baggage,%s", ContextPropagatorComponentProvider.COMPONENT_NAME));
    }
  }

  private void addSpanExporter(OpenTelemetryConfigurationModel model) {
    TracerProviderModel tracerProvider = Objects.requireNonNull(model.getTracerProvider());
    List<SpanProcessorModel> processors = tracerProvider.getProcessors();

    if (processors == null) {
      processors = new ArrayList<>();
    }

    boolean hasExporter =
        processors.stream()
            .anyMatch(
                spanProcessorModel ->
                    spanProcessorModel.getBatch() != null
                        || spanProcessorModel.getSimple() != null);

    if (hasExporter) {
      return;
    }

    SpanProcessorModel spanProcessorModel =
        new SpanProcessorModel()
            .setBatch(
                new BatchSpanProcessorModel()
                    .setExportTimeout(60000)
                    .setMaxQueueSize(1024)
                    .setMaxExportBatchSize(512)
                    .setExporter(
                        new SpanExporterModel()
                            .setExtensionProperty(
                                SpanExporterComponentProvider.COMPONENT_NAME,
                                properties(
                                    "timeout",
                                    10000,
                                    "protocol",
                                    "http/protobuf",
                                    "compression",
                                    "gzip",
                                    "endpoint",
                                    serviceKeyAndEndpoint[1] + "/v1/traces",
                                    "headers_list",
                                    String.format(
                                        "authorization=Bearer %s", serviceKeyAndEndpoint[0])))));

    ArrayList<SpanProcessorModel> spanProcessorModels = new ArrayList<>(processors);
    spanProcessorModels.add(spanProcessorModel);
    tracerProvider.setProcessors(spanProcessorModels);
  }

  private void addMetricExporter(OpenTelemetryConfigurationModel model) {
    MeterProviderModel meterProvider = model.getMeterProvider();
    List<MetricReaderModel> readers = Objects.requireNonNull(meterProvider).getReaders();

    if (readers == null) {
      readers = new ArrayList<>();
    }

    if (!readers.isEmpty()) {
      return;
    }

    model.setMeterProvider(
        meterProvider.setReaders(
            Collections.singletonList(
                new MetricReaderModel()
                    .setPeriodic(
                        new PeriodicMetricReaderModel()
                            .setTimeout(30000)
                            .setInterval(60000)
                            .setExporter(
                                new PushMetricExporterModel()
                                    .setExtensionProperty(
                                        MetricExporterComponentProvider.COMPONENT_NAME,
                                        properties(
                                            "timeout",
                                            10000,
                                            "protocol",
                                            "http/protobuf",
                                            "compression",
                                            "gzip",
                                            "endpoint",
                                            serviceKeyAndEndpoint[1] + "/v1/metrics",
                                            "temporality_preference",
                                            "delta",
                                            "default_histogram_aggregation",
                                            "base2_exponential_bucket_histogram",
                                            "headers_list",
                                            String.format(
                                                "authorization=Bearer %s",
                                                serviceKeyAndEndpoint[0]))))))));
  }

  private void addLogExporter(OpenTelemetryConfigurationModel model) {
    LoggerProviderModel loggerProvider = Objects.requireNonNull(model.getLoggerProvider());
    List<LogRecordProcessorModel> processors = loggerProvider.getProcessors();

    if (processors == null) {
      processors = new ArrayList<>();
    }

    boolean hasExporter =
        processors.stream()
            .anyMatch(
                logRecordProcessorModel ->
                    logRecordProcessorModel.getBatch() != null
                        || logRecordProcessorModel.getSimple() != null);

    if (hasExporter) {
      return;
    }

    LogRecordProcessorModel logRecordProcessorModel =
        new LogRecordProcessorModel()
            .setBatch(
                new BatchLogRecordProcessorModel()
                    .setScheduleDelay(1000)
                    .setMaxExportBatchSize(512)
                    .setMaxQueueSize(1024)
                    .setExportTimeout(30000)
                    .setExporter(
                        new LogRecordExporterModel()
                            .setExtensionProperty(
                                LogExporterComponentProvider.COMPONENT_NAME,
                                properties(
                                    "timeout",
                                    10000,
                                    "protocol",
                                    "http/protobuf",
                                    "compression",
                                    "gzip",
                                    "endpoint",
                                    serviceKeyAndEndpoint[1] + "/v1/logs",
                                    "headers_list",
                                    String.format(
                                        "authorization=Bearer %s", serviceKeyAndEndpoint[0])))));

    ArrayList<LogRecordProcessorModel> logRecordProcessorModels = new ArrayList<>(processors);
    logRecordProcessorModels.add(logRecordProcessorModel);
    loggerProvider.setProcessors(logRecordProcessorModels);
  }

  private void setServiceKeyAndEndpoint(OpenTelemetryConfigurationModel model) {
    DeclarativeConfigProperties solarwinds = getSolarwindsConfig(model);

    serviceKeyAndEndpoint[0] =
        solarwinds.getString(ConfigProperty.AGENT_SERVICE_KEY.getConfigFileKey(), "");

    String endpoint = solarwinds.getString(ConfigProperty.AGENT_COLLECTOR.getConfigFileKey(), "");
    endpoint = endpoint.replaceAll("https?://apm|^apm", "https://otel");

    serviceKeyAndEndpoint[0] = ServiceKeyUtils.getApiKey(serviceKeyAndEndpoint[0]);
    serviceKeyAndEndpoint[1] = endpoint;
  }

  private void parseProxyConfig(OpenTelemetryConfigurationModel model) {
    DeclarativeConfigProperties solarwinds = getSolarwindsConfig(model);
    try {
      ProxyConfig proxyConfig = parser.convert(solarwinds);
      if (proxyConfig != null) {
        ConfigManager.setConfig(ConfigProperty.AGENT_PROXY, proxyConfig);
      }
    } catch (InvalidConfigException e) {
      throw new RuntimeException(e);
    }
  }

  private DeclarativeConfigProperties getSolarwindsConfig(OpenTelemetryConfigurationModel model) {
    DeclarativeConfigProperties configProperties =
        DeclarativeConfiguration.toConfigProperties(model);

    return Objects.requireNonNull(
        SolarwindsConfigResolver.resolve(configProperties),
        "Solarwinds configuration cannot be null.");
  }
}
