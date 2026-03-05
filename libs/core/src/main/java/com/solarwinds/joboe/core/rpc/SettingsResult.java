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

import com.solarwinds.joboe.sampling.Settings;
import java.util.List;
import lombok.Getter;

@Getter
public class SettingsResult extends Result {
  private final List<Settings> settings;

  public SettingsResult(
      ResultCode resultCode, String arg, String warning, List<Settings> settings) {
    super(resultCode, arg, warning);
    this.settings = settings;
  }
}
