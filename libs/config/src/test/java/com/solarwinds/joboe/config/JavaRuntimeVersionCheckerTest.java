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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetSystemProperty;

class JavaRuntimeVersionCheckerTest {

  @Test
  @SetSystemProperty(key = "java.version", value = "1.8.0_250")
  void returnFalse() {
    boolean jdkVersionSupported = JavaRuntimeVersionChecker.isJdkVersionSupported();
    assertFalse(jdkVersionSupported);
  }

  @Test
  void returnTrueWhenReferenceAndVersionAreTheSame() {
    boolean jdkVersionSupported =
        JavaRuntimeVersionChecker.isJdkVersionGreaterOrEqualToRef("11", "11");
    assertTrue(jdkVersionSupported);
  }

  @Test
  void returnTrueWhenVersionIsGreater() {
    boolean jdkVersionSupported =
        JavaRuntimeVersionChecker.isJdkVersionGreaterOrEqualToRef("11", "17");
    assertTrue(jdkVersionSupported);
  }
}
