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

import java.util.ArrayList;
import java.util.List;

public class JavaVersionComparator {
  public static int compare(String v1, String v2) {
    return new Version(v1).compare(new Version(v2));
  }

  private static class Version {
    List<Integer> versions = new ArrayList<>();

    Version(String v) {
      String[] arr = v.split("[_.]");
      for (String ver : arr) {
        try {
          versions.add(Integer.valueOf(ver));
        } catch (NumberFormatException e) {
          versions.add(0);
        }
      }
    }

    int compare(Version other) {
      int selfIdx = 0, otherIdx = 0;
      while (selfIdx < versions.size() && otherIdx < other.versions.size()) {
        int selfVersion = versions.get(selfIdx);
        int otherVersion = other.versions.get(otherIdx);

        if (selfVersion != otherVersion) {
          return selfVersion - otherVersion;
        }
        selfIdx++;
        otherIdx++;
      }
      if (selfIdx == versions.size() && otherIdx == other.versions.size()) return 0;
      return selfIdx == versions.size() ? -1 : 1;
    }
  }
}
