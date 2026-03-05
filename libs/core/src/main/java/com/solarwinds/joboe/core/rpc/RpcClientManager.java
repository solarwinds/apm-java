/*
 * © SolarWinds Worldwide, LLC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.solarwinds.joboe.core.rpc;

import com.solarwinds.joboe.config.ConfigManager;
import com.solarwinds.joboe.config.ConfigProperty;
import com.solarwinds.joboe.core.rpc.Client.ClientType;
import com.solarwinds.joboe.logging.Logger;
import com.solarwinds.joboe.logging.LoggerFactory;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Manages creation of PRC {@link Client}
 *
 * @author pluk
 */
public abstract class RpcClientManager {
  private static final Logger logger = LoggerFactory.getLogger();

  static final String DEFAULT_HOST =
      "apm.collector.na-01.cloud.solarwinds.com"; // default collector host: NH production

  static final int DEFAULT_PORT = 443; // default collector port

  protected static String collectorHost;

  protected static int collectorPort;

  protected static URL collectorCertLocation;

  static {
    init(
        (String) ConfigManager.getConfig(ConfigProperty.AGENT_COLLECTOR),
        (String) ConfigManager.getConfig(ConfigProperty.AGENT_COLLECTOR_SERVER_CERT_LOCATION));
  }

  static void init(String collector, String collectorCertValue) {
    if (collector != null) { // use system env if defined
      setHostAndPortByVariable(collector);
    } else {
      collectorHost = DEFAULT_HOST;
      collectorPort = DEFAULT_PORT;
    }

    if (collectorCertValue != null) {
      logger.info("Setting RPC Client to use server cert at [" + collectorCertValue + "]");
      try {
        File collectorCert = new File(collectorCertValue);
        if (collectorCert.exists()) {
          collectorCertLocation = collectorCert.toURI().toURL();
        } else {
          logger.warn(
              "Failed to load RPC collector server certificate from location ["
                  + collectorCertValue
                  + "], file does not exist!");
        }

      } catch (MalformedURLException e) {
        logger.warn(
            "Failed to load RPC collector server certificate from location ["
                + collectorCertValue
                + "], using default location instead!");
      }
    }
  }

  private static void setHostAndPortByVariable(String variable) {
    String[] tokens = variable.split(":");
    if (tokens.length == 1) {
      collectorHost = tokens[0];
      collectorPort = DEFAULT_PORT;
    } else {
      collectorHost = tokens[0];
      try {
        collectorPort = Integer.parseInt(tokens[1]);
      } catch (NumberFormatException e) {
        logger.warn(
            "Failed to parse the port number from "
                + ConfigProperty.AGENT_COLLECTOR.getEnvironmentVariableKey()
                + " : ["
                + variable
                + "]");
        collectorPort = DEFAULT_PORT;
      }
    }

    logger.info(
        "Using RPC collector host ["
            + collectorHost
            + "] port ["
            + collectorPort
            + "] for RPC client creation");
  }

  public static Client getClient(OperationType operationType) throws ClientException {
    return getClient(
        operationType, (String) ConfigManager.getConfig(ConfigProperty.AGENT_SERVICE_KEY));
  }

  public static Client getClient(OperationType operationType, String serviceKey)
      throws ClientException {
    return ClientManagerProvider.getClientManager(ClientType.GRPC)
        .orElseThrow(() -> new ClientException("Unknown Client type requested"))
        .getClientImpl(operationType, serviceKey);
  }

  protected abstract void close();

  protected abstract Client getClientImpl(OperationType operationType, String serviceKey);

  public enum OperationType {
    TRACING,
    PROFILING,
    SETTINGS,
    METRICS,
    STATUS
  }
}
