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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mockStatic;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class MetadataTest {

  @Captor private ArgumentCaptor<SettingsArgChangeListener<Integer>> listenerArgumentCaptor;

  @Test
  public void testHexEncode() throws Exception {
    // Make sure we can encode and decode hex strings
    Metadata md1 = new Metadata();
    md1.randomize();

    String hex1 = md1.toHexString();

    Metadata md2 = new Metadata();
    md2.fromHexString(hex1);

    assertEquals(md1, md2);
  }

  @Test
  public void testRandomization() {

    // Make sure IDs are unique:
    Metadata md1 = new Metadata();
    md1.randomize();

    Metadata md2 = new Metadata();
    md2.randomize();

    assertNotEquals(md1.toHexString(), md2.toHexString());

    String hex2 = md2.toHexString();

    md2.randomizeOpID();
    assertNotEquals(md2.toHexString(), hex2);

    Metadata md3 = new Metadata(md2);
    assertEquals(md2, md3);

    // Make sure flag is set properly
    Metadata md4 = new Metadata();
    md4.randomize(true);
    assertTrue(md4.isSampled());

    Metadata md5 = new Metadata();
    md5.randomize(false);
    assertFalse(md5.isSampled());
  }

  @Test
  public void testCompatibility() {
    // should not accept trace id from different version
    String v1Id = "01-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01";
    String v0Id = "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01";
    Metadata.setup(SamplingConfiguration.builder().build());

    assertFalse(Metadata.isCompatible(v1Id));
    assertTrue(Metadata.isCompatible(v0Id));
  }

  @Test
  public void testSampled() throws SamplingException {
    String sampledId = "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01";
    String notSampledId = "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-00";
    Metadata.setup(SamplingConfiguration.builder().build());

    assertTrue(new Metadata(sampledId).isSampled());
    assertFalse(new Metadata(notSampledId).isSampled());

    Metadata md = new Metadata();
    md.setSampled(true);
    assertTrue(md.isSampled());
    md.setSampled(false);
    assertFalse(md.isSampled());
  }

  @Test
  public void testInit() {

    // Test initialization
    Metadata md = new Metadata();
    assertFalse(md.isValid());

    md.randomizeOpID();
    assertFalse(md.isValid());

    md.randomizeTaskID();
    assertTrue(md.isValid());
  }

  @Test
  public void testTtlChange() {
    MockedStatic<SettingsManager> settingsManagerMock = mockStatic(SettingsManager.class);
    assertEquals(Metadata.DEFAULT_TTL, Metadata.getTtl());
    Metadata.setup(SamplingConfiguration.builder().build());
    settingsManagerMock.verify(
        () -> SettingsManager.registerListener(listenerArgumentCaptor.capture()), atLeastOnce());

    int newTtl = 10;
    listenerArgumentCaptor
        .getAllValues()
        .forEach(
            integerSettingsArgChangeListener -> integerSettingsArgChangeListener.onChange(newTtl));
    assertEquals(newTtl * 1000, Metadata.getTtl()); // sec to millisec

    // revert to default
    listenerArgumentCaptor
        .getAllValues()
        .forEach(
            integerSettingsArgChangeListener -> integerSettingsArgChangeListener.onChange(null));
    assertEquals(Metadata.DEFAULT_TTL, Metadata.getTtl());
    settingsManagerMock.close();
  }

  @Test
  public void testMaxEventsChange() {
    MockedStatic<SettingsManager> settingsManagerMock = mockStatic(SettingsManager.class);
    Metadata.setup(SamplingConfiguration.builder().build());
    assertEquals(Metadata.DEFAULT_MAX_EVENTS, Metadata.getMaxEvents());
    settingsManagerMock.verify(
        () -> SettingsManager.registerListener(listenerArgumentCaptor.capture()), atLeastOnce());

    int newMaxEvents = 100;
    listenerArgumentCaptor
        .getAllValues()
        .forEach(
            integerSettingsArgChangeListener ->
                integerSettingsArgChangeListener.onChange(newMaxEvents));
    assertEquals(newMaxEvents, Metadata.getMaxEvents());

    // revert to default
    listenerArgumentCaptor
        .getAllValues()
        .forEach(
            integerSettingsArgChangeListener -> integerSettingsArgChangeListener.onChange(null));

    assertEquals(Metadata.DEFAULT_MAX_EVENTS, Metadata.getMaxEvents());
    settingsManagerMock.close();
  }

  @Test
  public void testMaxBacktracesChange() {
    MockedStatic<SettingsManager> settingsManagerMock = mockStatic(SettingsManager.class);
    Metadata.setup(SamplingConfiguration.builder().build());
    assertEquals(Metadata.DEFAULT_MAX_BACKTRACES, Metadata.getMaxBacktraces());
    settingsManagerMock.verify(
        () -> SettingsManager.registerListener(listenerArgumentCaptor.capture()), atLeastOnce());

    int newMaxBacktraces = 100;
    listenerArgumentCaptor
        .getAllValues()
        .forEach(
            integerSettingsArgChangeListener ->
                integerSettingsArgChangeListener.onChange(newMaxBacktraces));
    assertEquals(newMaxBacktraces, Metadata.getMaxBacktraces());

    // revert to default
    listenerArgumentCaptor
        .getAllValues()
        .forEach(
            integerSettingsArgChangeListener -> integerSettingsArgChangeListener.onChange(null));
    assertEquals(Metadata.DEFAULT_MAX_BACKTRACES, Metadata.getMaxBacktraces());
    settingsManagerMock.close();
  }

  @Test
  public static String getXTraceid(int version, boolean sampled) {
    Metadata metadata = new Metadata();
    metadata.randomize(sampled);
    return metadata.toHexString(version);
  }
}
