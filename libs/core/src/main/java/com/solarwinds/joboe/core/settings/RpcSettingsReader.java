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

package com.solarwinds.joboe.core.settings;

import com.solarwinds.joboe.core.rpc.Client;
import com.solarwinds.joboe.core.rpc.ClientLoggingCallback;
import com.solarwinds.joboe.core.rpc.ResultCode;
import com.solarwinds.joboe.core.rpc.SettingsResult;
import com.solarwinds.joboe.logging.Logger;
import com.solarwinds.joboe.logging.LoggerFactory;
import com.solarwinds.joboe.sampling.Settings;

/**
 * Reads {@link Settings} from the source provided by the rpc {@link Client} specified during
 * instantiation
 */
public class RpcSettingsReader implements SettingsReader {
  private static final Logger logger = LoggerFactory.getLogger();

  private final Client rpcClient;
  private final ClientLoggingCallback<SettingsResult> loggingCallback =
      new ClientLoggingCallback<SettingsResult>("get service settings");

  private static final String SSL_CLIENT_VERSION = "2";

  /**
   * @param rpcClient client to use for retrieving settings
   */
  public RpcSettingsReader(Client rpcClient) {
    this.rpcClient = rpcClient;
  }

  /* (non-Javadoc)
   * @see com.solarwinds.joboe.core.SettingsReader#getLayerSampleRate(java.lang.String)
   */
  @Override
  public Settings getSettings() throws OboeSettingsException {
    SettingsResult result;
    try {
      result = rpcClient.getSettings(SSL_CLIENT_VERSION, loggingCallback).get();
      if (result.getResultCode() == ResultCode.OK) {
        logger.debug("Got settings from collector: " + result.getSettings().get(0));
        return result.getSettings().get(0);
      }
    } catch (Exception e) {
      throw new OboeSettingsException("Exception from RPC call: " + e.getMessage(), e);
    }

    throw new OboeSettingsException(
        "Rpc call returns non OK status code: " + result.getResultCode());
  }

  @Override
  public void close() {
    if (rpcClient != null) {
      rpcClient.close();
    }
  }
}
