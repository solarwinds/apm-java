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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.autoconfigure.spi.internal.DefaultConfigProperties;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.semconv.incubating.ProcessIncubatingAttributes;
import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ResourceCustomizerTest {

  @InjectMocks private ResourceCustomizer tested;

  private final Resource resource =
      Resource.create(
          Attributes.builder()
              .put(
                  ProcessIncubatingAttributes.PROCESS_COMMAND_LINE,
                  "-Dsw.apm.service.key=token:chubi-token")
              .put(
                  ProcessIncubatingAttributes.PROCESS_COMMAND_ARGS,
                  Collections.singletonList("-Dsw.apm.service.key=token:chubi-token"))
              .build());

  @Test
  void verifyThatAutoConfiguredResourceIsCached() {
    tested.apply(resource, DefaultConfigProperties.createFromMap(Collections.emptyMap()));
    assertNotNull(ResourceCustomizer.getResource());
  }

  @Test
  void verifyThatServiceKeyIsMaskedWhenPresentInProcessCommandLine() {
    Resource actual =
        tested.apply(resource, DefaultConfigProperties.createFromMap(Collections.emptyMap()));
    assertEquals(
        "-Dsw.apm.service.key=****",
        actual.getAttribute(ProcessIncubatingAttributes.PROCESS_COMMAND_LINE));
  }

  @Test
  void verifyThatProcessCommandLineIsNotModifiedWhenServiceKeyIsNotPresentInProcessCommandLine() {
    Resource resource =
        Resource.create(
            Attributes.builder()
                .put(
                    ProcessIncubatingAttributes.PROCESS_COMMAND_LINE,
                    "-Duser.country=US -Duser.language=en")
                .build());
    Resource actual =
        tested.apply(resource, DefaultConfigProperties.createFromMap(Collections.emptyMap()));
    assertEquals(
        "-Duser.country=US -Duser.language=en",
        actual.getAttribute(ProcessIncubatingAttributes.PROCESS_COMMAND_LINE));
  }

  @Test
  void verifyThatServiceKeyIsMaskedWhenPresentInProcessCommandArgs() {
    Resource actual =
        tested.apply(resource, DefaultConfigProperties.createFromMap(Collections.emptyMap()));
    assertEquals(
        "-Dsw.apm.service.key=****",
        Objects.requireNonNull(
                actual.getAttribute(ProcessIncubatingAttributes.PROCESS_COMMAND_ARGS))
            .get(0));
  }

  @Test
  void verifyThatProcessCommandLineIsNotModifiedWhenServiceKeyIsNotPresentProcessCommandArgs() {
    Resource resource =
        Resource.create(
            Attributes.builder()
                .put(
                    ProcessIncubatingAttributes.PROCESS_COMMAND_ARGS,
                    Arrays.asList("-Duser.country=US", "-Duser.language=en"))
                .build());
    Resource actual =
        tested.apply(resource, DefaultConfigProperties.createFromMap(Collections.emptyMap()));
    assertEquals(
        Arrays.asList("-Duser.country=US", "-Duser.language=en"),
        actual.getAttribute(ProcessIncubatingAttributes.PROCESS_COMMAND_ARGS));
  }
}
