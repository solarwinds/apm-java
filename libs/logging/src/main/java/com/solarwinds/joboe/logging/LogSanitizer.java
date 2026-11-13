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

package com.solarwinds.joboe.logging;

/**
 * Neutralizes line breaks and other control characters in untrusted values before they are written
 * to the agent log, preventing forged log entries (CWE-117) and terminal escape sequence injection
 * (CWE-150). Tabs are left as is.
 */
public final class LogSanitizer {
  private LogSanitizer() {}

  public static String sanitize(String value) {
    if (value == null) {
      return null;
    }

    StringBuilder builder = null;
    for (int i = 0; i < value.length(); i++) {
      char c = value.charAt(i);
      String replacement;
      switch (c) {
        case '\r':
          replacement = "\\r";
          break;
        case '\n':
          replacement = "\\n";
          break;
        case '\u0085':
        case '\u2028':
        case '\u2029':
          replacement = "_";
          break;
        default:
          replacement =
              c != '\t' && Character.isISOControl(c) ? String.format("\\u%04x", (int) c) : null;
      }

      if (replacement != null) {
        if (builder == null) {
          builder = new StringBuilder(value.length());
          builder.append(value, 0, i);
        }

        builder.append(replacement);
      } else if (builder != null) {
        builder.append(c);
      }
    }

    return builder != null ? builder.toString() : value;
  }
}
