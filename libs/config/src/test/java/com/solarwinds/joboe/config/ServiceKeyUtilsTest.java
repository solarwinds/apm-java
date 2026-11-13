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
}
