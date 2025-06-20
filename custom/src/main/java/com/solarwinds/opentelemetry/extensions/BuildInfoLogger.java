package com.solarwinds.opentelemetry.extensions;

import com.google.auto.service.AutoService;
import com.solarwinds.joboe.logging.Logger;
import com.solarwinds.joboe.logging.LoggerFactory;
import io.opentelemetry.javaagent.extension.AgentListener;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;

@AutoService(AgentListener.class)
public class BuildInfoLogger implements AgentListener {
  @Override
  public void afterAgent(AutoConfiguredOpenTelemetrySdk autoConfiguredOpenTelemetrySdk) {
    Logger logger = LoggerFactory.getLogger();
    logger.info(String.format("Otel agent version: %s", BuildConfig.OTEL_AGENT_VERSION));
    logger.info(
        String.format("Solarwinds extension version: %s", BuildConfig.SOLARWINDS_AGENT_VERSION));

    logger.info(String.format("Solarwinds build datetime: %s", BuildConfig.BUILD_DATETIME));
    logger.info(String.format("Your Java version: %s", System.getProperty("java.version")));
  }
}
