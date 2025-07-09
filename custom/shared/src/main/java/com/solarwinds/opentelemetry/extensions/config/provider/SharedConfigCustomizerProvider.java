package com.solarwinds.opentelemetry.extensions.config.provider;

import com.google.auto.service.AutoService;
import com.solarwinds.joboe.config.ConfigProperty;
import com.solarwinds.joboe.config.ServiceKeyUtils;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.sdk.extension.incubator.fileconfig.DeclarativeConfiguration;
import io.opentelemetry.sdk.extension.incubator.fileconfig.DeclarativeConfigurationCustomizer;
import io.opentelemetry.sdk.extension.incubator.fileconfig.DeclarativeConfigurationCustomizerProvider;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.AttributeLimitsModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.BatchLogRecordProcessorModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.BatchSpanProcessorModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.LogRecordExporterModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.LogRecordProcessorModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.LoggerProviderModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.MeterProviderModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.MetricReaderModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.OpenTelemetryConfigurationModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.OtlpGrpcExporterModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.PeriodicMetricReaderModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.PropagatorModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.PushMetricExporterModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.SamplerModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.SpanExporterModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.SpanProcessorModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.TracerProviderModel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@AutoService(DeclarativeConfigurationCustomizerProvider.class)
public class SharedConfigCustomizerProvider implements DeclarativeConfigurationCustomizerProvider {

  private final String[] serviceKeyAndEndpoint = new String[2];

  @Override
  public void customize(DeclarativeConfigurationCustomizer customizer) {
    customizer.addModelCustomizer(
        configurationModel -> {
          setServiceKeyAndEndpoint(configurationModel);
          configurationModel.withAttributeLimits(
              new AttributeLimitsModel().withAttributeCountLimit(128));

          MeterProviderModel meterProvider = configurationModel.getMeterProvider();
          TracerProviderModel tracerProvider = configurationModel.getTracerProvider();
          LoggerProviderModel loggerProvider = configurationModel.getLoggerProvider();

          if (meterProvider == null) {
            meterProvider = new MeterProviderModel();
            configurationModel.withMeterProvider(meterProvider);
          }

          if (tracerProvider == null) {
            tracerProvider = new TracerProviderModel();
            configurationModel.withTracerProvider(tracerProvider);
          }

          if (loggerProvider == null) {
            loggerProvider = new LoggerProviderModel();
            configurationModel.withLoggerProvider(loggerProvider);
          }

          addSampler(tracerProvider);
          addProcessors(tracerProvider);
          addMetricExporter(configurationModel);

          addLogExporter(configurationModel);
          addSpanExporter(configurationModel);
          PropagatorModel propagatorModel = new PropagatorModel();

          addContextPropagators(propagatorModel);
          configurationModel.withPropagator(propagatorModel);
          return configurationModel;
        });
  }

  private void addProcessors(TracerProviderModel model) {
    List<SpanProcessorModel> processors =
        Arrays.asList(
            new SpanProcessorModel()
                .withAdditionalProperty(
                    InboundMeasurementMetricsComponentProvider.COMPONENT_NAME,
                    Collections.emptyMap()),
            new SpanProcessorModel()
                .withAdditionalProperty(
                    SpanStacktraceComponentProvider.COMPONENT_NAME, Collections.emptyMap()));

    ArrayList<SpanProcessorModel> allProcessors = new ArrayList<>(model.getProcessors());
    allProcessors.addAll(processors);
    model.withProcessors(allProcessors);
  }

  private void addSampler(TracerProviderModel model) {
    model.withSampler(
        new SamplerModel()
            .withAdditionalProperty(
                SamplerComponentProvider.COMPONENT_NAME, Collections.emptyMap()));
  }

  private void addContextPropagators(PropagatorModel model) {
    model.withCompositeList(
        String.format(
            "tracecontext,baggage,%s", ContextPropagatorComponentProvider.COMPONENT_NAME));
  }

  private void addSpanExporter(OpenTelemetryConfigurationModel model) {
    TracerProviderModel tracerProvider = Objects.requireNonNull(model.getTracerProvider());
    List<SpanProcessorModel> processors = tracerProvider.getProcessors();
    boolean hasExporter =
        processors.stream()
            .anyMatch(
                spanProcessorModel ->
                    spanProcessorModel.getBatch() != null
                        || spanProcessorModel.getSimple() != null);

    if (hasExporter) return;
    SpanProcessorModel spanProcessorModel =
        new SpanProcessorModel()
            .withBatch(
                new BatchSpanProcessorModel()
                    .withExportTimeout(60000)
                    .withMaxQueueSize(1024)
                    .withMaxExportBatchSize(512)
                    .withExporter(new SpanExporterModel().withOtlpGrpc(createModel())));

    ArrayList<SpanProcessorModel> spanProcessorModels = new ArrayList<>(processors);
    spanProcessorModels.add(spanProcessorModel);
    tracerProvider.withProcessors(spanProcessorModels);
  }

  private void addMetricExporter(OpenTelemetryConfigurationModel model) {
    MeterProviderModel meterProvider = model.getMeterProvider();
    List<MetricReaderModel> readers = Objects.requireNonNull(meterProvider).getReaders();
    if (!readers.isEmpty()) return;

    Map<String, Object> configs = new HashMap<>();
    configs.put("timeout", 10000);
    configs.put("protocol", "grpc");

    configs.put("compression", "gzip");
    configs.put("endpoint", serviceKeyAndEndpoint[1]);
    configs.put("temporality_preference", "cumulative");

    configs.put("default_histogram_aggregation", "explicit_bucket_histogram");
    configs.put("headers_list", String.format("authorization=Bearer %s", serviceKeyAndEndpoint[0]));
    model.withMeterProvider(
        new MeterProviderModel()
            .withReaders(
                Collections.singletonList(
                    new MetricReaderModel()
                        .withPeriodic(
                            new PeriodicMetricReaderModel()
                                .withTimeout(30000)
                                .withInterval(60000)
                                .withExporter(
                                    new PushMetricExporterModel()
                                        .withAdditionalProperty(
                                            MetricExporterComponentProvider.COMPONENT_NAME,
                                            configs))))));
  }

  private void addLogExporter(OpenTelemetryConfigurationModel model) {
    LoggerProviderModel loggerProvider = Objects.requireNonNull(model.getLoggerProvider());
    List<LogRecordProcessorModel> processors = loggerProvider.getProcessors();

    boolean hasExporter =
        processors.stream()
            .anyMatch(logRecordProcessorModel -> logRecordProcessorModel.getBatch() != null);

    if (hasExporter) return;
    LogRecordProcessorModel logRecordProcessorModel =
        new LogRecordProcessorModel()
            .withBatch(
                new BatchLogRecordProcessorModel()
                    .withScheduleDelay(1000)
                    .withMaxExportBatchSize(512)
                    .withMaxQueueSize(1024)
                    .withExportTimeout(30000)
                    .withExporter(new LogRecordExporterModel().withOtlpGrpc(createModel())));

    ArrayList<LogRecordProcessorModel> logRecordProcessorModels = new ArrayList<>(processors);
    logRecordProcessorModels.add(logRecordProcessorModel);
    loggerProvider.withProcessors(logRecordProcessorModels);
  }

  private void setServiceKeyAndEndpoint(OpenTelemetryConfigurationModel model) {
    DeclarativeConfigProperties configProperties =
        DeclarativeConfiguration.toConfigProperties(model);
    DeclarativeConfigProperties solarwinds =
        configProperties
            .getStructured("instrumentation/development", DeclarativeConfigProperties.empty())
            .getStructured("java", DeclarativeConfigProperties.empty())
            .getStructured("solarwinds");

    serviceKeyAndEndpoint[0] =
        Objects.requireNonNull(solarwinds, "Solarwinds configuration cannot be null.")
            .getString(ConfigProperty.AGENT_SERVICE_KEY.getConfigFileKey(), "");

    String endpoint = solarwinds.getString(ConfigProperty.AGENT_COLLECTOR.getConfigFileKey(), "");
    if (endpoint.startsWith("http")) {
      endpoint = endpoint.replaceAll("https?://apm", "https://otel");

    } else {
      endpoint = String.format("https://%s", endpoint.replace("apm", "otel"));
    }

    serviceKeyAndEndpoint[0] = ServiceKeyUtils.getApiKey(serviceKeyAndEndpoint[0]);
    serviceKeyAndEndpoint[1] = endpoint;
  }

  private OtlpGrpcExporterModel createModel() {
    return new OtlpGrpcExporterModel()
        .withCompression("gzip")
        .withEndpoint(serviceKeyAndEndpoint[1])
        .withTimeout(10000)
        .withHeadersList(String.format("authorization=Bearer %s", serviceKeyAndEndpoint[0]));
  }
}
