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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class ServiceKeyUtilsTest {

  @Test
  void masksApiTokenAndKeepsServiceName() {
    assertEquals(
        "0123********cdef:my-service",
        ServiceKeyUtils.maskServiceKey("0123456789abcdef:my-service"));
  }

  @Test
  void masksKeyWithoutServiceName() {
    assertEquals("0123********cdef", ServiceKeyUtils.maskServiceKey("0123456789abcdef"));
  }

  @Test
  void sanitizesLineBreaksInMaskedKey() {
    assertEquals(
        "0123********cdef:svc\\r\\nINFO forged",
        ServiceKeyUtils.maskServiceKey("0123456789abcdef:svc\r\nINFO forged"));
  }

  @Test
  void returnsNullForNull() {
    assertNull(ServiceKeyUtils.maskServiceKey(null));
  }

  @Test
  void withServiceNameAppendsNameToTokenOnlyKey() {
    assertEquals("token:my-service", ServiceKeyUtils.withServiceName("token", "My Service"));
  }

  @Test
  void withServiceNameReplacesServiceKeyName() {
    assertEquals(
        "token:final-name", ServiceKeyUtils.withServiceName("token:key-name", "final-name"));
  }

  @Test
  void withServiceNameUsesDefaultWhenNameIsMissing() {
    assertEquals(
        "token:unknown_service:java", ServiceKeyUtils.withServiceName("token:key-name", null));
    assertEquals("token:unknown_service:java", ServiceKeyUtils.withServiceName("token", " "));
  }

  @Test
  void transformServiceNameNormalizesName() {
    assertEquals(
        "my-service.v1:prod_a", ServiceKeyUtils.transformServiceName("My Service.v1:prod_a!"));
  }

  @Test
  void transformServiceNameTruncatesLongName() {
    StringBuilder name = new StringBuilder();
    for (int i = 0; i < ServiceKeyUtils.SERVICE_NAME_MAX_LENGTH + 10; i++) {
      name.append('a');
    }

    assertEquals(
        ServiceKeyUtils.SERVICE_NAME_MAX_LENGTH,
        ServiceKeyUtils.transformServiceName(name.toString()).length());
  }

  @Test
  void transformServiceKeyNormalizesServiceName() {
    assertEquals("token:my-service", ServiceKeyUtils.transformServiceKey("token:My Service"));
  }

  @Test
  void transformServiceKeyKeepsTokenOnlyKey() {
    assertEquals("token", ServiceKeyUtils.transformServiceKey("token"));
  }

  @Test
  void withServiceNameUsesDefaultWhenNormalizedNameIsEmpty() {
    assertEquals("token:unknown_service:java", ServiceKeyUtils.withServiceName("token", "!!!"));
    assertEquals("token:unknown_service:java", ServiceKeyUtils.withServiceName("token", "中文"));
  }

  @Test
  void withServiceNameNormalizesLegacyServiceKeyName() {
    assertEquals(
        "token:legacy-name",
        ServiceKeyUtils.withServiceName(
            "token:Legacy Name", ServiceKeyUtils.getServiceName("token:Legacy Name")));
  }
}
