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

package com.solarwinds.opentelemetry.extensions.profile;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SampleEmitterTest {

  private SampleEmitter sampleEmitter;

  @Mock private ReadWriteSpan mockSpan;

  @Captor private ArgumentCaptor<String> eventNameCaptor;

  @Captor private ArgumentCaptor<Attributes> attributesCaptor;

  @BeforeEach
  void setUp() {
    sampleEmitter = new SampleEmitter(mockSpan);
  }

  @Test
  @DisplayName("beginEntryEmit should initialize entry attributes builder")
  void beginEntryEmitShouldInitializeBuilder() {
    sampleEmitter.beginEntryEmit();
    sampleEmitter.endEntryEmit();

    verify(mockSpan).addEvent(eventNameCaptor.capture(), attributesCaptor.capture());
    assertEquals("sw.profile", eventNameCaptor.getValue());
    assertNotNull(attributesCaptor.getValue());
  }

  @Test
  @DisplayName("beginExitEmit should initialize exit attributes builder")
  void beginExitEmitShouldInitializeBuilder() {
    sampleEmitter.beginExitEmit();
    sampleEmitter.endExitEmit();

    verify(mockSpan).addEvent(eventNameCaptor.capture(), attributesCaptor.capture());
    assertEquals("sw.profile", eventNameCaptor.getValue());
    assertNotNull(attributesCaptor.getValue());
  }

  @Test
  @DisplayName("beginSampleEmit should initialize sample attributes builder")
  void beginSampleEmitShouldInitializeBuilder() {
    sampleEmitter.beginSampleEmit();
    sampleEmitter.endSampleEmit();

    verify(mockSpan).addEvent(eventNameCaptor.capture(), attributesCaptor.capture());
    assertEquals("sw.profile", eventNameCaptor.getValue());
    assertNotNull(attributesCaptor.getValue());
  }

  @Test
  @DisplayName("addAttribute should add string attribute to entry builder")
  void addAttributeShouldAddStringToEntry() {
    sampleEmitter.beginEntryEmit();

    sampleEmitter.addAttribute("stringKey", "stringValue");
    sampleEmitter.endEntryEmit();

    verify(mockSpan).addEvent(eventNameCaptor.capture(), attributesCaptor.capture());
    Attributes attributes = attributesCaptor.getValue();

    assertEquals("sw.profile", eventNameCaptor.getValue());
    assertEquals("stringValue", attributes.get(AttributeKey.stringKey("stringKey")));
  }

  @Test
  @DisplayName("addAttribute should add long attribute to entry builder")
  void addAttributeShouldAddLongToEntry() {
    sampleEmitter.beginEntryEmit();

    sampleEmitter.addAttribute("longKey", 12345L);
    sampleEmitter.endEntryEmit();

    verify(mockSpan).addEvent(eventNameCaptor.capture(), attributesCaptor.capture());
    Attributes attributes = attributesCaptor.getValue();
    assertEquals(12345L, attributes.get(AttributeKey.longKey("longKey")));
  }

  @Test
  @DisplayName("addAttribute should add list of longs as long array to entry builder")
  void addAttributeShouldAddListOfLongsToEntry() {
    sampleEmitter.beginEntryEmit();
    List<Long> longList = Arrays.asList(1L, 2L, 3L, 4L, 5L);

    sampleEmitter.addAttribute("longListKey", longList);
    sampleEmitter.endEntryEmit();

    verify(mockSpan).addEvent(eventNameCaptor.capture(), attributesCaptor.capture());
    Attributes attributes = attributesCaptor.getValue();

    List<Long> result = attributes.get(AttributeKey.longArrayKey("longListKey"));
    assertNotNull(result);
    assertEquals(5, result.size());
    assertArrayEquals(new Long[] {1L, 2L, 3L, 4L, 5L}, result.toArray(new Long[0]));
  }

  @Test
  @DisplayName("addAttribute should add list of maps as JSON string array to entry builder")
  void addAttributeShouldAddListOfMapsToEntry() {
    sampleEmitter.beginEntryEmit();
    Map<String, Object> map1 = new HashMap<>();
    map1.put("key1", "value1");

    Map<String, Object> map2 = new HashMap<>();
    map2.put("key2", "value2");
    List<Map<String, Object>> mapList = Arrays.asList(map1, map2);

    sampleEmitter.addAttribute("mapListKey", mapList);
    sampleEmitter.endEntryEmit();

    verify(mockSpan).addEvent(eventNameCaptor.capture(), attributesCaptor.capture());
    Attributes attributes = attributesCaptor.getValue();

    List<String> result = attributes.get(AttributeKey.stringArrayKey("mapListKey"));
    assertNotNull(result);
    assertEquals(2, result.size());
    assertTrue(result.get(0).contains("key1"));
    assertTrue(result.get(1).contains("key2"));
  }

  @Test
  @DisplayName("addAttribute should add string attribute to sample builder")
  void addAttributeShouldAddStringToSample() {
    sampleEmitter.beginSampleEmit();

    sampleEmitter.addAttribute("sampleKey", "sampleValue");
    sampleEmitter.endSampleEmit();

    verify(mockSpan).addEvent(eventNameCaptor.capture(), attributesCaptor.capture());
    Attributes attributes = attributesCaptor.getValue();
    assertEquals("sampleValue", attributes.get(AttributeKey.stringKey("sampleKey")));
  }

  @Test
  @DisplayName("addAttribute should add string attribute to exit builder")
  void addAttributeShouldAddStringToExit() {
    sampleEmitter.beginExitEmit();

    sampleEmitter.addAttribute("exitKey", "exitValue");
    sampleEmitter.endExitEmit();

    verify(mockSpan).addEvent(eventNameCaptor.capture(), attributesCaptor.capture());
    Attributes attributes = attributesCaptor.getValue();
    assertEquals("exitValue", attributes.get(AttributeKey.stringKey("exitKey")));
  }

  @Test
  @DisplayName("addAttribute should handle multiple attributes")
  void addAttributeShouldHandleMultipleAttributes() {
    sampleEmitter.beginEntryEmit();

    sampleEmitter.addAttribute("string", "value");
    sampleEmitter.addAttribute("long", 999L);
    sampleEmitter.addAttribute("list", Arrays.asList(1L, 2L));
    sampleEmitter.endEntryEmit();

    verify(mockSpan).addEvent(eventNameCaptor.capture(), attributesCaptor.capture());
    Attributes attributes = attributesCaptor.getValue();

    assertEquals("value", attributes.get(AttributeKey.stringKey("string")));
    assertEquals(999L, attributes.get(AttributeKey.longKey("long")));
    assertNotNull(attributes.get(AttributeKey.longArrayKey("list")));
  }

  @Test
  @DisplayName("addAttribute should not add attribute when no builder is initialized")
  void addAttributeShouldNotAddWhenNoBuilderInitialized() {
    sampleEmitter.addAttribute("key", "value");
    verifyNoInteractions(mockSpan);
  }

  @Test
  @DisplayName("endEntryEmit should return true and add event")
  void endEntryEmitShouldReturnTrueAndAddEvent() {
    sampleEmitter.beginEntryEmit();
    boolean result = sampleEmitter.endEntryEmit();

    assertTrue(result);
    verify(mockSpan).addEvent(eq("sw.profile"), any(Attributes.class));
  }

  @Test
  @DisplayName("endExitEmit should return true and add event")
  void endExitEmitShouldReturnTrueAndAddEvent() {
    sampleEmitter.beginExitEmit();

    boolean result = sampleEmitter.endExitEmit();
    assertTrue(result);
    verify(mockSpan).addEvent(eq("sw.profile"), any(Attributes.class));
  }

  @Test
  @DisplayName("endSampleEmit should return true and add event")
  void endSampleEmitShouldReturnTrueAndAddEvent() {
    sampleEmitter.beginSampleEmit();

    boolean result = sampleEmitter.endSampleEmit();
    assertTrue(result);
    verify(mockSpan).addEvent(eq("sw.profile"), any(Attributes.class));
  }

  @Test
  @DisplayName("multiple emit cycles should work independently")
  void multipleEmitCyclesShouldWorkIndependently() {
    sampleEmitter.beginEntryEmit();
    sampleEmitter.addAttribute("entry", "value1");
    sampleEmitter.endEntryEmit();

    sampleEmitter.beginSampleEmit();
    sampleEmitter.addAttribute("sample", "value2");
    sampleEmitter.endSampleEmit();

    verify(mockSpan, times(2)).addEvent(eq("sw.profile"), any(Attributes.class));
  }

  @Test
  @DisplayName("addAttribute should handle empty list")
  void addAttributeShouldHandleEmptyList() {
    sampleEmitter.beginEntryEmit();
    List<Long> emptyList = Collections.emptyList();

    assertDoesNotThrow(
        () -> {
          sampleEmitter.addAttribute("emptyList", emptyList);
          sampleEmitter.endEntryEmit();
        });
  }

  @Test
  @DisplayName("addAttribute with Integer should not add attribute")
  void addAttributeWithIntegerShouldNotAdd() {
    sampleEmitter.beginEntryEmit();

    sampleEmitter.addAttribute("intKey", 123);
    sampleEmitter.endEntryEmit();

    verify(mockSpan).addEvent(eventNameCaptor.capture(), attributesCaptor.capture());
    Attributes attributes = attributesCaptor.getValue();

    assertNull(attributes.get(AttributeKey.longKey("intKey")));
    assertNull(attributes.get(AttributeKey.stringKey("intKey")));
  }

  @Test
  @DisplayName("concurrent builders should work independently")
  void concurrentBuildersShouldWorkIndependently() {
    sampleEmitter.beginEntryEmit();
    sampleEmitter.beginSampleEmit();
    sampleEmitter.beginExitEmit();

    sampleEmitter.addAttribute("key", "allBuilders");
    sampleEmitter.endEntryEmit();
    sampleEmitter.endSampleEmit();

    sampleEmitter.endExitEmit();
    verify(mockSpan, times(3)).addEvent(eq("sw.profile"), any(Attributes.class));
  }

  @Test
  @DisplayName("endExitEmit should nullify span reference")
  void endExitEmitShouldNullifySpan() {
    sampleEmitter.beginExitEmit();
    sampleEmitter.endExitEmit();

    sampleEmitter.beginEntryEmit();
    assertDoesNotThrow(() -> sampleEmitter.addAttribute("test", "value"));
  }

  @Test
  @DisplayName("addAttribute should convert list of maps to JSON strings")
  void addAttributeShouldConvertMapsToJson() {
    sampleEmitter.beginEntryEmit();
    Map<String, Object> map1 = new HashMap<String, Object>();
    map1.put("name", "John");
    map1.put("age", 30);
    Map<String, Object> map2 = new HashMap<String, Object>();
    map2.put("name", "Jane");
    map2.put("age", 25);
    List<Map<String, Object>> mapList = Arrays.asList(map1, map2);

    sampleEmitter.addAttribute("users", mapList);
    sampleEmitter.endEntryEmit();

    verify(mockSpan).addEvent(eventNameCaptor.capture(), attributesCaptor.capture());
    Attributes attributes = attributesCaptor.getValue();
    List<String> jsonStrings = attributes.get(AttributeKey.stringArrayKey("users"));

    assertNotNull(jsonStrings);
    assertEquals(2, jsonStrings.size());
    assertTrue(jsonStrings.get(0).contains("John"));
    assertTrue(jsonStrings.get(0).contains("30"));
    assertTrue(jsonStrings.get(1).contains("Jane"));
    assertTrue(jsonStrings.get(1).contains("25"));
  }

  @Test
  @DisplayName("endEntryEmit should add event with empty attributes when no attributes added")
  void endEntryEmitShouldAddEventWithEmptyAttributes() {
    sampleEmitter.beginEntryEmit();
    sampleEmitter.endEntryEmit();

    verify(mockSpan).addEvent(eventNameCaptor.capture(), attributesCaptor.capture());
    Attributes attributes = attributesCaptor.getValue();

    assertEquals("sw.profile", eventNameCaptor.getValue());
    assertEquals(0, attributes.size());
  }

  @Test
  @DisplayName("endExitEmit should add event with empty attributes when no attributes added")
  void endExitEmitShouldAddEventWithEmptyAttributes() {
    sampleEmitter.beginExitEmit();
    sampleEmitter.endExitEmit();

    verify(mockSpan).addEvent(eventNameCaptor.capture(), attributesCaptor.capture());
    Attributes attributes = attributesCaptor.getValue();

    assertEquals("sw.profile", eventNameCaptor.getValue());
    assertEquals(0, attributes.size());
  }

  @Test
  @DisplayName("endSampleEmit should add event with empty attributes when no attributes added")
  void endSampleEmitShouldAddEventWithEmptyAttributes() {
    sampleEmitter.beginSampleEmit();
    sampleEmitter.endSampleEmit();

    verify(mockSpan).addEvent(eventNameCaptor.capture(), attributesCaptor.capture());
    Attributes attributes = attributesCaptor.getValue();

    assertEquals("sw.profile", eventNameCaptor.getValue());
    assertEquals(0, attributes.size());
  }
}
