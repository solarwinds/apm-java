package com.appoptics.api.ext;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.tracelytics.joboe.Constants;
import com.tracelytics.joboe.Context;
import com.tracelytics.joboe.Metadata;
import com.tracelytics.joboe.OboeException;
import com.tracelytics.joboe.TracingMode;
import com.tracelytics.joboe.TestReporter.DeserializedEvent;
import com.tracelytics.joboe.settings.TestSettingsReader.SettingsMockupBuilder;
import com.tracelytics.joboe.settings.TestSettingsReader.SettingsMockup;
import com.tracelytics.joboe.span.impl.ScopeManager;
import com.tracelytics.joboe.span.impl.Span;
import com.tracelytics.joboe.span.impl.TransactionNameManager;

public class TraceTest extends BaseTest {

    public TraceTest() throws Exception {
        super();
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }
    
    
    public void testSampledTrace() throws OboeException {
        reader.put(new SettingsMockupBuilder().withFlags(TracingMode.ALWAYS).withSampleRate(1000000).build()); //ALWAYS sample rate = 100%
        Trace.startTrace("sampled").report();
        //assert that there's an active span
        Span currentSpan = ScopeManager.INSTANCE.activeSpan();
        assertEquals("sampled", currentSpan.getOperationName());
        assertEquals(true, currentSpan.isRoot());
        assertEquals(true, currentSpan.context().isSampled());
        
        Trace.createEntryEvent("child").report();
        Trace.createExitEvent("child").report();
        Trace.createInfoEvent("sampled").report();
        Trace.logException(new Throwable() {});
        Metadata reportedMetadata = new Metadata(Trace.endTrace("sampled"));
        assertTrue(reportedMetadata.isValid());
        assertTrue(reportedMetadata.isSampled());
        assertEquals(null, ScopeManager.INSTANCE.activeSpan());
        
        List<DeserializedEvent> sentEvents = reporter.getSentEvents();
        
        assertEquals(6, sentEvents.size());
    }
    
    public void testNotSampledTrace() throws OboeException {
        reader.put(new SettingsMockupBuilder().withFlags(TracingMode.NEVER).withSampleRate(0).build()); //NEVER sample rate = 0%
        Trace.startTrace("not-sampled").report();
        //assert that there's an active span (not sampled)
        Span currentSpan = ScopeManager.INSTANCE.activeSpan();
        assertEquals("not-sampled", currentSpan.getOperationName());
        assertEquals(true, currentSpan.isRoot());
        assertEquals(false, currentSpan.context().isSampled());
        
        Trace.createEntryEvent("child").report();
        Trace.createExitEvent("child").report();
        Trace.createInfoEvent("not-sampled").report();
        Trace.logException(new Throwable() {});
        Metadata reportedMetadata = new Metadata(Trace.endTrace("not-sampled"));
        assertTrue(reportedMetadata.isValid());
        assertFalse(reportedMetadata.isSampled());
        assertEquals(null, ScopeManager.INSTANCE.activeSpan());
        
        List<DeserializedEvent> sentEvents = reporter.getSentEvents();
        assertEquals(0, sentEvents.size());
    }
    
    public void testContinueTraceSampledId() throws OboeException {
        reader.put(new SettingsMockupBuilder().withFlags(TracingMode.ALWAYS).withSampleRate(1000000).build()); //ALWAYS sample rate = 100%
        Metadata testMetadata = getMetadata(true); 
        String sampledId = testMetadata.toHexString(); 
        Trace.continueTrace("simple", sampledId).report();
        //assert that there's an active span
        Span currentSpan = ScopeManager.INSTANCE.activeSpan();
        assertEquals("simple", currentSpan.getOperationName());
        assertEquals(true, currentSpan.isRoot());
        assertEquals(true, currentSpan.context().isSampled());
        
        List<DeserializedEvent> sentEvents = reporter.getSentEvents();
        assertEquals(1, sentEvents.size());
        DeserializedEvent continueEvent = sentEvents.get(0);
      //make sure there's an edge pointing at the incoming x-trace ID
        assertEquals(testMetadata.opHexString(), continueEvent.getSentEntries().get(Constants.XTR_EDGE_KEY)); 
        
        Metadata metadata = Context.getMetadata();
        assertTrue(metadata.isTaskEqual(testMetadata));
        assertFalse(metadata.isOpEqual(testMetadata));
        assertTrue(metadata.isValid());
        assertTrue(metadata.isSampled());
    }
    
    public void testContinueTraceIncompatibleId() throws OboeException {
        reader.put(new SettingsMockupBuilder().withFlags(TracingMode.ALWAYS).withSampleRate(1000000).build()); //ALWAYS sample rate = 100%
        Metadata testMetadata = getMetadata(true);
        String incompatibleId = testMetadata.toHexString(Metadata.CURRENT_VERSION + 1); //make this one version ahead, hence incompatible  
        Trace.continueTrace("simple", incompatibleId).report(); //it should just ignore this x-trace id and start a new trace
        //assert that there's an active span
        Span currentSpan = ScopeManager.INSTANCE.activeSpan();
        assertEquals("simple", currentSpan.getOperationName());
        assertEquals(true, currentSpan.isRoot());
        assertEquals(true, currentSpan.context().isSampled());
        
        List<DeserializedEvent> sentEvents = reporter.getSentEvents();
        assertEquals(1, sentEvents.size());
        
        Metadata metadata = Context.getMetadata();
        assertFalse(metadata.isTaskEqual(testMetadata)); //should not be the same task as the incoming x-trace id is ignored
        assertFalse(metadata.isOpEqual(testMetadata));
        assertTrue(metadata.isValid());
        assertTrue(metadata.isSampled());
    }
    
    public void testContinueTraceNotSampledId() throws OboeException {
        reader.put(new SettingsMockupBuilder().withFlags(TracingMode.ALWAYS).withSampleRate(1000000).build()); //ALWAYS sample rate = 100%
        Metadata testMetadata = getMetadata(false); 
        String notSampledId = testMetadata.toHexString();
        Trace.continueTrace("simple", notSampledId).report(); //it should not continue trace as incoming x-trace is not-sampled
        //assert that there's an active span
        Span currentSpan = ScopeManager.INSTANCE.activeSpan();
        assertEquals("simple", currentSpan.getOperationName());
        assertEquals(true, currentSpan.isRoot());
        assertEquals(false, currentSpan.context().isSampled());
        
        List<DeserializedEvent> sentEvents = reporter.getSentEvents();
        assertEquals(0, sentEvents.size());
        
        Metadata metadata = Context.getMetadata();
        assertTrue(metadata.isTaskEqual(testMetadata)); //task id should be equal as it should be propagated
        assertTrue(metadata.isValid()); //it is a valid metadata just not sampled
        assertFalse(metadata.isSampled()); //not sampled
        
        assertEquals(notSampledId, Trace.getCurrentXTraceID());
    }
    
    public void testContinueTraceNeverMode() throws OboeException {
        reader.put(new SettingsMockupBuilder().withFlags(TracingMode.NEVER).withSampleRate(0).build()); //NEVER sample rate = 0%
        Metadata testMetadata = getMetadata(true); 
        String sampledId = testMetadata.toHexString(); 
        Trace.continueTrace("not-sampled", sampledId).report(); //it should not continue trace this layer has trace mode never
        //assert that there's an active span
        Span currentSpan = ScopeManager.INSTANCE.activeSpan();
        assertEquals("not-sampled", currentSpan.getOperationName());
        assertEquals(true, currentSpan.isRoot());
        assertEquals(false, currentSpan.context().isSampled());
        
        List<DeserializedEvent> sentEvents = reporter.getSentEvents();
        assertEquals(0, sentEvents.size());
        
        Metadata metadata = Context.getMetadata();
        assertTrue(metadata.isTaskEqual(testMetadata)); //task id should be equal as it should be propagated
        assertTrue(metadata.isValid()); //it is a valid metadata just not sampled
        assertFalse(metadata.isSampled()); //not sampled
    }
    
    public void testEmptyContext() {
        assertEquals("", Trace.endTrace("test")); //no valid context
    }
    
    public void testSetTransactionName() throws Exception {
        reader.put(new SettingsMockupBuilder().withFlags(TracingMode.ALWAYS).withSampleRate(1000000).build()); //ALWAYS sample rate = 100%
        
        assertEquals(false, Trace.setTransactionName("test-1")); //cannot set transaction name as there's no valid context
        
        reporter.reset();
        Trace.startTrace("test-layer").report();
        assertEquals(true, Trace.setTransactionName("test-2")); //ok, there's an active trace
        Trace.endTrace("test-layer");
        assertEquals("test-2", reporter.getSentEvents().get(1).getSentEntries().get("TransactionName"));
                
        reporter.reset();
        Trace.startTrace("test-layer").report();
        assertEquals(true, Trace.setTransactionName("test@123#$%")); //ok, there's an active trace 
        Trace.endTrace("test-layer");
        assertEquals("test_123___", reporter.getSentEvents().get(1).getSentEntries().get("TransactionName")); //transformed transaction name with invalid characters replaced with _
        
        reporter.reset();
        Trace.startTrace("test-layer").report();
        String longString = "0123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789";
        assertEquals(true, Trace.setTransactionName(longString)); //ok, there's an active trace
        Trace.endTrace("test-layer");
        assertEquals(longString.substring(0, TransactionNameManager.MAX_TRANSACTION_NAME_LENGTH - TransactionNameManager.TRANSACTION_NAME_ELLIPSIS.length()) + TransactionNameManager.TRANSACTION_NAME_ELLIPSIS, reporter.getSentEvents().get(1).getSentEntries().get("TransactionName")); //truncated transaction name
        
        reporter.reset();
        Trace.startTrace("test-layer").report();
        assertEquals(true, Trace.setTransactionName("  space  ")); //ok, there's an active trace
        Trace.endTrace("test-layer");
        assertEquals("  space  ", reporter.getSentEvents().get(1).getSentEntries().get("TransactionName")); //empty space should not be replaced
        
        reporter.reset();
        Trace.startTrace("test-layer").report();
        assertEquals(true, Trace.setTransactionName("-.:_\\\\/?")); //ok, there's an active trace
        Trace.endTrace("test-layer");
        assertEquals("-.:_\\\\/?", reporter.getSentEvents().get(1).getSentEntries().get("TransactionName")); //no transformation as all those are valid characters
        
        reporter.reset();
        Trace.startTrace("test-layer").report();
        assertEquals(false, Trace.setTransactionName(null)); //not ok, transaction name should not be null
        Trace.endTrace("test-layer");
        assertEquals(TransactionNameManager.DEFAULT_SDK_TRANSACTION_NAME_PREFIX + "test-layer", reporter.getSentEvents().get(1).getSentEntries().get("TransactionName")); //default name for SDK traces
        
        reporter.reset();
        Trace.startTrace("test-layer").report();
        assertEquals(false, Trace.setTransactionName("")); //not ok, transaction name should not be empty
        Trace.endTrace("test-layer");
        assertEquals(TransactionNameManager.DEFAULT_SDK_TRANSACTION_NAME_PREFIX + "test-layer", reporter.getSentEvents().get(1).getSentEntries().get("TransactionName")); //default name for SDK traces
        
        reporter.reset();
        Trace.startTrace("test-layer").report();
        Future<?> future = Executors.newSingleThreadExecutor().submit(new Runnable() {
            @Override
            public void run() {
                assertEquals(true, Trace.setTransactionName("test-4")); //ok, there's an active trace, transaction name set in spawned thread should be reflected in the active trace 
            }
        });
        future.get();
        Trace.endTrace("test-layer");
        assertEquals("test-4", reporter.getSentEvents().get(1).getSentEntries().get("TransactionName"));
        
        //test multiple transaction name on same trace
        reporter.reset();
        Trace.startTrace("test-layer").report();
        assertEquals(true, Trace.setTransactionName("name-1"));
        assertEquals(true, Trace.setTransactionName("name-2"));
        Trace.endTrace("test-layer");
        assertEquals("name-2", reporter.getSentEvents().get(1).getSentEntries().get("TransactionName")); //should use the last value
        
        //test transaction name precedence
        reporter.reset();
        Trace.startTrace("test-layer").report();
        ScopeManager.INSTANCE.activeSpan().setTag("URL", "/1/2/3"); //cheat a bit by calling core code directly...as we currently do not expose tags via SDK
        Trace.endTrace("test-layer");
        assertEquals("/1/2", reporter.getSentEvents().get(1).getSentEntries().get("TransactionName")); //no custom transaction name, should just return the URL based transaction name
        
        reporter.reset();
        Trace.startTrace("test-layer").report();
        assertEquals(true, Trace.setTransactionName("my-transaction"));
        ScopeManager.INSTANCE.activeSpan().setTag("URL", "/1/2/3"); //cheat a bit by calling core code directly...as we currently do not expose tags via SDK
        Trace.endTrace("test-layer");
        assertEquals("my-transaction", reporter.getSentEvents().get(1).getSentEntries().get("TransactionName")); //custom transaction name should override the url based name
    }
    
    
    private static Metadata getMetadata(boolean sampled) {
        Metadata metadata = new Metadata();
        metadata.randomize(sampled);
        
        return metadata;
    }
}
