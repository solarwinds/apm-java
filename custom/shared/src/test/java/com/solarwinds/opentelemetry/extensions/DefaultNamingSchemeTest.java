/*
 * Copyright SolarWinds Worldwide, LLC.
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.solarwinds.joboe.config.ConfigManager;
import com.solarwinds.joboe.config.ConfigProperty;
import com.solarwinds.joboe.config.InvalidConfigException;
import io.opentelemetry.api.common.Attributes;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DefaultNamingSchemeTest {

  @InjectMocks private DefaultNamingScheme tested;

  @Mock private NamingScheme namingSchemeMock;

  @Test
  void verifyNoDelegationToNext() {
    String name = tested.createName(Attributes.empty());

    verify(namingSchemeMock, times(0)).createName(any());
    assertNull(name);
  }

  @Test
  void verifyTransactionNameIsReturnedWhenSetInEnvironment() throws InvalidConfigException {
    ConfigManager.setConfig(ConfigProperty.AGENT_TRANSACTION_NAME, "test");
    String name = tested.createName(Attributes.empty());

    assertEquals("test", name);
  }
}
