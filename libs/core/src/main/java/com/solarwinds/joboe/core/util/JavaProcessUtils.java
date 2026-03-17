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

import java.lang.management.ManagementFactory;

/**
 * Helper to extract information on the running JVM process
 *
 * @author pluk
 */
public final class JavaProcessUtils {
  private static Integer pid = null;

  private JavaProcessUtils() {
    // prevent instantiations
  }

  /**
   * Copied from <code>Event.getPID()</code> Retrieves PID from current Java process
   *
   * @return
   */
  public static int getPid() {
    // You'd think getting the PID would be simple:
    // http://arhipov.blogspot.com/2011/01/java-snippet-get-pid-at-runtime.html
    if (pid == null) {
      String nameOfRunningVM = ManagementFactory.getRuntimeMXBean().getName();
      int p = nameOfRunningVM.indexOf('@');
      String pidStr = nameOfRunningVM.substring(0, p);
      try {
        pid = Integer.parseInt(pidStr);
      } catch (NumberFormatException ex) {
        // Shouldn't happen
        pid = -1;
      }
    }

    return pid;
  }
}
