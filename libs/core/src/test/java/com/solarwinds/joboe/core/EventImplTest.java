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

import static com.solarwinds.joboe.core.Constants.XTR_ASYNC_KEY;
import static com.solarwinds.joboe.core.Constants.XTR_EDGE_KEY;
import static com.solarwinds.joboe.core.Constants.XTR_HOSTNAME_KEY;
import static com.solarwinds.joboe.core.Constants.XTR_METADATA_KEY;
import static com.solarwinds.joboe.core.Constants.XTR_PROCESS_ID_KEY;
import static com.solarwinds.joboe.core.Constants.XTR_THREAD_ID_KEY;
import static com.solarwinds.joboe.core.Constants.XTR_TIMESTAMP_U_KEY;
import static com.solarwinds.joboe.core.Constants.XTR_XTRACE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.solarwinds.joboe.config.InvalidConfigException;
import com.solarwinds.joboe.core.TestReporter.DeserializedEvent;
import com.solarwinds.joboe.core.ebson.BsonDocument;
import com.solarwinds.joboe.core.ebson.BsonReader;
import com.solarwinds.joboe.core.ebson.BsonToken;
import com.solarwinds.joboe.core.util.TestUtils;
import com.solarwinds.joboe.sampling.Metadata;
import com.solarwinds.joboe.sampling.SamplingConfiguration;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class EventImplTest {
  private final Logger log = Logger.getLogger(getClass().getName());
  private static final TestReporter reporter = TestUtils.initTraceReporter();

  @BeforeEach
  void setup() {
    Metadata.setup(SamplingConfiguration.builder().build());
  }

  @AfterEach
  protected void tearDown() {
    Assertions.assertNotNull(reporter);
    reporter.reset();
  }

  /*  Test that events can be sent and decoded, using a mock sender. */
  @Test
  public void testLocalSendEvent() {

    // Metadata for the context:
    Metadata md = new Metadata();
    md.randomize();
    log.info("Context Metadata: " + md.toHexString());
    String origMdOp = md.opHexString();

    // Dummy up an event:
    Event evt = new EventImpl(md, true);

    String testKey = "test";
    String testVal = "testFromJava";

    evt.addInfo(testKey, testVal);
    log.info("Event Metadata: " + evt.getMetadata().toHexString());

    // Send it:
    TestReporter reporter = ReporterFactory.getInstance().createTestReporter();
    evt.report(md, reporter);

    // Decode what was "sent":
    ByteBuffer buf = ByteBuffer.wrap(reporter.getLastSent()).order(ByteOrder.LITTLE_ENDIAN);
    BsonReader reader = BsonToken.DOCUMENT.reader();
    BsonDocument doc = (BsonDocument) reader.readFrom(buf);

    // Check that we received expected values:
    assertEquals(doc.get(XTR_METADATA_KEY), evt.getMetadata().toHexString());
    assertEquals(doc.get(XTR_EDGE_KEY), origMdOp);
    assertEquals(doc.get(testKey), testVal);

    // If these aren't there the test will fail due to an exception:
    Integer pid = (Integer) doc.get(XTR_PROCESS_ID_KEY);
    Long tid = (Long) doc.get(XTR_THREAD_ID_KEY);
    String host = (String) doc.get(XTR_HOSTNAME_KEY);
    Long time_u = (Long) doc.get(XTR_TIMESTAMP_U_KEY);

    log.info(
        "Received: host: " + host + " pid: " + pid + " tid: " + tid + " timestamp_u:" + time_u);
  }

  /** Tests network send/receive by sending UDP to ourselves */
  @Test
  public void testNetworkSendEvent() throws Exception {

    // Metadata for the context:
    Metadata md = new Metadata();
    md.randomize();
    log.info("Context Metadata: " + md.toHexString());
    String origMdOp = md.opHexString();

    // Dummy up an event:
    Event evt = new EventImpl(md, true);

    // Set up a UDP receiver:
    int udpPort = 45352;
    final DatagramSocket listener = new DatagramSocket(udpPort);
    listener.setSoTimeout(5000);
    byte[] rcvBuf = new byte[16384];
    final DatagramPacket pkt = new DatagramPacket(rcvBuf, rcvBuf.length);

    // Send something to ourselves
    EventReporter reporter = ReporterFactory.getInstance().createUdpReporter("127.0.0.1", udpPort);
    evt.report(md, reporter);

    // Did we get it?
    listener.receive(pkt);
    log.info("Received: " + pkt.getLength());

    // Decode what was sent
    ByteBuffer buf = ByteBuffer.wrap(pkt.getData()).order(ByteOrder.LITTLE_ENDIAN);
    BsonReader reader = BsonToken.DOCUMENT.reader();
    BsonDocument doc = (BsonDocument) reader.readFrom(buf);

    // Check that we received expected values:
    assertEquals(doc.get(XTR_METADATA_KEY), evt.getMetadata().toHexString());
    assertEquals(doc.get(XTR_EDGE_KEY), origMdOp);

    // If these aren't there the test will fail due to an exception:
    Integer pid = (Integer) doc.get(XTR_PROCESS_ID_KEY);
    Long tid = (Long) doc.get(XTR_THREAD_ID_KEY);
    String host = (String) doc.get(XTR_HOSTNAME_KEY);
    Long time_u = (Long) doc.get(XTR_TIMESTAMP_U_KEY);

    log.info(
        "Received: host: " + host + " pid: " + pid + " tid: " + tid + " timestamp_u:" + time_u);
  }

  @Test
  public void testEventReport() throws Exception {
    Metadata contextMetadata = new Metadata();
    contextMetadata.randomize(true);

    Event event;
    event = new EventImpl(new Metadata(), false); // invalid context
    event.report(contextMetadata, reporter);
    assert (reporter.getSentEvents().isEmpty());

    Metadata metadata = new Metadata();
    metadata.randomize(false); // not sampled
    event = new EventImpl(metadata, false); // not sampled context
    event.report(contextMetadata, reporter);
    assert (reporter.getSentEvents().isEmpty());

    event =
        new EventImpl(contextMetadata, contextMetadata.toHexString(), false); // same task and op id
    event.report(contextMetadata, reporter);
    assert (reporter.getSentEvents().isEmpty());

    Metadata differentTaskIdMetadata = new Metadata();
    differentTaskIdMetadata.randomize(true);
    event = new EventImpl(differentTaskIdMetadata, false); // different task Id
    event.report(contextMetadata, reporter);
    assert (reporter.getSentEvents().isEmpty());

    event =
        new EventImpl(contextMetadata, false); // valid metadata - same task ID but different op ID
    event.report(contextMetadata, null); // but reporter is null
    assert (reporter.getSentEvents().isEmpty());

    event =
        new EventImpl(contextMetadata, false); // valid metadata - same task ID but different op ID
    event.report(contextMetadata, reporter); // ok, should report event
    assertEquals(1, reporter.getSentEvents().size());
  }

  /** Over-sized event with a large KV */
  @Test
  public void testOversizedEvent1() {
    String bigValue = new String(new char[1000000]);

    Metadata md = new Metadata();
    md.randomize(true);
    Event testEvent = Context.createEventWithContext(md, false);

    testEvent.addInfo("key1", bigValue);
    testEvent.addInfo("key2", new String[] {bigValue, bigValue, bigValue});
    testEvent.addInfo("key3", 0);
    testEvent.addInfo("key4", false);
    testEvent.addInfo("key5", new String[0]);
    testEvent.addInfo("key6", new String[] {"hi"});
    testEvent.addInfo("key7", new Object[] {1, bigValue});

    Metadata testEdge = new Metadata(md);
    testEdge.randomizeOpID();
    testEvent.addEdge(testEdge.toHexString());

    TestReporter reporter = ReporterFactory.getInstance().createTestReporter();
    testEvent.report(md, reporter);

    // Decode what was "sent":
    ByteBuffer buf = ByteBuffer.wrap(reporter.getLastSent()).order(ByteOrder.LITTLE_ENDIAN);
    BsonReader reader = BsonToken.DOCUMENT.reader();
    BsonDocument doc = (BsonDocument) reader.readFrom(buf);

    // Check that we received expected values:
    assertTrue(bigValue.startsWith((String) doc.get("key1")));
    assertEquals(
        0,
        ((BsonDocument) doc.get("key2"))
            .size()); // bson converts it to BsonDocument, should not include any as the value is
    // too big to even fit in one
    assertEquals(0, doc.get("key3"));
    assertEquals(false, doc.get("key4"));
    assertEquals(0, ((BsonDocument) doc.get("key5")).size()); // bson converts it to BsonDocument
    assertEquals(1, ((BsonDocument) doc.get("key6")).size()); // bson converts it to BsonDocument
    assertEquals(
        1,
        ((BsonDocument) doc.get("key7"))
            .size()); // bson converts it to BsonDocument, should not include the 2nd argument as
    // it's too big
    assertEquals(testEdge.opHexString(), doc.get(Constants.XTR_EDGE_KEY));
  }

  /**
   * Over-sized event with too many KV pairs
   *
   * @throws InvalidConfigException
   */
  @Test
  public void testOversizedEvent2() throws InvalidConfigException {
    Metadata md = new Metadata();
    md.randomize(true);
    Event testEvent = Context.createEventWithContext(md, false);

    for (int i = 0; i < 10000; i++) {
      testEvent.addInfo(String.valueOf(i), i);
    }

    TestReporter reporter = ReporterFactory.getInstance().createTestReporter();
    testEvent.report(md, reporter);

    // Decode what was "sent":
    ByteBuffer buf = ByteBuffer.wrap(reporter.getLastSent()).order(ByteOrder.LITTLE_ENDIAN);
    BsonReader reader = BsonToken.DOCUMENT.reader();
    BsonDocument doc = (BsonDocument) reader.readFrom(buf);

    assertTrue(doc.containsKey(Constants.XTR_THREAD_ID_KEY));
    assertTrue(doc.containsKey(Constants.XTR_HOSTNAME_KEY));
    assertTrue(doc.containsKey(Constants.XTR_METADATA_KEY));
    assertTrue(doc.containsKey(XTR_XTRACE));
    assertTrue(doc.containsKey(Constants.XTR_PROCESS_ID_KEY));
    assertTrue(doc.containsKey(Constants.XTR_TIMESTAMP_U_KEY));

    // Check that we received expected max entries defined in Event.MAX_KEY_COUNT
    for (int i = 0; i < EventImpl.MAX_KEY_COUNT - 7; i++) { // minus the 7 important keys above
      assertEquals(doc.get(String.valueOf(i)), i);
    }
  }

  /**
   * Over-sized event with single KV as a huge array
   *
   * @throws InvalidConfigException
   */
  @Test
  public void testOversizedEvent3() throws InvalidConfigException {
    final int ARRAY_SIZE = 100000;
    Object[] hugeArray = new Object[ARRAY_SIZE];
    for (int i = 0; i < ARRAY_SIZE; i++) {
      hugeArray[i] = 0;
    }

    Metadata md = new Metadata();
    md.randomize(true);
    Event testEvent = Context.createEventWithContext(md, false);

    testEvent.addInfo("HugeArray", hugeArray);
    TestReporter reporter = ReporterFactory.getInstance().createTestReporter();
    testEvent.report(md, reporter);

    // Decode what was "sent":
    ByteBuffer buf = ByteBuffer.wrap(reporter.getLastSent()).order(ByteOrder.LITTLE_ENDIAN);
    BsonReader reader = BsonToken.DOCUMENT.reader();
    BsonDocument doc = (BsonDocument) reader.readFrom(buf);

    assertTrue(doc.containsKey(Constants.XTR_THREAD_ID_KEY));
    assertTrue(doc.containsKey(Constants.XTR_HOSTNAME_KEY));
    assertTrue(doc.containsKey(Constants.XTR_METADATA_KEY));
    assertTrue(doc.containsKey(XTR_XTRACE));
    assertTrue(doc.containsKey(Constants.XTR_PROCESS_ID_KEY));
    assertTrue(doc.containsKey(Constants.XTR_TIMESTAMP_U_KEY));

    // Check that at least part of the array is included
    assertTrue(doc.containsKey("HugeArray"));
  }

  /**
   * Over-sized event with single KV as a very long string
   *
   * @throws InvalidConfigException
   */
  @Test
  public void testOversizedEvent4() throws InvalidConfigException {
    final int STRING_APPEND_COUNT = 100000;
    StringBuffer longString = new StringBuffer();
    for (int i = 0; i < STRING_APPEND_COUNT; i++) {
      longString.append(i);
    }

    Metadata md = new Metadata();
    md.randomize(true);
    Event testEvent = Context.createEventWithContext(md, false);

    testEvent.addInfo("LongString", longString.toString());
    TestReporter reporter = ReporterFactory.getInstance().createTestReporter();
    testEvent.report(md, reporter);

    // Decode what was "sent":
    ByteBuffer buf = ByteBuffer.wrap(reporter.getLastSent()).order(ByteOrder.LITTLE_ENDIAN);
    BsonReader reader = BsonToken.DOCUMENT.reader();
    BsonDocument doc = (BsonDocument) reader.readFrom(buf);

    assertTrue(doc.containsKey(Constants.XTR_THREAD_ID_KEY));
    assertTrue(doc.containsKey(Constants.XTR_HOSTNAME_KEY));
    assertTrue(doc.containsKey(Constants.XTR_METADATA_KEY));
    assertTrue(doc.containsKey(XTR_XTRACE));
    assertTrue(doc.containsKey(Constants.XTR_PROCESS_ID_KEY));
    assertTrue(doc.containsKey(Constants.XTR_TIMESTAMP_U_KEY));

    // Check that at least part of the longString is included
    assertTrue(doc.containsKey("LongString"));
  }

  /**
   * Over-sized event with alot of KVs and long keys and values
   *
   * @throws InvalidConfigException
   */
  @Test
  public void testOversizedEvent5() throws InvalidConfigException {
    final int STRING_APPEND_COUNT = 1000;
    StringBuffer longString = new StringBuffer();
    for (int i = 0; i < STRING_APPEND_COUNT; i++) {
      longString.append(i);
    }
    final String longPrefix = longString.toString();

    Metadata md = new Metadata();
    md.randomize(true);
    Event testEvent = Context.createEventWithContext(md, false);

    for (int i = 0; i < 100; i++) {
      testEvent.addInfo(longPrefix + i, longPrefix + 1);
    }

    TestReporter reporter = ReporterFactory.getInstance().createTestReporter();
    testEvent.report(md, reporter);

    // Decode what was "sent":
    ByteBuffer buf = ByteBuffer.wrap(reporter.getLastSent()).order(ByteOrder.LITTLE_ENDIAN);
    BsonReader reader = BsonToken.DOCUMENT.reader();
    BsonDocument doc = (BsonDocument) reader.readFrom(buf);

    assertTrue(doc.containsKey(Constants.XTR_THREAD_ID_KEY));
    assertTrue(doc.containsKey(Constants.XTR_HOSTNAME_KEY));
    assertTrue(doc.containsKey(Constants.XTR_METADATA_KEY));
    assertTrue(doc.containsKey(XTR_XTRACE));
    assertTrue(doc.containsKey(Constants.XTR_PROCESS_ID_KEY));
    assertTrue(doc.containsKey(Constants.XTR_TIMESTAMP_U_KEY));

    // non of the long KVs made it as the key itself probably does not fit for the byte allocate for
    // each entry. This is ok as this is one extreme case
  }

  /**
   * Over-sized event with large map value
   *
   * @throws InvalidConfigException
   */
  @Test
  public void testOversizedEvent6() throws InvalidConfigException {
    int TEST_KEY_COUNT = 32 * 1024;
    String PREFIX = "漢字";
    Map<String, String> map = new HashMap<String, String>();
    for (int i = 0; i < TEST_KEY_COUNT; i++) {
      map.put(PREFIX + i, PREFIX);
    }

    Metadata md = new Metadata();
    md.randomize(true);
    Event testEvent = Context.createEventWithContext(md, false);

    testEvent.addInfo("large-map", map);
    testEvent.addInfo("k1", 1); // these 2 should still make it
    testEvent.addInfo("k2", "2");

    TestReporter reporter = ReporterFactory.getInstance().createTestReporter();
    testEvent.report(md, reporter);

    // Decode what was "sent":
    ByteBuffer buf = ByteBuffer.wrap(reporter.getLastSent()).order(ByteOrder.LITTLE_ENDIAN);
    BsonReader reader = BsonToken.DOCUMENT.reader();
    BsonDocument doc = (BsonDocument) reader.readFrom(buf);

    assertTrue(doc.containsKey(Constants.XTR_THREAD_ID_KEY));
    assertTrue(doc.containsKey(Constants.XTR_HOSTNAME_KEY));
    assertTrue(doc.containsKey(Constants.XTR_METADATA_KEY));
    assertTrue(doc.containsKey(XTR_XTRACE));
    assertTrue(doc.containsKey(Constants.XTR_PROCESS_ID_KEY));
    assertTrue(doc.containsKey(Constants.XTR_TIMESTAMP_U_KEY));

    // large-map should be dropped
    assertFalse(doc.containsKey("large-map"));
    // the 2 smaller KVs should be there
    assertEquals(1, doc.get("k1"));
    assertEquals("2", doc.get("k2"));
  }

  /**
   * Over-sized event with large collection value
   *
   * @throws InvalidConfigException
   */
  @Test
  public void testOversizedEvent7() throws InvalidConfigException {
    int TEST_KEY_COUNT = 32 * 1024;
    String PREFIX = "漢字";
    List<String> list = new ArrayList<String>();
    for (int i = 0; i < TEST_KEY_COUNT; i++) {
      list.add(PREFIX + i);
    }

    Metadata md = new Metadata();
    md.randomize(true);
    Event testEvent = Context.createEventWithContext(md, false);

    testEvent.addInfo("long-list", list);
    testEvent.addInfo("k1", 1); // these 2 should still make it
    testEvent.addInfo("k2", "2");

    TestReporter reporter = ReporterFactory.getInstance().createTestReporter();
    testEvent.report(md, reporter);

    // Decode what was "sent":
    ByteBuffer buf = ByteBuffer.wrap(reporter.getLastSent()).order(ByteOrder.LITTLE_ENDIAN);
    BsonReader reader = BsonToken.DOCUMENT.reader();
    BsonDocument doc = (BsonDocument) reader.readFrom(buf);

    assertTrue(doc.containsKey(Constants.XTR_THREAD_ID_KEY));
    assertTrue(doc.containsKey(Constants.XTR_HOSTNAME_KEY));
    assertTrue(doc.containsKey(Constants.XTR_METADATA_KEY));
    assertTrue(doc.containsKey(XTR_XTRACE));
    assertTrue(doc.containsKey(Constants.XTR_PROCESS_ID_KEY));
    assertTrue(doc.containsKey(Constants.XTR_TIMESTAMP_U_KEY));

    // long-list should be dropped
    assertFalse(doc.containsKey("long-list"));
    // the 2 smaller KVs should be there
    assertEquals(1, doc.get("k1"));
    assertEquals("2", doc.get("k2"));
  }

  @Test
  public void testAsyncByMarkedEvent() {
    Metadata metadata = Context.getMetadata();
    metadata.randomize(true);

    Event event = Context.createEventWithContext(metadata);
    event.addInfo(
        "Layer", "test",
        "Label", "entry");
    event.setAsync();
    event.report(event.metadata, reporter);

    event = Context.createEventWithContext(metadata);
    event.addInfo(
        "Layer", "test",
        "Label", "exit");
    event.setAsync();
    event.report(event.metadata, reporter);

    for (DeserializedEvent deserializedEvent : reporter.getSentEvents()) {
      assertEquals(true, deserializedEvent.getSentEntries().get(Constants.XTR_ASYNC_KEY));
    }
  }

  @Test
  public void testAsyncByMarkedMetadata() {
    Metadata metadata = Context.getMetadata();
    metadata.randomize(true);
    metadata.setIsAsync(true);

    Event event = Context.createEventWithContext(metadata);
    event.addInfo("Layer", "test", "Label", "entry");
    event.report(metadata, reporter);

    event = Context.createEventWithContext(metadata);
    event.addInfo("Layer", "test-nested", "Label", "entry");
    event.report(metadata, reporter);

    event = Context.createEventWithContext(metadata);
    event.addInfo("Layer", "test-nested", "Label", "exit");
    event.report(metadata, reporter);

    event = Context.createEventWithContext(metadata);
    event.addInfo("Layer", "test", "Label", "exit");
    event.report(metadata, reporter);

    event = Context.createEventWithContext(metadata);
    event.addInfo("Layer", "test", "Label", "entry");
    event.report(metadata, reporter);

    event = Context.createEventWithContext(metadata);
    event.addInfo("Layer", "test-nested", "Label", "entry");
    event.report(metadata, reporter);

    event = Context.createEventWithContext(metadata);
    event.addInfo("Layer", "test-nested", "Label", "exit");
    event.report(metadata, reporter);

    event = Context.createEventWithContext(metadata);
    event.addInfo("Layer", "test", "Label", "exit");
    event.report(metadata, reporter);

    List<DeserializedEvent> deserializedEvents = reporter.getSentEvents();
    assertEquals(8, deserializedEvents.size());

    // only the first (0) and the fifth (4)
    for (int i = 0; i < deserializedEvents.size(); i++) {
      if (i == 0 || i == 4) {
        assertEquals(true, deserializedEvents.get(i).getSentEntries().get(Constants.XTR_ASYNC_KEY));
      } else {
        assertNull(deserializedEvents.get(i).getSentEntries().get(XTR_ASYNC_KEY));
      }
    }
  }

  @Test
  public void testW3cContextToXTrace() {
    assertEquals(
        "2BA6A6D97A748BFC9F91A4DC46A0D15BBB00000000B6968E14AC09A25A01",
        EventImpl.w3cContextToXTrace("00-a6a6d97a748bfc9f91a4dc46a0d15bbb-b6968e14ac09a25a-01"));
    assertEquals(
        "2BA6A6D97A748BFC9F91A4DC46A0D15BBB00000000B6968E14AC09A25A00",
        EventImpl.w3cContextToXTrace("00-a6a6d97a748bfc9f91a4dc46a0d15bbb-b6968e14ac09a25a-00"));
  }
}
