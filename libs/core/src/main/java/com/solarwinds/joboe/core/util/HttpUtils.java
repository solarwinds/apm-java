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

public final class HttpUtils {
  private HttpUtils() {} // no instantiation on this class is allowed

  public static String trimQueryParameters(String uri) {
    if (uri == null) {
      return null;
    }

    int querySeparatorPosition = uri.indexOf('?');
    if (querySeparatorPosition == -1) {
      return uri;
    } else {
      return uri.substring(0, querySeparatorPosition);
    }
  }

  public static boolean isServerErrorStatusCode(int statusCode) {
    return statusCode / 100 == 5; // all 5xx
  }

  public static boolean isClientErrorStatusCode(int statusCode) {
    return statusCode / 100 == 4; // all 4xx
  }

  public static boolean isErrorStatusCode(int statusCode) {
    return isServerErrorStatusCode(statusCode) || isClientErrorStatusCode(statusCode);
  }
}
