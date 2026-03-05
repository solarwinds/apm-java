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

package com.solarwinds.joboe.core.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.solarwinds.joboe.config.JavaVersionComparator;
import org.junit.jupiter.api.Test;

public class JavaVersionComparatorTest {
  @Test
  public void testVersionCompare() {
    assertTrue(JavaVersionComparator.compare("1.8.0", "17.0.1") < 0);
    assertTrue(JavaVersionComparator.compare("1.8.0", "1.8.0_252") < 0);
    assertTrue(JavaVersionComparator.compare("1.8.0_252", "1.8.0_332") < 0);
    assertTrue(JavaVersionComparator.compare("1.8.0", "1.8.1") < 0);
    assertTrue(JavaVersionComparator.compare("1.8.0", "1.9.1") < 0);
    assertTrue(JavaVersionComparator.compare("1.8.2", "1.10.1") < 0);
    assertEquals(0, JavaVersionComparator.compare("1.8.0", "1.8.0"));
    assertTrue(JavaVersionComparator.compare("16.8.2", "17.0.1") < 0);
  }
}
