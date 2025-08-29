package com.solarwinds.opentelemetry.extensions.config.provider;

import com.google.auto.service.AutoService;
import io.opentelemetry.sdk.extension.incubator.fileconfig.DeclarativeConfigurationCustomizer;
import io.opentelemetry.sdk.extension.incubator.fileconfig.DeclarativeConfigurationCustomizerProvider;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.ExperimentalResourceDetectionModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.ExperimentalResourceDetectorModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.ResourceModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.SpanProcessorModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.TracerProviderModel;
import java.util.ArrayList;
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

          ResourceModel resourceModel = configurationModel.getResource();
          if (resourceModel == null) {
            resourceModel = new ResourceModel();
            configurationModel.withResource(resourceModel);
          }

          addResourceDetector(resourceModel);
          addProcessors(tracerProvider);
          return configurationModel;
        });
  }

  private void addResourceDetector(ResourceModel resourceModel) {
    ExperimentalResourceDetectionModel detectionDevelopment =
        resourceModel.getDetectionDevelopment();
    if (detectionDevelopment == null) {
      detectionDevelopment = new ExperimentalResourceDetectionModel();
      resourceModel.withDetectionDevelopment(detectionDevelopment);
    }

    List<ExperimentalResourceDetectorModel> detectors = detectionDevelopment.getDetectors();
    if (detectors == null) {
      detectors = new ArrayList<>();
    }

    List<ExperimentalResourceDetectorModel> newDetectors = new ArrayList<>(detectors);
    newDetectors.add(
        new ExperimentalResourceDetectorModel()
            .withAdditionalProperty(
                ResourceComponentProvider.COMPONENT_NAME, Collections.emptyMap()));

    newDetectors.add(
        new ExperimentalResourceDetectorModel()
            .withAdditionalProperty(
                HostIdResourceComponentProvider.COMPONENT_NAME, Collections.emptyMap()));
    detectionDevelopment.withDetectors(newDetectors);
  }

  private void addProcessors(TracerProviderModel model) {
    List<SpanProcessorModel> processors =
        Collections.singletonList(
            new SpanProcessorModel()
                .withAdditionalProperty(
                    ProfilingSpanProcessorComponentProvider.COMPONENT_NAME,
                    Collections.emptyMap()));

    ArrayList<SpanProcessorModel> allProcessors = new ArrayList<>(model.getProcessors());
    allProcessors.addAll(processors);
    model.withProcessors(allProcessors);
  }
}
