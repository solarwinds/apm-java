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

package com.solarwinds.joboe.core.settings;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import com.google.protobuf.InvalidProtocolBufferException;
import com.solarwinds.joboe.core.rpc.ResultCode;
import com.solarwinds.joboe.core.rpc.RpcSettings;
import com.solarwinds.joboe.core.rpc.SettingsResult;
import com.solarwinds.joboe.sampling.Settings;
import com.solarwinds.trace.ingestion.proto.Collector;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SettingsUtilTest {

  private static byte[] settingsBlob;

  @Mock private RpcSettings settingsMock;

  @BeforeAll
  static void setup() throws IOException {
    settingsBlob =
        Files.readAllBytes(
            Paths.get(new File("src/test/resources/solarwinds-apm-settings-raw").getPath()));
  }

  @Test
  void testTransformToKVSetting() {
    when(settingsMock.getTtl()).thenReturn(60L);
    SettingsResult settingsResult =
        new SettingsResult(ResultCode.OK, "arg", "we up", Collections.singletonList(settingsMock));

    boolean anyMatch =
        settingsResult.getSettings().stream().anyMatch(settings -> settings.getTtl() == 60);
    assertTrue(anyMatch);
  }

  @Test
  void testTransformToLocalSettings() throws InvalidProtocolBufferException {
    SettingsResult settingsResult =
        SettingsUtil.transformToLocalSettings(Collector.SettingsResult.parseFrom(settingsBlob));
    assertEquals(ResultCode.OK, settingsResult.getResultCode());
  }

  @Test
  void testConvertSetting() throws InvalidProtocolBufferException {
    Collector.SettingsResult settingsResult = Collector.SettingsResult.parseFrom(settingsBlob);
    Settings settings = SettingsUtil.convertSetting(settingsResult.getSettings(0));
    assertEquals(120, settings.getTtl());
  }

  @Test
  void testConvertType() {
    assertEquals(-1, SettingsUtil.convertType(Collector.OboeSettingType.UNRECOGNIZED));
    assertEquals(
        Settings.OBOE_SETTINGS_TYPE_DEFAULT_SAMPLE_RATE,
        SettingsUtil.convertType(Collector.OboeSettingType.DEFAULT_SAMPLE_RATE));
    assertEquals(
        Settings.OBOE_SETTINGS_TYPE_LAYER_SAMPLE_RATE,
        SettingsUtil.convertType(Collector.OboeSettingType.LAYER_SAMPLE_RATE));
    assertEquals(
        Settings.OBOE_SETTINGS_TYPE_LAYER_APP_SAMPLE_RATE,
        SettingsUtil.convertType(Collector.OboeSettingType.LAYER_APP_SAMPLE_RATE));
    assertEquals(
        Settings.OBOE_SETTINGS_TYPE_LAYER_HTTPHOST_SAMPLE_RATE,
        SettingsUtil.convertType(Collector.OboeSettingType.LAYER_HTTPHOST_SAMPLE_RATE));
  }
}
