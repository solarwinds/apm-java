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

import com.solarwinds.joboe.core.profiler.ProfileSampleEmitter;
import com.solarwinds.joboe.logging.LoggerFactory;
import com.solarwinds.joboe.shaded.google.gson.Gson;
import com.solarwinds.joboe.shaded.google.gson.GsonBuilder;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import java.util.List;
import java.util.Map;

public class SampleEmitter implements ProfileSampleEmitter {
  private static final Gson gson = new GsonBuilder().create();

  private AttributesBuilder entryAttributesBuilder;

  private AttributesBuilder exitAttributesBuilder;

  private AttributesBuilder sampleAttributesBuilder;

  private ReadWriteSpan readWriteSpan;

  public SampleEmitter(ReadWriteSpan readWriteSpan) {
    this.readWriteSpan = readWriteSpan;
  }

  @Override
  public <T> void addAttribute(String key, T value) {
    if (value instanceof String) {
      if (entryAttributesBuilder != null) {
        entryAttributesBuilder.put(AttributeKey.stringKey(key), (String) value);
      }

      if (sampleAttributesBuilder != null) {
        sampleAttributesBuilder.put(AttributeKey.stringKey(key), (String) value);
      }

      if (exitAttributesBuilder != null) {
        exitAttributesBuilder.put(AttributeKey.stringKey(key), (String) value);
      }
    }

    if (value instanceof Long) {
      if (entryAttributesBuilder != null) {
        entryAttributesBuilder.put(AttributeKey.longKey(key), (Long) value);
      }

      if (sampleAttributesBuilder != null) {
        sampleAttributesBuilder.put(AttributeKey.longKey(key), (Long) value);
      }

      if (exitAttributesBuilder != null) {
        exitAttributesBuilder.put(AttributeKey.longKey(key), (Long) value);
      }
    }

    if (value instanceof List) {
      boolean match = ((List<?>) value).stream().anyMatch(item -> item instanceof Long);
      Long[] longs = null;
      if (match) {
        longs = ((List<?>) value).stream().map(Long.class::cast).toArray(Long[]::new);
      }

      if (entryAttributesBuilder != null && match) {
        entryAttributesBuilder.put(AttributeKey.longArrayKey(key), longs);
      }

      if (sampleAttributesBuilder != null && match) {
        sampleAttributesBuilder.put(AttributeKey.longArrayKey(key), longs);
      }

      if (exitAttributesBuilder != null && match) {
        exitAttributesBuilder.put(AttributeKey.longArrayKey(key), longs);
      }

      match = ((List<?>) value).stream().anyMatch(item -> item instanceof Map);
      String[] strings = null;
      if (match) {
        strings = ((List<?>) value).stream().map(gson::toJson).toArray(String[]::new);
      }

      if (entryAttributesBuilder != null && match) {
        entryAttributesBuilder.put(AttributeKey.stringArrayKey(key), strings);
      }

      if (sampleAttributesBuilder != null && match) {
        sampleAttributesBuilder.put(AttributeKey.stringArrayKey(key), strings);
      }

      if (exitAttributesBuilder != null && match) {
        exitAttributesBuilder.put(AttributeKey.stringArrayKey(key), strings);
      }
    }
  }

  @Override
  public void beginEntryEmit() {
    entryAttributesBuilder = Attributes.builder();
  }

  @Override
  public void beginExitEmit() {
    exitAttributesBuilder = Attributes.builder();
  }

  @Override
  public void beginSampleEmit() {
    sampleAttributesBuilder = Attributes.builder();
  }

  @Override
  public boolean endEntryEmit() {
    readWriteSpan.addEvent("sw.profile", entryAttributesBuilder.build());
    entryAttributesBuilder = null;
    LoggerFactory.getLogger().debug("Entry emit has been ended: span = " + readWriteSpan);
    return true;
  }

  @Override
  public boolean endExitEmit() {
    readWriteSpan.addEvent("sw.profile", exitAttributesBuilder.build());
    LoggerFactory.getLogger().debug("Exit emit has been ended: span = " + readWriteSpan);
    exitAttributesBuilder = null;
    readWriteSpan = null;
    return true;
  }

  @Override
  public boolean endSampleEmit() {
    readWriteSpan.addEvent("sw.profile", sampleAttributesBuilder.build());
    LoggerFactory.getLogger().debug("Sample emit has been ended: span = " + readWriteSpan);
    sampleAttributesBuilder = null;
    return true;
  }
}
