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

public abstract class Event {
  protected final Metadata metadata;

  protected Event(Metadata metadata) {
    this.metadata = metadata;
  }

  /**
   * Add key/value pair to event
   *
   * @param key
   * @param value
   */
  public abstract void addInfo(String key, Object value);

  /**
   * Add all key /value pairs to event
   *
   * @param infoMap
   */
  public abstract void addInfo(Map<String, ?> infoMap);

  /**
   * Add all key/value pairs to event. This assumes that info contains alternating Name/Value pairs
   * (String, Object).
   *
   * @param info
   */
  public abstract void addInfo(Object... info);

  public abstract void addEdge(Metadata md);

  public abstract void addEdge(String hexstr);

  /** Marks event as Asynchronous. (One could also add this k/v manually.) */
  public abstract void setAsync();

  public final Metadata getMetadata() {
    return metadata;
  }

  /**
   * Report event to agent
   *
   * @param md metadata from context - if null, then no context metadata check and update will be
   *     done
   * @param reporter Event Reporter
   */
  public abstract void report(Metadata md, EventReporter reporter);

  public abstract byte[] toBytes() throws BsonBufferException;

  public abstract ByteBuffer toByteBuffer() throws BsonBufferException;

  /**
   * Sets timestamp in microsecond since epoch time
   *
   * @param timestamp
   */
  public abstract void setTimestamp(long timestamp);

  /**
   * Sets an explicit thread ID to be reported in the event
   *
   * @param threadId
   */
  public abstract void setThreadId(Long threadId);
}
