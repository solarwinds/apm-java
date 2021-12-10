package com.appoptics.api.ext;

import com.tracelytics.joboe.Context;
//import com.tracelytics.joboe.StartupManager;
import com.tracelytics.joboe.TestReporter;
import com.tracelytics.joboe.settings.TestSettingsReader;
import com.tracelytics.joboe.span.impl.ScopeManager;

import junit.framework.TestCase;

public abstract class BaseTest extends TestCase {
    protected final TestReporter reporter;
    protected final TestSettingsReader reader;
    
    protected BaseTest() throws Exception {
  //      StartupManager.flagTestingMode();
        
        AgentChecker.setIsExtensionAvailable(true); //set to true for testing
        //TestingEnv testingEnv = (TestingEnv) StartupManager.isAgentReady().get(10, TimeUnit.SECONDS);
//        reporter = testingEnv.getTracingReporter();
//        reader = testingEnv.getSettingsReader();
        //TODO
        reporter = null;
        reader = null;
    }
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }
    
    @Override
    protected void tearDown() throws Exception {
        reader.reset();
        reporter.reset();
        Context.clearMetadata();
        ScopeManager.INSTANCE.removeAllScopes();
        super.tearDown();
    }
}
