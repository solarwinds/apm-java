package com.solarwinds.opentelemetry.extensions;

import com.solarwinds.joboe.core.EventReporter;
import com.solarwinds.joboe.core.ReporterFactory;
import com.solarwinds.joboe.core.rpc.ClientException;
import com.solarwinds.joboe.core.rpc.RpcClientManager;
import com.solarwinds.joboe.core.util.HostTypeDetector;
import com.solarwinds.joboe.logging.Logger;
import com.solarwinds.joboe.logging.LoggerFactory;

public final class ReporterProvider {
  private ReporterProvider() {}

  private static EventReporter eventReporter;

  public static EventReporter getEventReporter() {
    if (eventReporter == null) {
      Logger logger = LoggerFactory.getLogger();
      try {
        logger.debug("Building reporter");
        eventReporter =
            ReporterFactory.getInstance()
                .createHostTypeReporter(
                    RpcClientManager.getClient(RpcClientManager.OperationType.TRACING),
                    HostTypeDetector.getHostType());
        logger.debug("Built reporter");
      } catch (ClientException clientException) {
        logger.error(
            String.format("Unable to create event reporter due error -> %s", clientException));
      }
    }
    return eventReporter;
  }
}
