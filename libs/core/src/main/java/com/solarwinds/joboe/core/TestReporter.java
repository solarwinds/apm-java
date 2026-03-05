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

import com.solarwinds.joboe.core.ebson.BsonDocument;
import com.solarwinds.joboe.core.ebson.BsonDocuments;
import com.solarwinds.joboe.logging.Logger;
import com.solarwinds.joboe.logging.LoggerFactory;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Mock reporter: stores all 'sent' bytes in memory, for testing. Take note that this reporter works
 * in a ThreadLocal manner such that events collected/retrieved are only visible to the thread
 * itself
 *
 * @author pluk
 */
public class TestReporter implements EventReporter {
  private static final Logger logger = LoggerFactory.getLogger();

  private final ThreadLocal<Deque<byte[]>> byteBufList = ThreadLocal.withInitial(LinkedList::new);

  public TestReporter() {}

  public void reset() {
    byteBufList.set(new LinkedList<>());
  }

  @Override
  public void send(Event event) {
    try {
      byte[] buf = event.toBytes();
      logger.debug("Sent " + buf.length + " bytes");
      byteBufList.get().add(buf);
    } catch (BsonBufferException e) {
      logger.error("Failed to send events : " + e.getMessage(), e);
    }
  }

  public byte[] getLastSent() {
    return byteBufList.get().getLast();
  }

  public List<BsonDocument> getSentEventsAsBsonDocument() {
    List<BsonDocument> documents = new ArrayList<BsonDocument>();
    for (byte[] eventBytes : getBufList()) {
      documents.add(getBsonDocumentFromBytes(eventBytes));
    }

    return documents;
  }

  public List<DeserializedEvent> getSentEvents(boolean includeInitEvents) {
    List<DeserializedEvent> events = new ArrayList<DeserializedEvent>();
    Set<String> initLayers = new HashSet<String>();
    for (byte[] eventBytes : getBufList()) {
      boolean isInitEvent = false;
      Map<String, Object> kvs = new HashMap<String, Object>();

      for (Map.Entry<String, Object> kv : getEntriesFromBytes(eventBytes)) {
        // make sure it is not the init event, we do not count init events here
        if (!includeInitEvents) {
          if ("__Init".equals(kv.getKey())) { // then this is the init entry event
            isInitEvent = true;
          } else if ("Layer".equals(kv.getKey())) {
            if (initLayers.contains(kv.getValue().toString())) { // then this is the init exit event
              isInitEvent = true;
            }
          }
        }
        kvs.put(kv.getKey(), kv.getValue());
      }

      if (!isInitEvent) {
        events.add(new DeserializedEvent(kvs));
      } else {
        // track the layer name, we want to get rid of the corresponding exit init event ooo
        initLayers.add((String) kvs.get("Layer"));
      }
    }
    return events;
  }

  public List<DeserializedEvent> getSentEvents() {
    return getSentEvents(false);
  }

  public Deque<byte[]> getBufList() {
    return byteBufList.get();
  }

  /**
   * Event does not provide getInfo method, so we need this helper method to get the info in order
   * to verify the result
   *
   * @param event
   * @param key
   * @return
   */
  public static Object getValueFromEvent(Event event, String key) {
    ByteBuffer buffer =
        ByteBuffer.allocate(Constants.MAX_EVENT_BUFFER_SIZE).order(ByteOrder.LITTLE_ENDIAN);
    try {
      buffer.put(event.toBytes());
      buffer.flip();

      BsonDocument doc = BsonDocuments.readFrom(buffer);
      return doc.get(key);
    } catch (BsonBufferException e) {
      logger.error("Failed to get value from event : " + e.getMessage(), e);
      return null;
    }
  }

  private static Set<Entry<String, Object>> getEntriesFromBytes(byte[] bytes) {
    return getBsonDocumentFromBytes(bytes).entrySet();
  }

  private static BsonDocument getBsonDocumentFromBytes(byte[] bytes) {
    ByteBuffer buffer =
        ByteBuffer.allocate(Constants.MAX_EVENT_BUFFER_SIZE).order(ByteOrder.LITTLE_ENDIAN);
    buffer.put(bytes);
    buffer.flip();

    return BsonDocuments.readFrom(buffer);
  }

  public static class DeserializedEvent {
    private Map<String, Object> kvs = new HashMap<String, Object>();

    private DeserializedEvent(Map<String, Object> kvs) {
      this.kvs = kvs;
    }

    public Map<String, Object> getSentEntries() {
      return new HashMap<String, Object>(kvs);
    }

    @Override
    public String toString() {
      return getSentEntries().toString();
    }
  }

  @Override
  public EventReporterStats consumeStats() {
    int sentEvent = getBufList().size();
    return new EventReporterStats(sentEvent, 0, 0, sentEvent, 0);
  }

  @Override
  public void close() {}
}
