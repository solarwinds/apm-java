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

package com.solarwinds.opentelemetry.extensions.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.DeclarativeConfiguration;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.DistributionModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.DistributionPropertyModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.ExperimentalInstrumentationModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.ExperimentalLanguageSpecificInstrumentationModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.ExperimentalLanguageSpecificInstrumentationPropertyModel;
import io.opentelemetry.sdk.autoconfigure.declarativeconfig.model.OpenTelemetryConfigurationModel;
import org.junit.jupiter.api.Test;

class SolarwindsConfigResolverTest {

  @Test
  void resolveReturnsNullWhenNeitherNodePresent() {
    assertNull(resolve(new OpenTelemetryConfigurationModel()));
  }

  @Test
  void resolveReturnsDistributionWhenOnlyDistributionPresent() {
    DeclarativeConfigProperties resolved =
        resolve(
            new OpenTelemetryConfigurationModel()
                .withDistribution(distribution("service-key", "distribution.collector")));

    assertNotNull(resolved);
    assertEquals("service-key", resolved.getString("agent.serviceKey"));
    assertEquals("distribution.collector", resolved.getString("agent.collector"));
  }

  @Test
  void resolveReturnsInstrumentationWhenOnlyInstrumentationPresent() {
    DeclarativeConfigProperties resolved =
        resolve(
            new OpenTelemetryConfigurationModel()
                .withInstrumentationDevelopment(
                    instrumentation("service-key", "instrumentation.collector")));

    assertNotNull(resolved);
    assertEquals("service-key", resolved.getString("agent.serviceKey"));
    assertEquals("instrumentation.collector", resolved.getString("agent.collector"));
  }

  @Test
  void presentButEmptyDistributionNodeIsHonoredAndDefersEveryKeyToInstrumentation() {
    DeclarativeConfigProperties resolved =
        resolve(
            new OpenTelemetryConfigurationModel()
                .withDistribution(
                    new DistributionModel()
                        .withAdditionalProperty("solarwinds", new DistributionPropertyModel()))
                .withInstrumentationDevelopment(
                    instrumentation("service-key", "instrumentation.collector")));

    assertNotNull(resolved);
    assertEquals("service-key", resolved.getString("agent.serviceKey"));
    assertEquals("instrumentation.collector", resolved.getString("agent.collector"));
  }

  @Test
  void distributionWinsPerKeyAndInstrumentationFillsTheRest() {
    DeclarativeConfigProperties resolved =
        resolve(
            new OpenTelemetryConfigurationModel()
                .withDistribution(
                    new DistributionModel()
                        .withAdditionalProperty(
                            "solarwinds",
                            new DistributionPropertyModel()
                                .withAdditionalProperty("agent.collector", "distribution.collector")))
                .withInstrumentationDevelopment(
                    instrumentation("instrumentation-key", "instrumentation.collector")));

    assertNotNull(resolved);
    assertEquals("distribution.collector", resolved.getString("agent.collector"));
    assertEquals("instrumentation-key", resolved.getString("agent.serviceKey"));
  }

  @Test
  void getPropertyKeysIsTheUnionOfBothNodes() {
    DeclarativeConfigProperties resolved =
        resolve(
            new OpenTelemetryConfigurationModel()
                .withDistribution(
                    new DistributionModel()
                        .withAdditionalProperty(
                            "solarwinds",
                            new DistributionPropertyModel()
                                .withAdditionalProperty("agent.collector", "distribution.collector")))
                .withInstrumentationDevelopment(
                    new ExperimentalInstrumentationModel()
                        .withJava(
                            new ExperimentalLanguageSpecificInstrumentationModel()
                                .withAdditionalProperty(
                                    "solarwinds",
                                    new ExperimentalLanguageSpecificInstrumentationPropertyModel()
                                        .withAdditionalProperty(
                                            "agent.serviceKey", "instrumentation-key")))));

    assertNotNull(resolved);
    assertTrue(resolved.getPropertyKeys().contains("agent.collector"));
    assertTrue(resolved.getPropertyKeys().contains("agent.serviceKey"));
  }

  private static DeclarativeConfigProperties resolve(OpenTelemetryConfigurationModel model) {
    return SolarwindsConfigResolver.resolve(DeclarativeConfiguration.toConfigProperties(model));
  }

  private static DistributionModel distribution(String serviceKey, String collector) {
    return new DistributionModel()
        .withAdditionalProperty(
            "solarwinds",
            new DistributionPropertyModel()
                .withAdditionalProperty("agent.serviceKey", serviceKey)
                .withAdditionalProperty("agent.collector", collector));
  }

  private static ExperimentalInstrumentationModel instrumentation(
      String serviceKey, String collector) {
    return new ExperimentalInstrumentationModel()
        .withJava(
            new ExperimentalLanguageSpecificInstrumentationModel()
                .withAdditionalProperty(
                    "solarwinds",
                    new ExperimentalLanguageSpecificInstrumentationPropertyModel()
                        .withAdditionalProperty("agent.serviceKey", serviceKey)
                        .withAdditionalProperty("agent.collector", collector)));
  }
}
