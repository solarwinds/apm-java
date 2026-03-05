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

package com.solarwinds.joboe.sampling;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

public class SettingsArgTest {

  @Test
  public void testDoubleSettingsArg() {
    SettingsArg<Double> arg = new SettingsArg.DoubleSettingsArg("test-double");
    assertEquals("test-double", arg.getKey());
    ByteBuffer buffer = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
    buffer.putDouble(1.23);
    buffer.flip();
    assertEquals(1.23, arg.readValue(buffer));
  }

  @Test
  public void testIntegerSettingsArg() {
    SettingsArg<Integer> arg = new SettingsArg.IntegerSettingsArg("test-int");
    assertEquals("test-int", arg.getKey());
    ByteBuffer buffer = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
    buffer.putInt(3);
    buffer.flip();
    assertEquals(3, (int) arg.readValue(buffer));
  }

  @Test
  public void testBooleanSettingsArg() {
    SettingsArg<Boolean> arg = new SettingsArg.BooleanSettingsArg("test-boolean");
    assertEquals("test-boolean", arg.getKey());
    ByteBuffer buffer = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
    buffer.putInt(1); // boolean uses int in bytebuffer (binary)
    buffer.flip();
    assertEquals(true, arg.readValue(buffer));

    buffer = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
    buffer.putInt(0); // boolean uses int in bytebuffer (binary)
    buffer.flip();
    assertEquals(false, arg.readValue(buffer));
  }

  @Test
  public void testDoubleSettingsArgReadValueWithObject() {
    SettingsArg<Double> arg = new SettingsArg.DoubleSettingsArg("test-double");
    assertEquals(3.0, arg.readValue(3), 0);
    assertEquals(3.45, arg.readValue(3.45), 0);
    assertNull(arg.readValue("3.0"));
  }

  @Test
  public void testIntegerSettingsArgReadValueWithObject() {
    SettingsArg<Integer> arg = new SettingsArg.IntegerSettingsArg("test-int");
    assertEquals(3, arg.readValue(3));
    assertEquals(3, arg.readValue(3.0));
    assertNull(arg.readValue("3.0"));
  }

  @Test
  public void testBooleanSettingsArgReadValueWithObject() {
    SettingsArg<Boolean> arg = new SettingsArg.BooleanSettingsArg("test-boolean");
    assertTrue(arg.readValue(true));
    assertFalse(arg.readValue(false));
    assertNull(arg.readValue("3.0"));
  }

  @Test
  public void testByteSettingsArgReadValueWithObject() {
    SettingsArg<byte[]> arg = new SettingsArg.ByteArraySettingsArg("test-bytes");
    assertTrue(Arrays.equals(new byte[1], arg.readValue(new byte[1])));
    assertTrue(Arrays.equals("bytes".getBytes(), arg.readValue("bytes")));
    assertNull(arg.readValue(4));
  }

  @Test
  public void testInvalidArg() {
    SettingsArg<Double> arg = new SettingsArg.DoubleSettingsArg("test-invalid");
    assertEquals("test-invalid", arg.getKey());
    ByteBuffer buffer =
        ByteBuffer.allocate(4)
            .order(ByteOrder.LITTLE_ENDIAN); // 4 bytes will trigger buffer underflow
    buffer.putInt(3);
    buffer.flip();
    assertNull(arg.readValue(buffer)); // should just give null
  }
}
