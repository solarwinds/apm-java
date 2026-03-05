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

package com.solarwinds.joboe.core.util;

import com.solarwinds.joboe.core.HostId;
import com.solarwinds.joboe.logging.Logger;
import com.solarwinds.joboe.logging.LoggerFactory;
import java.util.*;
import lombok.Getter;

/**
 * Helper to extract information on the host this JVM runs on
 *
 * @author pluk
 */
public class HostInfoUtils {
  private static final Logger logger = LoggerFactory.getLogger();
  private static HostInfoReader reader;

  static {
    for (HostInfoReaderProvider provider : ServiceLoader.load(HostInfoReaderProvider.class)) {
      logger.debug("Use HostInfoReaderProvider implementation " + provider.getClass().getName());
      HostInfoUtils.reader = provider.getHostInfoReader();
      break; // use the first implementation found in the path
    }
    if (HostInfoUtils.reader == null) {
      reader = new DummyHostInfoReader();
    }
  }

  public static String getAzureInstanceId() {
    if (reader instanceof AzureInstanceIdReader)
      return ((AzureInstanceIdReader) reader).getAzureInstanceId();
    return "unknown-azure-instance";
  }

  @Getter
  public enum OsType {
    LINUX("Linux"),
    WINDOWS("Windows"),
    MAC("Mac"),
    UNKNOWN("Unknown");
    private final String label;

    OsType(String label) {
      this.label = label;
    }
  }

  private HostInfoUtils() {
    // prevent instantiations
  }

  public static OsType getOsType() {
    String osName = System.getProperty("os.name");
    if (osName == null) {
      logger.warn("Failed to identify OS, system property `os.name` is null");
      return OsType.UNKNOWN;
    } else if (osName.toLowerCase().startsWith("windows")) {
      return OsType.WINDOWS;
    } else if (osName.toLowerCase().startsWith("linux")) {
      return OsType.LINUX;
    } else if (osName.toLowerCase().startsWith("mac")) {
      return OsType.MAC;
    } else {
      logger.info("Unsupported OS type detected: " + osName);
      return OsType.UNKNOWN;
    }
  }

  public static void init(HostInfoReader reader) {
    HostInfoUtils.reader = reader;
  }

  public static String getHostName() {
    if (reader instanceof HostNameReader) {
      return ((HostNameReader) reader).getHostName();
    }
    return "unknown-host";
  }

  public static HostId getHostId() {
    return reader.getHostId();
  }

  /**
   * Get a map of host information
   *
   * @return
   */
  public static Map<String, Object> getHostMetadata() {
    if (reader instanceof HostMetadataReader) {
      return ((HostMetadataReader) reader).getHostMetadata();
    }
    return new HashMap<>();
  }

  public static NetworkAddressInfo getNetworkAddressInfo() {
    if (reader instanceof NetworkAddressInfoReader) {
      return ((NetworkAddressInfoReader) reader).getNetworkAddressInfo();
    }
    return new NetworkAddressInfo(Collections.emptyList(), Collections.emptyList());
  }

  @Getter
  public static class NetworkAddressInfo {
    public NetworkAddressInfo(List<String> ipAddresses, List<String> macAddresses) {
      super();
      this.ipAddresses = ipAddresses;
      this.macAddresses = macAddresses;
    }

    private final List<String> ipAddresses;
    private final List<String> macAddresses;
  }
}
