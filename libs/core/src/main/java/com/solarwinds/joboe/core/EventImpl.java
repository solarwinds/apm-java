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

import static com.solarwinds.joboe.core.Constants.*;

import com.solarwinds.joboe.core.ebson.BsonDocument;
import com.solarwinds.joboe.core.ebson.BsonDocuments;
import com.solarwinds.joboe.core.ebson.BsonToken;
import com.solarwinds.joboe.core.ebson.BsonWriter;
import com.solarwinds.joboe.core.ebson.MultiValList;
import com.solarwinds.joboe.core.util.HostInfoUtils;
import com.solarwinds.joboe.core.util.JavaProcessUtils;
import com.solarwinds.joboe.core.util.TimeUtils;
import com.solarwinds.joboe.logging.Logger;
import com.solarwinds.joboe.logging.LoggerFactory;
import com.solarwinds.joboe.sampling.Metadata;
import com.solarwinds.joboe.sampling.SamplingException;
import java.lang.reflect.Array;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;
import java.util.Map.Entry;

public class EventImpl extends Event {
  private static final Logger logger = LoggerFactory.getLogger();

  private BsonDocument.Builder bsonBuilder;
  private final MultiValList<String> edges = new MultiValList<String>(1);
  private boolean isAsync = false;

  private boolean isEntry = false;
  private boolean isExit = false;

  private Long threadId = null;

  private Long timestamp = null;

  static final int MAX_KEY_COUNT = 1024;
  private static final Collection<String> BASIC_KEYS =
      Arrays.asList(
          "Layer",
          "Label",
          Constants.XTR_ASYNC_KEY,
          Constants.XTR_EDGE_KEY,
          Constants.XTR_AO_EDGE_KEY,
          Constants.XTR_THREAD_ID_KEY,
          Constants.XTR_HOSTNAME_KEY,
          Constants.XTR_METADATA_KEY,
          Constants.XTR_XTRACE,
          Constants.XTR_PROCESS_ID_KEY,
          Constants.XTR_TIMESTAMP_U_KEY);

  // Buffer: used for building BSON byte stream, one-per-thread so we don't keep reallocating
  // buffers
  private static final ThreadLocal<ByteBuffer> BUFFER =
      new ThreadLocal<ByteBuffer>() {
        @Override
        protected ByteBuffer initialValue() {
          return ByteBuffer.allocate(MAX_EVENT_BUFFER_SIZE).order(ByteOrder.LITTLE_ENDIAN);
        }
      };

  public EventImpl(Metadata ctxMetadata, boolean addEdge) {
    super(new Metadata(ctxMetadata));
    if (ctxMetadata.isSampled()) { // only further init the metadata if it's being traced
      init();
      if (addEdge) {
        addEdge(ctxMetadata);
      }
    }
  }

  /**
   * Creates an event with a previously determined metadataID . See ServletInstrumentation for an
   * example where this was needed.
   */
  public EventImpl(Metadata ctxMetadata, String metadataID, boolean addEdge)
      throws SamplingException {
    super(new Metadata(metadataID));
    initOverride();
    if (addEdge) {
      addEdge(ctxMetadata);
    }
  }

  /**
   * Creates an event with a previously determined metadata
   *
   * @param parentMetadata
   * @param eventMetadata
   */
  EventImpl(Metadata parentMetadata, Metadata eventMetadata) {
    super(eventMetadata);
    initOverride();
    if (parentMetadata != null) {
      addEdge(parentMetadata);
    }
  }

  private void init() {
    metadata.randomizeOpID();
    bsonBuilder = BsonDocuments.builder();
    String traceContext = metadata.toHexString();
    bsonBuilder.put(XTR_METADATA_KEY, traceContext);
    bsonBuilder.put(XTR_XTRACE, w3cContextToXTrace(traceContext));
  }

  /**
   * Sets XTrace ID - used in special cases where we need to override (See ServlerInstrumentation
   * for an example)
   */
  private void initOverride() {
    // We do NOT randomize here because we assume we are constructed with a metadata that was
    // already randomized
    bsonBuilder = BsonDocuments.builder();
    String traceContext = metadata.toHexString();
    bsonBuilder.put(XTR_METADATA_KEY, traceContext);
    bsonBuilder.put(XTR_XTRACE, w3cContextToXTrace(traceContext));
  }

  protected static String w3cContextToXTrace(String w3cContext) {
    String[] arr = w3cContext.split("-");
    if (arr.length != 4) {
      return "";
    }

    String padding = "00000000"; // eight zeros
    return "2B" + arr[1].toUpperCase() + padding + arr[2].toUpperCase() + arr[3];
  }

  /* (non-Javadoc)
   * @see com.solarwinds.joboe.core.Event#addInfo(java.lang.String, java.lang.Object)
   */
  @Override
  public void addInfo(String key, Object value) {
    if (metadata.isSampled()) {
      insertToBsonBuilder(key, value);
    }
  }

  /* (non-Javadoc)
   * @see com.solarwinds.joboe.core.Event#addInfo(java.util.Map)
   */
  @Override
  public void addInfo(Map<String, ?> infoMap) {
    if (metadata.isSampled()) {
      for (Map.Entry<String, ?> entry : infoMap.entrySet()) {
        insertToBsonBuilder(entry.getKey(), entry.getValue());
      }
    }
  }

  /* (non-Javadoc)
   * @see com.solarwinds.joboe.core.Event#addInfo(java.lang.Object)
   */
  @Override
  public void addInfo(Object... info) {
    if (metadata.isSampled()) {
      if (info.length % 2 == 1) {
        throw new RuntimeException("Even number of arguments expected");
      }

      for (int i = 0; i < info.length / 2; i++) {
        if (!(info[i * 2] instanceof String)) {
          throw new RuntimeException("String expected.");
        }
        insertToBsonBuilder((String) info[i * 2], info[i * 2 + 1]);
      }
    }
  }

  /**
   * Inserts custom keys and values into the Bson map, certain checks might be performed (for
   * example checking whether it's entry/exit event), But the key and value will always be inserted
   * into the Bson map (hence not validations)
   *
   * @param key
   * @param value
   */
  private void insertToBsonBuilder(String key, Object value) {
    if ("Label".equals(key) && value instanceof String) {
      if ("entry".equals(value)) {
        isEntry = true;
      } else if ("exit".equals(value)) {
        isExit = true;
      }
    } else if ("Backtrace".equals(key)
        && !metadata.incrNumBacktraces()) { // do not add backtrace if limit is exceeded
      return;
    }

    bsonBuilder.put(key, value);
  }

  /* (non-Javadoc)
   * @see com.solarwinds.joboe.core.Event#addEdge(com.solarwinds.joboe.sampling.Metadata)
   */
  @Override
  public void addEdge(Metadata md) {
    if (metadata.isSampled()
        && md.isSampled()
        && metadata.isTaskEqual(md)
        && !edges.contains(md.opHexString())) {
      edges.add(md.opHexString());
    }
  }

  /* (non-Javadoc)
   * @see com.solarwinds.joboe.core.Event#addEdge(java.lang.String)
   */
  @Override
  public void addEdge(String hexstr) {
    try {
      addEdge(new Metadata(hexstr));
    } catch (SamplingException ex) {
      logger.debug("Invalid XTrace ID: " + hexstr, ex);
    }
  }

  /* (non-Javadoc)
   * @see com.solarwinds.joboe.core.Event#setAsync()
   */
  @Override
  public void setAsync() {
    this.isAsync = true;
  }

  /* (non-Javadoc)
   * @see com.solarwinds.joboe.core.Event#report(com.solarwinds.joboe.core.EventReporter)
   */
  @Override
  public void report(EventReporter reporter) {
    report(Context.getMetadata(), reporter);
  }

  /* (non-Javadoc)
   * @see com.solarwinds.joboe.core.Event#report(com.solarwinds.joboe.sampling.Metadata, com.solarwinds.joboe.core.EventReporter)
   */
  @Override
  public void report(Metadata contextMetadata, EventReporter reporter) {
    if (reporter == null) {
      return;
    }

    if (contextMetadata != null) {
      if (!metadata.isSampled()) {
        return;
      }

      // Event metadata must have the same taskID as the event
      if (!metadata.isTaskEqual(contextMetadata)) {
        return;
      }

      // Event metadata must has a different opID
      if (metadata.isOpEqual(contextMetadata)) {
        return;
      }
    }

    // Add common key/value pairs
    addEdges();
    addTimestamps();
    addProcessInfo();
    addHostname();

    if (isAsync) { // if the event is marked explicitly as async
      addAsync();
    } else if (contextMetadata != null
        && contextMetadata.isAsync()) { // or if the associated context is marked as async
      if (isEntry) {
        if (contextMetadata.incrementAndGetAsyncLayerLevel() == 1) {
          addAsync(); // only report async on entry event of the top extent within an async stack
        }
      } else if (isExit) {
        contextMetadata.decrementAndGetAsyncLayerLevel();
      }
    }

    try {
      reporter.send(this);
      // Update the context's opID to that of the event
      if (contextMetadata != null) {
        contextMetadata.setOpID(metadata);
      }
    } catch (EventReporterException e) {
      logger.trace(
          "Failed to send out event, exception message ["
              + e.getMessage()
              + "]. Please take note that existing code flow should not be affected, this might only impact the instrumentation of current trace");
    } catch (Throwable ex) {
      logger.error(
          "Failed to send out event, exception message ["
              + ex.getMessage()
              + "]. Please take note that existing code flow should not be affected, this might only impact the instrumentation of current trace",
          ex);
    }
  }

  /* (non-Javadoc)
   * @see com.solarwinds.joboe.core.Event#toBytes()
   */
  @Override
  public byte[] toBytes() throws BsonBufferException {
    BsonDocument doc = bsonBuilder.build();
    BsonWriter writer = BsonToken.DOCUMENT.writer();
    ByteBuffer buffer = BUFFER.get();
    buffer.clear(); // cast for JDK 8- runtime compatibility

    boolean retry = false;
    try {
      writer.writeTo(buffer, doc);
    } catch (BufferOverflowException e) { // cannot use multi-catch for 1.6 source compatibility
      retry = true;
    } catch (IllegalArgumentException e) {
      retry = true;
    }

    if (retry) {
      logger.warn("The KVs are too big to be converted. Trimming down the KVs...");
      doc = trimDoc(doc);

      buffer.clear(); // cast for JDK 8- runtime compatibility

      if (doc != null) {
        RuntimeException bsonException = null;
        try {
          writer.writeTo(buffer, doc); // try again
        } catch (BufferOverflowException e) { // cannot use multi-catch for 1.6 source compatibility
          bsonException = e;
        } catch (IllegalArgumentException e) {
          bsonException = e;
        }

        // still failing after retry, throw exception
        if (bsonException != null) {
          throw new BsonBufferException(bsonException);
        }
      }
    }

    buffer.flip(); // cast for JDK 8- runtime compatibility

    byte[] bytes = new byte[buffer.remaining()]; // allocate an array with the actual size required
    buffer.get(bytes);
    return bytes;
  }

  /* (non-Javadoc)
   * @see com.solarwinds.joboe.core.Event#toByteBuffer()
   */
  @Override
  public ByteBuffer toByteBuffer() throws BsonBufferException {
    return ByteBuffer.wrap(toBytes());
  }

  /**
   * Trims the BsonDocument using a conservative strategy. This should only be invoked as a fall
   * back to when a BufferOverflowException is encountered during event conversion. Each event
   * creator (instrumentation, metrics collector) should try their best to avoid overflowing the
   * event
   *
   * @param doc
   * @return trimmed BsonDocument
   */
  private BsonDocument trimDoc(BsonDocument doc) {
    BsonDocument.Builder newBuilder = BsonDocuments.builder();

    int bytesAllowed = MAX_EVENT_BUFFER_SIZE; // bytes allowed to build the new doc(event)

    // First extract basic key values, those should not be subject to trimming
    Map<String, Object> basicKeyValues = extractBasicKeyValues(doc);

    // calculate bytes taken by the basic key values
    for (Entry<String, Object> basicKeyValue : basicKeyValues.entrySet()) {
      bytesAllowed -= getBsonByteSize(basicKeyValue.getKey());
      try {
        bytesAllowed -= getBsonByteSize(basicKeyValue.getValue());
      } catch (IllegalArgumentException e) {
        logger.warn(
            "Unknown value type for basic key ["
                + basicKeyValue.getKey()
                + "]. Type ["
                + basicKeyValue.getValue().getClass().getName()
                + "]");
      }
    }

    if (bytesAllowed < 0) { // should not happen...unless we add in some crazy basic keys or the
      // MAX_EVENT_BUFFER_SIZE is unreasonably small
      logger.warn("Cannot send an event as the basic key values fail to fit in the event!");
      return null;
    }

    newBuilder.putAll(basicKeyValues); // add all basic keys

    Map<String, Object> otherKeyValues = new LinkedHashMap<String, Object>(doc);
    otherKeyValues
        .keySet()
        .removeAll(BASIC_KEYS); // now the otherKeyValues contain all the non-basic keys

    // First try to trim down the number of entries based on MAX_KEY_COUNT
    trimKeyValues(otherKeyValues);

    // Now iterate through each KV entry and check whether trimming to its value is necessary
    int maxBytePerKeyValue =
        bytesAllowed
            / otherKeyValues
                .size(); // just an approximation. Take note this is a conservative estimation and
    // will not fully utilize all the space available.

    logger.debug(
        "Trimming KVs on other key count "
            + otherKeyValues.size()
            + " maxBytePerKeyValue "
            + maxBytePerKeyValue);

    // test buffer to check accumulative size of the bson document to be built
    ByteBuffer testBuffer =
        ByteBuffer.allocate(MAX_EVENT_BUFFER_SIZE).order(ByteOrder.LITTLE_ENDIAN);
    BsonWriter testWriter = BsonToken.DOCUMENT.writer();

    testWriter.writeTo(testBuffer, newBuilder.build());

    for (Entry<String, Object> entry : otherKeyValues.entrySet()) {
      // check if key alone would fit
      if (getBsonByteSize(entry.getKey()) >= maxBytePerKeyValue) {
        logger.warn(
            "Dropping event entry with key ["
                + entry.getKey().substring(0, 10)
                + "...] as the key is too long");
        continue;
      }

      int maxByteForValue =
          maxBytePerKeyValue - getBsonByteSize(entry.getKey()); // the byte remaining for the value

      // check if the value would fit in the remaining byte
      Object value = entry.getValue();
      if (value instanceof String) { // trim if string is too long
        if (getBsonByteSize(value) > maxByteForValue) {
          value = ((String) value).substring(0, maxByteForValue / 2); // 2 byte each character
        }
      } else if (value instanceof Object[]) { // trim if array is too long
        Object[] valueArray = (Object[]) value;

        int arrayValueSize = 0;

        // iterate the array calculate how much byte has been taken, if it exceeds maxBytePerValue,
        // drop the current array element and the rest in the array
        int arrayWalker;
        for (arrayWalker = 0; arrayWalker < valueArray.length; arrayWalker++) {
          Object arrayElement = valueArray[arrayWalker];
          try {
            arrayValueSize += getBsonByteSize(arrayElement);
            arrayValueSize +=
                8; // also it keeps track of the index of the array. adding 8 as a conservative
            // estimate
          } catch (IllegalArgumentException e) {
            logger.warn(
                "Found unknown type ["
                    + arrayElement.getClass().getName()
                    + "] in KV "
                    + entry.getKey()
                    + " while trimming the doc. Trimming the rest of the elements in the array...");
            break;
          }

          if (arrayValueSize > maxByteForValue) {
            logger.warn(
                "Found oversized array in KV "
                    + entry.getKey()
                    + " while trimming the doc. Trimming the elements with index from "
                    + arrayWalker);
            break;
          }
        }

        if (arrayWalker < valueArray.length) { // trim the array in this KV
          Object[] trimmedArray = new Object[arrayWalker];
          System.arraycopy(valueArray, 0, trimmedArray, 0, arrayWalker);
          value = trimmedArray;
        }
      } else { // not string or object[], not going to trim
        try {
          logger.debug(
              "Skip "
                  + entry.getKey()
                  + " for trimming as it is type "
                  + (entry.getValue() != null ? entry.getValue().getClass().getName() : "null ")
                  + ". Estimated size is "
                  + getBsonByteSize(entry.getValue()));
        } catch (IllegalArgumentException e) {
          logger.warn(
              "Skip "
                  + entry.getKey()
                  + " for trimming as it is type "
                  + (entry.getValue() != null ? entry.getValue().getClass().getName() : "null ")
                  + ". Size cannot be estimated: "
                  + e.getMessage(),
              e);
        }
      }

      // try to serialize it to ensure it would not throw exception, take note that the test buffer
      // accumulates each KV as a separate test document
      // and each test document is just a single KV bson
      // This comparison should work as the size of the actual bson document which contains all KVs
      // should be slightly smaller than many single KV bson documents combined
      BsonDocument.Builder testBuilder = BsonDocuments.builder();
      testBuilder.put(entry.getKey(), value);
      int lastPosition = testBuffer.position();
      try {
        testWriter.writeTo(testBuffer, testBuilder.build());

        // safe to add this to builder
        newBuilder.put(entry.getKey(), value);
      } catch (BufferOverflowException e) {
        logger.warn(
            "Failed to write KV ["
                + entry.getKey()
                + "] to event, as adding it does not fit the bytebuffer, skipping this KV!");
        testBuffer.position(
            lastPosition); // roll back to last position. Cast for JDK 8- runtime compatibility
      }
    }

    BsonDocument newDocument = newBuilder.build();

    return newDocument;
  }

  /**
   * Returns the estimate byte size of the object in bson. Take note that this only account for Bson
   * object type
   *
   * @param object to use for byte size estimate
   */
  private static int getBsonByteSize(Object object) {
    if (object instanceof String) { // if it's a String element check if it's too long
      return ((String) object).length() * 2; // 2 byte each character
    } else if (object instanceof Boolean) {
      return 1;
    } else if (object instanceof Byte) {
      return 1;
    } else if (object instanceof Double) {
      return Double.SIZE / 8;
    } else if (object instanceof Integer) {
      return Integer.SIZE / 8;
    } else if (object instanceof Long) {
      return Long.SIZE / 8;
    } else if (object != null && object.getClass().isArray()) {
      int size = 0;
      int length = Array.getLength(object);
      for (int i = 0; i < length; i++) {
        Object obj = Array.get(object, i);
        size += getBsonByteSize(obj);
      }
      return size;
    } else if (object instanceof MultiValList) {
      MultiValList<?> list = (MultiValList<?>) object;
      int size = 0;
      for (Object element : list) {
        size += getBsonByteSize(element);
      }
      return size;
    } else if (object instanceof Collection) {
      Collection<?> collection = (Collection<?>) object;
      int size = 0;
      Iterator<?> iterator = collection.iterator();
      while (iterator.hasNext()) {
        size += getBsonByteSize(iterator.next());
      }
      return size;
    } else if (object instanceof Map) {
      Map<?, ?> map = (Map<?, ?>) object;
      int size = 0;
      for (Entry<?, ?> entry : map.entrySet()) {
        size += getBsonByteSize(entry.getKey()) + getBsonByteSize(entry.getValue());
      }
      return size;
    } else if (object == null) {
      return 1;
    } else { // other unknown types. To be safe, just drop from here
      throw new IllegalArgumentException(
          "unknown size for type [" + object.getClass().getName() + "]");
    }
  }

  /**
   * Trim the input KV map such that it contains up to MAX_KEY_COUNT entries.
   *
   * @param keyValues
   * @return a new Map instance of the trimmed entries
   */
  private static void trimKeyValues(Map<String, Object> keyValues) {
    if (keyValues.size() > MAX_KEY_COUNT) {
      logger.warn(
          "Found " + keyValues.size() + " KVs in the event, trimming it down to " + MAX_KEY_COUNT);
      Set<String> trimmedKeys = new HashSet<String>();

      for (Entry<String, Object> entry : keyValues.entrySet()) {
        if (trimmedKeys.size() >= MAX_KEY_COUNT) {
          break;
        }
        trimmedKeys.add(entry.getKey());
      }

      keyValues.keySet().retainAll(trimmedKeys);
    }
  }

  /**
   * @param keyValues
   * @return a new instance of Map of key values that with keys defined in BASIC_KEYS
   */
  private static Map<String, Object> extractBasicKeyValues(Map<String, Object> keyValues) {
    Map<String, Object> basicKeyValues = new LinkedHashMap<String, Object>(keyValues);
    basicKeyValues.keySet().retainAll(BASIC_KEYS);
    return basicKeyValues;
  }

  private void addTimestamps() {
    if (timestamp == null) {
      timestamp = TimeUtils.getTimestampMicroSeconds();
    }

    addInfo(XTR_TIMESTAMP_U_KEY, timestamp);
  }

  private void addHostname() {
    addInfo(XTR_HOSTNAME_KEY, HostInfoUtils.getHostName());
  }

  private void addProcessInfo() {
    addInfo(XTR_THREAD_ID_KEY, threadId != null ? threadId : Thread.currentThread().getId());
    addInfo(XTR_PROCESS_ID_KEY, JavaProcessUtils.getPid());
  }

  private void addEdges() {
    if (!edges.isEmpty()) {
      addInfo(XTR_EDGE_KEY, edges);
      addInfo(XTR_AO_EDGE_KEY, toAOEdges(edges));
    }
  }

  private MultiValList<String> toAOEdges(MultiValList<String> edges) {
    MultiValList<String> aoEdges = new MultiValList<>();
    for (String edge : edges) {
      aoEdges.add(edge.toUpperCase());
    }
    return aoEdges;
  }

  private void addAsync() {
    addInfo(XTR_ASYNC_KEY, true);
  }

  /* (non-Javadoc)
   * @see com.solarwinds.joboe.core.Event#setTimestamp(long)
   */
  @Override
  public void setTimestamp(long timestamp) {
    this.timestamp = timestamp;
  }

  @Override
  public void setThreadId(Long threadId) {
    this.threadId = threadId;
  }
}
