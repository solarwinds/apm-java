package com.solarwinds.opentelemetry.extensions.config.provider;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.doNothing;

import io.opentelemetry.sdk.extension.incubator.fileconfig.DeclarativeConfigurationCustomizer;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.OpenTelemetryConfigurationModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.SpanProcessorModel;
import io.opentelemetry.sdk.extension.incubator.fileconfig.internal.model.TracerProviderModel;
import java.util.List;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CustomConfigCustomizerProviderTest {

  @InjectMocks private CustomConfigCustomizerProvider tested;

  @Mock private DeclarativeConfigurationCustomizer declarativeConfigurationCustomizerMock;

  @Captor
  private ArgumentCaptor<Function<OpenTelemetryConfigurationModel, OpenTelemetryConfigurationModel>>
      functionArgumentCaptor;

  @Test
  void testCustomize() {
    OpenTelemetryConfigurationModel openTelemetryConfigurationModel =
        new OpenTelemetryConfigurationModel();

    doNothing()
        .when(declarativeConfigurationCustomizerMock)
        .addModelCustomizer(functionArgumentCaptor.capture());

    tested.customize(declarativeConfigurationCustomizerMock);
    functionArgumentCaptor.getValue().apply(openTelemetryConfigurationModel);

    TracerProviderModel tracerProvider = openTelemetryConfigurationModel.getTracerProvider();
    assertNotNull(tracerProvider);

    List<SpanProcessorModel> processors = tracerProvider.getProcessors();
    assertNotNull(
        processors
            .get(0)
            .getAdditionalProperties()
            .get(ProfilingSpanProcessorComponentProvider.COMPONENT_NAME));
  }
}
