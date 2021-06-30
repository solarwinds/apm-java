package com.appoptics.api.ext;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;

import com.appoptics.api.ext.Metrics;
import com.tracelytics.monitor.metrics.CustomMetricsCollector;

public class MetricsTest extends BaseTest {
    public MetricsTest() throws Exception {
        super();
    }
    
//    @BeforeClass
//    public void setupClass() throws OboeSettingsException {
//        originalSettings = reader.getSettings();
//        Map<String, Settings> quickRefreshSettings = Collections.<String, Settings>singletonMap(SettingsFetcher.DEFAULT_LAYER, new TestSettingsReader.SettingsMockup((short) 0, 0, Collections.singletonMap(SettingsArg.METRIC_FLUSH_INTERVAL.getKey(), 2)));
//        reader.setAll(quickRefreshSettings);
//        ((SimpleSettingsFetcher) SettingsManager.getFetcher()).fetch(); //force interval update
//    }
//    
//    @AfterClass
//    public void tearDownClass() {
//        reader.setAll(originalSettings);
//        ((SimpleSettingsFetcher) SettingsManager.getFetcher()).fetch(); //force interval update
//    }
    
    
    @After
    @Override
    protected void tearDown() throws Exception {
        CustomMetricsCollector.INSTANCE.reset();
        super.tearDown();
    }
    
    
    public void testIncrementMetrics() throws Exception {
        Metrics.incrementMetric("test-1", null);
        Metrics.incrementMetric("test-1", 2, false, null);
        
        Metrics.incrementMetric("test-1", Collections.singletonMap("key-1", "1"));
        Metrics.incrementMetric("test-1", 2, false, Collections.singletonMap("key-1", "1"));
        Metrics.incrementMetric("test-1", 3, false, Collections.singletonMap("key-1", "1"));
        
        Metrics.incrementMetric("test-1", 2, false, Collections.singletonMap("key-1", "2"));
        
        Map<String, String> tags = new HashMap<String, String>();
        tags.put("key-1", "1");
        tags.put("key-2", "2");
        Metrics.incrementMetric("test-1", 3, false, tags);
        
        Metrics.incrementMetric("test-1", 4, true, tags);
        
        
        Metrics.incrementMetric("test-2", null);
        
        CustomMetricsCollector collector = CustomMetricsCollector.INSTANCE;
        assertEquals(3, (long) collector.getCount("test-1", null));
        assertEquals(6, (long) collector.getCount("test-1", Collections.singletonMap("key-1", "1")));
        assertEquals(2, (long) collector.getCount("test-1", Collections.singletonMap("key-1", "2")));
        assertEquals(3, (long) collector.getCount("test-1", tags));
        
        Map<String, String> tagsWithHost = new HashMap<String, String>(tags);
        tagsWithHost.put(Metrics.HOST_TAG_KEY, "true");
        assertEquals(4, (long) collector.getCount("test-1", tagsWithHost));
        
        assertEquals(1, (long) collector.getCount("test-2", null));
    }
    
    public void testSummaryMetrics() throws Exception {
        Metrics.summaryMetric("test-1", 1, null);
        Metrics.summaryMetric("test-1", 1, 2, false, null);
        
        Metrics.summaryMetric("test-1", 1, Collections.singletonMap("key-1", "1"));
        Metrics.summaryMetric("test-1", 1, 2, false, Collections.singletonMap("key-1", "1"));
        Metrics.summaryMetric("test-1", 1, 3, false, Collections.singletonMap("key-1", "1"));
        
        Metrics.summaryMetric("test-1", 1, 2, false, Collections.singletonMap("key-1", "2"));
        
        Map<String, String> tags = new HashMap<String, String>();
        tags.put("key-1", "1");
        tags.put("key-2", "2");
        Metrics.summaryMetric("test-1", 3, tags);
        Metrics.summaryMetric("test-1", 5.5, 2, true, tags);
        
        Metrics.summaryMetric("test-2", 4, null);
        
        CustomMetricsCollector collector = CustomMetricsCollector.INSTANCE;
        assertEquals(2.0, collector.getSum("test-1", null));
        assertEquals(3, (long) collector.getCount("test-1", null));
        assertEquals(3.0, collector.getSum("test-1", Collections.singletonMap("key-1", "1")));
        assertEquals(6, (long) collector.getCount("test-1", Collections.singletonMap("key-1", "1")));
        assertEquals(1.0, collector.getSum("test-1", Collections.singletonMap("key-1", "2")));
        assertEquals(2, (long) collector.getCount("test-1", Collections.singletonMap("key-1", "2")));
        assertEquals(3.0, collector.getSum("test-1", tags));
        assertEquals(1, (long) collector.getCount("test-1", tags));
        Map<String, String> tagsWithHost = new HashMap<String, String>(tags);
        tagsWithHost.put(Metrics.HOST_TAG_KEY, "true");
        assertEquals(5.5, collector.getSum("test-1", tagsWithHost));
        assertEquals(2, (long) collector.getCount("test-1", tagsWithHost));
        assertEquals(4.0, collector.getSum("test-2", null));
        assertEquals(1, (long) collector.getCount("test-2", null));
    }
}
