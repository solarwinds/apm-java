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

package com.solarwinds.joboe.config;

import com.solarwinds.joboe.logging.LogSanitizer;
import com.solarwinds.joboe.logging.LoggerFactory;

public class ServiceKeyUtils {
  public static final char SEPARATOR_CHARACTER =
      ':'; // char that separates customer key and service name within the service key
  public static final int SERVICE_NAME_MAX_LENGTH = 255;

  public static final String DEFAULT_SERVICE_NAME = "unknown_service:java";

  public static String maskServiceKey(String serviceKey) {
    if (serviceKey == null) {
      return serviceKey;
    }

    final int headCharacterCount = 4;
    final int tailCharacterCount = 4;
    final char maskCharacter = '*';

    int separatorIndex = serviceKey.indexOf(SEPARATOR_CHARACTER);
    String serviceName = getServiceName(serviceKey);
    String customerKey =
        separatorIndex != -1 ? serviceKey.substring(0, separatorIndex) : serviceKey;

    if (customerKey.length() > headCharacterCount + tailCharacterCount) {
      StringBuilder mask = new StringBuilder();
      for (int i = 0; i < customerKey.length() - (headCharacterCount + tailCharacterCount); i++) {
        mask.append(maskCharacter);
      }

      customerKey =
          customerKey.substring(0, headCharacterCount)
              + mask
              + customerKey.substring(customerKey.length() - tailCharacterCount);
    }

    return LogSanitizer.sanitize(
        serviceName != null ? customerKey + SEPARATOR_CHARACTER + serviceName : customerKey);
  }

  public static String getServiceName(String serviceKey) {
    int separatorIndex = serviceKey.indexOf(SEPARATOR_CHARACTER);
    return separatorIndex != -1 ? serviceKey.substring(separatorIndex + 1) : null;
  }

  public static String getApiKey(String serviceKey) {
    int separatorIndex = serviceKey.indexOf(SEPARATOR_CHARACTER);
    return separatorIndex != -1 ? serviceKey.substring(0, separatorIndex) : serviceKey;
  }

  /**
   * Builds the {@code <api token>:<service name>} key expected by the collector from the final
   * service name, normalized with {@link #transformServiceName}, falling back to {@link
   * #DEFAULT_SERVICE_NAME} when there's none.
   */
  public static String withServiceName(String serviceKey, String serviceName) {
    return getApiKey(serviceKey)
        + SEPARATOR_CHARACTER
        + transformServiceName(serviceNameOrDefault(serviceName));
  }

  public static String serviceNameOrDefault(String serviceName) {
    return isNotBlank(serviceName) ? serviceName : DEFAULT_SERVICE_NAME;
  }

  public static void warnIfServiceNameInServiceKey(String serviceKey) {
    if (serviceKey != null && isNotBlank(getServiceName(serviceKey))) {
      LoggerFactory.getLogger()
          .warn(
              "Setting the service name in the service key is deprecated and will be removed in a"
                  + " future release. Set the service name with OTEL_SERVICE_NAME instead and"
                  + " provide only the API token as the service key.");
    }
  }

  private static boolean isNotBlank(String value) {
    return value != null && !value.trim().isEmpty();
  }

  public static String transformServiceKey(String serviceKey) {
    String serviceName = getServiceName(serviceKey);

    if (serviceName != null) {
      return getApiKey(serviceKey) + SEPARATOR_CHARACTER + transformServiceName(serviceName);
    } else {
      return serviceKey;
    }
  }

  /**
   * Normalizes a service name: lower case, whitespace replaced with '-', characters other than word
   * characters, '.', ':', '_' and '-' removed, and truncated to {@link #SERVICE_NAME_MAX_LENGTH}.
   */
  public static String transformServiceName(String serviceName) {
    String transformed =
        serviceName.toLowerCase().replaceAll("\\s", "-").replaceAll("[^\\w.:_-]", "");
    if (transformed.length() > SERVICE_NAME_MAX_LENGTH) {
      transformed = transformed.substring(0, SERVICE_NAME_MAX_LENGTH);
    }
    return transformed;
  }
}
