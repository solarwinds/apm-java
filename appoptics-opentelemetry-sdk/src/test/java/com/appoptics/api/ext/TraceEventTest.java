package com.appoptics.api.ext;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.appoptics.api.ext.Trace;
import com.appoptics.api.ext.TraceEvent;
import com.appoptics.api.ext.model.NoOpEvent;
import com.tracelytics.ext.ebson.MultiValList;
import com.tracelytics.joboe.Constants;
import com.tracelytics.joboe.Context;
import com.tracelytics.joboe.Metadata;
import com.tracelytics.joboe.OboeException;
import com.tracelytics.joboe.TracingMode;
import com.tracelytics.joboe.TestReporter.DeserializedEvent;
import com.tracelytics.joboe.settings.SettingsManager;
import com.tracelytics.joboe.settings.SimpleSettingsFetcher;
import com.tracelytics.joboe.settings.TestSettingsReader.SettingsMockup;
import com.tracelytics.joboe.settings.TestSettingsReader.SettingsMockupBuilder;

public class TraceEventTest extends BaseTest {

    public TraceEventTest() throws Exception {
        super();
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }
    
    
    public void testSampledEvent() throws OboeException {
        reader.put(new SettingsMockupBuilder().withFlags(TracingMode.ALWAYS).withSampleRate(1000000).build()); //ALWAYS sample rate = 100%
        Trace.startTrace("sampled").report();
        Metadata startContext = Context.getMetadata();
        String startEdge = startContext.opHexString();
        reporter.reset();
        
        TraceEvent tracedEvent;
        tracedEvent = Trace.createEntryEvent("entry-event");
        tracedEvent.addBackTrace();
        
        Metadata generatedContext = new Metadata(startContext); //create a clone so next operation would not modify the existing metadata
        generatedContext.randomizeOpID();
        
        tracedEvent.addEdge(generatedContext.toHexString());
        tracedEvent.addInfo(Collections.<String, Object>singletonMap("key1", 1));
        tracedEvent.addInfo("key2", "test", "key3", 3.0);
        tracedEvent.addInfo("key4", 4L);
        tracedEvent.setAsync();
        tracedEvent.report();
        
        List<DeserializedEvent> sentEvents = reporter.getSentEvents();
        assertEquals(1, sentEvents.size());
        
        Map<String, Object> sentEntries = sentEvents.get(0).getSentEntries();
        assertEquals("entry", sentEntries.get("Label"));
        assertEquals("entry-event", sentEntries.get("Layer"));
        assertTrue(sentEntries.containsKey("Backtrace"));
        MultiValList<String> expectedEdges = new MultiValList<String>();
        expectedEdges.add(startEdge);
        expectedEdges.add(generatedContext.opHexString());
        assertEquals(expectedEdges, sentEntries.get(Constants.XTR_EDGE_KEY));
        assertEquals(1, sentEntries.get("key1"));
        assertEquals("test", sentEntries.get("key2"));
        assertEquals(3.0, sentEntries.get("key3"));
        assertEquals(4L, sentEntries.get("key4"));
        assertEquals(true, sentEntries.get(Constants.XTR_ASYNC_KEY));
    }
    
    public void testNotSampledEvent() throws OboeException {
        reader.put(new SettingsMockupBuilder().withFlags(TracingMode.NEVER).withSampleRate(0).build()); //NEVER sample rate = 0%
        Trace.startTrace("not-sampled").report();
        TraceEvent noopEvent;
        noopEvent = Trace.createEntryEvent("entry-event");
        
        assertTrue("Unexpected event " + noopEvent.getClass().getName() + " test reader: " + System.identityHashCode(reader) + " settings fetcher " + SettingsManager.getFetcher() + " settings reader: " + System.identityHashCode(((SimpleSettingsFetcher) SettingsManager.getFetcher()).getReader()), noopEvent instanceof NoOpEvent);
        assertEquals(0, reporter.getSentEvents().size());
    }
}
