package com.solarwinds.opentelemetry.extensions.config.provider;

import com.google.auto.service.AutoService;
import io.opentelemetry.sdk.extension.incubator.fileconfig.DeclarativeConfigurationCustomizer;
import io.opentelemetry.sdk.extension.incubator.fileconfig.DeclarativeConfigurationCustomizerProvider;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.SpanProcessorModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.TracerProviderModel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@AutoService(DeclarativeConfigurationCustomizerProvider.class)
public class CustomConfigCustomizerProvider implements DeclarativeConfigurationCustomizerProvider {

  @Override
  public void customize(DeclarativeConfigurationCustomizer customizer) {
    customizer.addModelCustomizer(
        configurationModel -> {
          TracerProviderModel tracerProvider = configurationModel.getTracerProvider();
          if (tracerProvider == null) {
            tracerProvider = new TracerProviderModel();
            configurationModel.withTracerProvider(tracerProvider);
          }

          addProcessors(tracerProvider);
          return configurationModel;
        });
  }

  private void addProcessors(TracerProviderModel model) {
    List<SpanProcessorModel> processors =
        Arrays.asList(
            new SpanProcessorModel()
                .withAdditionalProperty(
                    ProfilingSpanProcessorComponentProvider.COMPONENT_NAME, Collections.emptyMap()),
            new SpanProcessorModel()
                .withAdditionalProperty(
                    InboundMetricsSpanProcessorComponentProvider.COMPONENT_NAME,
                    Collections.emptyMap()));

    ArrayList<SpanProcessorModel> allProcessors = new ArrayList<>(model.getProcessors());
    allProcessors.addAll(processors);
    model.withProcessors(allProcessors);
  }
}
