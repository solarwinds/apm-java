package com.appoptics.api.ext;

import java.util.HashMap;
import java.util.Map;

import com.tracelytics.monitor.metrics.CustomMetricsCollector;

/**
 * API for recording custom metrics. Recorded metrics will be aggregated and submitted periodically
 *
 */
// TODO remove the custom metrics API?
class Metrics {
    public static final String HOST_TAG_KEY = "HostTag";

    /**
     * Records a Summary Metric which consists of a single value.
     *
     * Summary metrics will be aggregated by the key- combination of metric name and tags,
     * with a sum (sum of value from each call with same key), and a count (sum of count from each call with same key, with by default is 1 per call).
     *
     * @param name  a custom name for this Summary Metric
     * @param value a double value for this Summary Metric
     * @param tags  a Map of custom tags for this Summary Metric
     */
    public static void summaryMetric(String name, double value, Map<String, String> tags) {
        summaryMetric(name, value, 1, false, tags);
    }

    /**
     * Records a Summary Metric which consists of a single value and a count.
     *
     * Summary metrics will be aggregated by the key- combination of metric name and tags,
     * with a sum (sum of value from each call with same key), and a count (sum of count from each call with same key).
     *
     * This is a more efficient method to record metrics with same key.
     *
     * For example, if a Summary Metric is to be recorded for an operation performed repeatedly,
     * then a total duration can be first accumulated with a count and passed to this method once instead of calling this repeatedly for each operation.
     *
     * @param name          a custom name for this Summary Metric
     * @param value         a double value for this Summary Metric, take note that this should be an accumulative value if count is greater than 1
     * @param count         a count of measured operations that produce the accumulative value
     * @param addHostTag    whether tag the Summary Metric with host information
     * @param tags          a Map of custom tags for this Summary Metric
     */
    public static void summaryMetric(String name, double value, int count, boolean addHostTag, Map<String, String> tags) {
        if (AgentChecker.isExtensionAvailable()) {
            if (addHostTag) {
                tags = tags != null ? new HashMap<String, String>(tags) : new HashMap<String, String>(); //input tags might not be mutable, use clone
                tags.put(HOST_TAG_KEY, "true");
            }
            CustomMetricsCollector.INSTANCE.recordSummaryMetric(name, value, count, tags);
        }
    }

    /**
     * Records a Count Metrics that increments by 1
     *
     * Count metrics will be aggregated by the key- combination of metric name and tags,
     * with a count (sum of count from each call with same key, default as 1).
     *
     * @param name  a custom name for this Count Metric
     * @param tags  a Map of custom tags for Count Metric
     */
    public static void incrementMetric(String name, Map<String, String> tags) {
        incrementMetric(name, 1, false, tags);
    }

    /**
     * Records a Count metrics that increments by the count parameter
     *
     * Count metrics will be aggregated by the key- combination of metric name and tags,
     * with a count (sum of count from each call with same key).
     *
     * This is a more efficient method to record metrics with same key.
     *
     * For example, if a Count Metric is to be recorded for an operation performed repeatedly,
     * then an accumulated count can be passed to this method once instead of calling this repeatedly for each operation.
     *
     * @param name          a custom name for this Count Metric
     * @param count         a accumulative count of operations
     * @param addHostTag    whether tag the Count Metric with host information
     * @param tags          a Map of custom tags for Count Metric
     */
    public static void incrementMetric(String name, int count, boolean addHostTag, Map<String, String> tags) {
        if (AgentChecker.isExtensionAvailable()) {
            if (addHostTag) {
                tags = tags != null ? new HashMap<String, String>(tags) : new HashMap<String, String>(); //input tags might not be mutable, use clone
                tags.put(HOST_TAG_KEY, "true");
            }
            CustomMetricsCollector.INSTANCE.recordIncrementMetrics(name, count, tags);
        }
    }
}
