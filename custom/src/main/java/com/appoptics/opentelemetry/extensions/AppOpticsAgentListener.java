package com.appoptics.opentelemetry.extensions;

import com.appoptics.opentelemetry.extensions.initialize.Initializer;
import com.google.auto.service.AutoService;
import io.opentelemetry.instrumentation.api.config.Config;
import io.opentelemetry.javaagent.extension.AgentListener;

@AutoService(AgentListener.class)
public class AppOpticsAgentListener implements AgentListener {
    @Override
    public void afterAgent(Config config) {
        Initializer.executeStartupTasks();
    }
}
