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

package com.solarwinds.joboe.core;

import com.solarwinds.joboe.sampling.Metadata;
import java.nio.ByteBuffer;
import java.util.Map;

public class NoopEvent extends Event {
  NoopEvent(Metadata metadata) {
    super(metadata);
  }

  @Override
  public void addInfo(String key, Object value) {}

  @Override
  public void addInfo(Map<String, ?> infoMap) {}

  @Override
  public void addInfo(Object... info) {}

  @Override
  public void addEdge(Metadata md) {}

  @Override
  public void addEdge(String hexstr) {}

  @Override
  public void setAsync() {}

  @Override
  public void report(EventReporter reporter) {}

  @Override
  public void report(Metadata md, EventReporter reporter) {}

  @Override
  public byte[] toBytes() {
    return null;
  }

  @Override
  public ByteBuffer toByteBuffer() {
    return null;
  }

  @Override
  public void setTimestamp(long timestamp) {}

  @Override
  public void setThreadId(Long threadId) {}
}
