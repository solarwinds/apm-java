package com.appoptics.opentelemetry.extensions;

import com.google.auto.service.AutoService;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSamplerProvider;
import io.opentelemetry.sdk.trace.samplers.Sampler;

@AutoService(ConfigurableSamplerProvider.class)
public class AppOpticsSamplerProvider implements ConfigurableSamplerProvider {
    @Override
    public Sampler createSampler(ConfigProperties config) {
        return new AppOpticsSampler();
    }

    @Override
    public String getName() {
        return "appoptics";
    }
}
