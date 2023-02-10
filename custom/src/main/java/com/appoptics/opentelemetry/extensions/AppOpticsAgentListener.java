package com.appoptics.opentelemetry.extensions;

import com.appoptics.opentelemetry.extensions.initialize.Initializer;
import com.google.auto.service.AutoService;
import io.opentelemetry.javaagent.extension.AgentListener;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;

/**
 * Executes startup task after it's safe to do so. See <a href="https://github.com/appoptics/opentelemetry-custom-distro/pull/7">...</a>
 */
@AutoService(AgentListener.class)
public class AppOpticsAgentListener implements AgentListener {
    @Override
    public void afterAgent(AutoConfiguredOpenTelemetrySdk autoConfiguredOpenTelemetrySdk) {
        if (AppOpticsTracerProviderCustomizer.getAgentEnabled()) {
            Initializer.executeStartupTasks(autoConfiguredOpenTelemetrySdk);
        }
    }
}
