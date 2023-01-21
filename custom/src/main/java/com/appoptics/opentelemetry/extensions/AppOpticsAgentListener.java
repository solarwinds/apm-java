package com.appoptics.opentelemetry.extensions;

import com.appoptics.opentelemetry.extensions.initialize.Initializer;
import com.google.auto.service.AutoService;
import io.opentelemetry.instrumentation.api.config.Config;
import io.opentelemetry.javaagent.extension.AgentListener;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;

/**
 * Executes startup task after it's safe to do so. See https://github.com/appoptics/opentelemetry-custom-distro/pull/7
 */
@AutoService(AgentListener.class)
public class AppOpticsAgentListener implements AgentListener {
    @Override
    public void afterAgent(Config config, AutoConfiguredOpenTelemetrySdk autoConfiguredOpenTelemetrySdk) {
        if (AppOpticsTracerProviderConfigurer.getAgentEnabled()) {
            Initializer.executeStartupTasks(autoConfiguredOpenTelemetrySdk);
        }
    }
}
