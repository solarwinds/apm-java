package com.appoptics.opentelemetry.extensions.initialize;

import com.appoptics.opentelemetry.extensions.AppOpticsPropertiesSupplier;
import com.appoptics.opentelemetry.extensions.AppOpticsTracerProviderCustomizer;
import com.google.auto.service.AutoService;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizer;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizerProvider;

import javax.annotation.Nonnull;

@AutoService({AutoConfigurationCustomizerProvider.class})
public class OtelAutoConfigurationCustomizerProviderImpl implements AutoConfigurationCustomizerProvider {
    @Override
    public void customize(@Nonnull AutoConfigurationCustomizer autoConfiguration) {
        autoConfiguration.addPropertiesSupplier(new AppOpticsPropertiesSupplier())
                .addTracerProviderCustomizer(new AppOpticsTracerProviderCustomizer());
    }
}
