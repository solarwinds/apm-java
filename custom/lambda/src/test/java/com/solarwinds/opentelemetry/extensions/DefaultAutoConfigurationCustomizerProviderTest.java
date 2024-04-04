package com.solarwinds.opentelemetry.extensions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DefaultAutoConfigurationCustomizerProviderTest {
  @InjectMocks private DefaultAutoConfigurationCustomizerProvider tested;

  @Mock private AutoConfigurationCustomizer autoConfigurationCustomizerMock;

  @Test
  void ensureExpectedCustomization() {
    when(autoConfigurationCustomizerMock.addPropertiesSupplier(any()))
        .thenReturn(autoConfigurationCustomizerMock);
    when(autoConfigurationCustomizerMock.addTracerProviderCustomizer(any()))
        .thenReturn(autoConfigurationCustomizerMock);

    when(autoConfigurationCustomizerMock.addResourceCustomizer(any()))
        .thenReturn(autoConfigurationCustomizerMock);
    when(autoConfigurationCustomizerMock.addMetricExporterCustomizer(any()))
        .thenReturn(autoConfigurationCustomizerMock);

    tested.customize(autoConfigurationCustomizerMock);

    verify(autoConfigurationCustomizerMock).addPropertiesSupplier(any());
    verify(autoConfigurationCustomizerMock).addTracerProviderCustomizer(any());
    verify(autoConfigurationCustomizerMock).addResourceCustomizer(any());

    verify(autoConfigurationCustomizerMock).addMetricExporterCustomizer(any());
  }

  @Test
  void returnIntMax() {
    assertEquals(Integer.MAX_VALUE, tested.order());
  }
}
