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

package com.solarwinds.opentelemetry.extensions.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atMostOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.solarwinds.opentelemetry.extensions.config.provider.AutoConfigurationCustomizerProviderImpl;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AutoConfigurationCustomizerProviderImplTest {

  @InjectMocks private AutoConfigurationCustomizerProviderImpl tested;

  @Mock private AutoConfigurationCustomizer autoConfigurationCustomizerMock;

  @Test
  void verifyThatWhenDisabledItIsNeverEnabled() {
    AutoConfigurationCustomizerProviderImpl.setAgentEnabled(false);
    AutoConfigurationCustomizerProviderImpl.setAgentEnabled(true);
    assertFalse(AutoConfigurationCustomizerProviderImpl.isAgentEnabled());
  }

  @Test
  void verifyThatOrderReturnsIntMax() {
    assertEquals(Integer.MAX_VALUE, tested.order());
  }

  @Test
  void verifyThatAutoConfigurationIsCustomizedWithSWODefaultPropertiesAndTraceProviderCustomizer() {
    when(autoConfigurationCustomizerMock.addTracerProviderCustomizer(any()))
        .thenReturn(autoConfigurationCustomizerMock);
    when(autoConfigurationCustomizerMock.addPropertiesSupplier(any()))
        .thenReturn(autoConfigurationCustomizerMock);
    when(autoConfigurationCustomizerMock.addResourceCustomizer(any()))
        .thenReturn(autoConfigurationCustomizerMock);

    when(autoConfigurationCustomizerMock.addMetricExporterCustomizer(any()))
        .thenReturn(autoConfigurationCustomizerMock);

    tested.customize(autoConfigurationCustomizerMock);

    verify(autoConfigurationCustomizerMock, atMostOnce()).addPropertiesCustomizer(any());
    verify(autoConfigurationCustomizerMock, atMostOnce()).addTracerProviderCustomizer(any());

    verify(autoConfigurationCustomizerMock, atMostOnce()).addResourceCustomizer(any());
    verify(autoConfigurationCustomizerMock, atMostOnce()).addMetricExporterCustomizer(any());
  }
}
