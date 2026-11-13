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

package com.solarwinds.joboe.logging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class LogSanitizerTest {

  @Test
  void escapesCarriageReturnAndLineFeed() {
    assertEquals("id\\r\\nMay 01 INFO forged", LogSanitizer.sanitize("id\r\nMay 01 INFO forged"));
  }

  @Test
  void replacesUnicodeLineBreaks() {
    assertEquals("a_b_c_d", LogSanitizer.sanitize("a\u0085b\u2028c\u2029d"));
  }

  @Test
  void escapesOtherControlCharacters() {
    assertEquals(
        "a\\u000bb\\u001b[31mc\\u0000d\\u007f",
        LogSanitizer.sanitize("a\u000Bb\u001B[31mc\u0000d\u007F")); // VT, ESC, NUL, DEL
  }

  @Test
  void sanitizedValueHasNoLineBreaksOrControlCharacters() {
    String sanitized =
        LogSanitizer.sanitize(
            "x\ny\rz\u2028w\u2029v\u0085u\u000Bt\fs\u001Br"); // line breaks, VT, FF, ESC
    assertEquals(1, sanitized.split("\\R", -1).length);
    assertTrue(sanitized.chars().noneMatch(Character::isISOControl));
  }

  @Test
  void returnsSameInstanceWhenNothingToEscape() {
    String clean = "00-0af7651916cd43dd8448eb211c80319c-b7ad6b7169203331-01\tok";
    assertSame(clean, LogSanitizer.sanitize(clean));
  }

  @Test
  void returnsNullForNull() {
    assertNull(LogSanitizer.sanitize(null));
  }
}
