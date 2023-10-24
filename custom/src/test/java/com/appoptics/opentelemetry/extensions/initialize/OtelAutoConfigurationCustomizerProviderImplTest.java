package com.appoptics.opentelemetry.extensions.initialize;

import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atMostOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OtelAutoConfigurationCustomizerProviderImplTest {

    @InjectMocks
    private OtelAutoConfigurationCustomizerProviderImpl tested;

    @Mock
    private AutoConfigurationCustomizer autoConfigurationCustomizerMock;


    @Test
    void verifyThatWhenDisabledItIsNeverEnabled() {
        OtelAutoConfigurationCustomizerProviderImpl.setAgentEnabled(false);
        OtelAutoConfigurationCustomizerProviderImpl.setAgentEnabled(true);
        assertFalse(OtelAutoConfigurationCustomizerProviderImpl.isAgentEnabled());
    }

    @Test
    void verifyThatOrderReturnsIntMax() {
        assertEquals(Integer.MAX_VALUE, tested.order());
    }

    @Test
    void verifyThatAutoConfigurationIsCustomizedWithSWODefaultPropertiesAndTraceProviderCustomizer() {
        when(autoConfigurationCustomizerMock.addTracerProviderCustomizer(any())).thenReturn(autoConfigurationCustomizerMock);
        when(autoConfigurationCustomizerMock.addPropertiesSupplier(any())).thenReturn(autoConfigurationCustomizerMock);
        when(autoConfigurationCustomizerMock.addResourceCustomizer(any())).thenReturn(autoConfigurationCustomizerMock);

        tested.customize(autoConfigurationCustomizerMock);
        verify(autoConfigurationCustomizerMock, atMostOnce()).addPropertiesCustomizer(any());
        verify(autoConfigurationCustomizerMock, atMostOnce()).addTracerProviderCustomizer(any());
    }
}