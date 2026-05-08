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

package com.solarwinds.opentelemetry.extensions;

import com.google.auto.service.AutoService;
import com.solarwinds.joboe.core.settings.TestSettingsReader;
import com.solarwinds.joboe.core.util.TestUtils;
import com.solarwinds.opentelemetry.core.AgentState;
import io.opentelemetry.javaagent.extension.AgentListener;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import java.util.concurrent.TimeUnit;

@AutoService(AgentListener.class)
public class TestAgentListener implements AgentListener {

  @Override
  public void afterAgent(AutoConfiguredOpenTelemetrySdk autoConfiguredOpenTelemetrySdk) {
    AgentState.waitForReady(30, TimeUnit.SECONDS);
    TestSettingsReader reader = TestUtils.initSettingsReader();
    reader.put(
        new TestSettingsReader.SettingsMockupBuilder()
            .withFlags(true, false, true, true, false)
            .withSampleRate(1_000_000)
            .build());
  }

  @Override
  public int order() {
    return Integer.MAX_VALUE;
  }
}
