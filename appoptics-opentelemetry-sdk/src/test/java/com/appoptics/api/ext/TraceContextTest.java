package com.appoptics.api.ext;

import com.appoptics.api.ext.Trace;
import com.appoptics.api.ext.TraceContext;
import com.tracelytics.joboe.Context;
import com.tracelytics.joboe.OboeException;
import com.tracelytics.joboe.TracingMode;
import com.tracelytics.joboe.settings.TestSettingsReader.SettingsMockupBuilder;

public class TraceContextTest extends BaseTest {

    public TraceContextTest() throws Exception {
        super();
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        
    }
    
    public void testEmpty() throws OboeException {
        TraceContext emptyContext = TraceContext.getDefault(); //not valid, not sampled
        
        Context.getMetadata().randomize(); //randomize current context to a valid context
        
        emptyContext.setAsDefault(); //now set the emptyContext back
        assertFalse(Context.getMetadata().isValid()); //empty context is not valid
        assertFalse(Context.getMetadata().isSampled()); //empty context is not sampled
        
        assertFalse(TraceContext.isSampled(Trace.getCurrentXTraceID()));
    }
    
    public void testSampled() {
        reader.put(new SettingsMockupBuilder().withFlags(TracingMode.ALWAYS).withSampleRate(1000000).build()); //ALWAYS sample rate = 100%
        Trace.startTrace("sampled").report();
        TraceContext sampledContext = TraceContext.getDefault();
        
        TraceContext.clearDefault(); //clear the current context
        assertFalse(Context.getMetadata().isValid());
        assertFalse(Context.getMetadata().isSampled());
        
        sampledContext.setAsDefault(); //now set back to sampledContext
        assertTrue(Context.getMetadata().isValid());
        assertTrue(Context.getMetadata().isSampled());
        
        assertTrue(TraceContext.isSampled(Trace.getCurrentXTraceID()));
    }
    
    
    
    public void testNotSampled() throws OboeException {
        reader.put(new SettingsMockupBuilder().withFlags(TracingMode.NEVER).withSampleRate(0).build()); //NEVER sample rate = 0%
        Trace.startTrace("not-sampled").report();
        TraceContext notSampledContext = TraceContext.getDefault();
        
        TraceContext.clearDefault(); //clear the current context
        assertFalse(Context.getMetadata().isValid());
        assertFalse(Context.getMetadata().isSampled());
        
        notSampledContext.setAsDefault(); //now set back to notSampledContext
        assertTrue(Context.getMetadata().isValid()); //it is valid as it has gone through the sampling deicison
        assertFalse(Context.getMetadata().isSampled()); //it is not sampled
        
        assertFalse(TraceContext.isSampled(Trace.getCurrentXTraceID()));
    }
}
