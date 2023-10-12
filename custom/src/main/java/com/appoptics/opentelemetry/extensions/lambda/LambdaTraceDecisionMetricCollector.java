package com.appoptics.opentelemetry.extensions.lambda;

import com.google.auto.service.AutoService;
import com.tracelytics.joboe.TraceConfig;
import com.tracelytics.joboe.TraceDecisionUtil;
import com.tracelytics.metrics.measurement.SimpleMeasurementMetricsEntry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.metrics.ObservableLongGauge;
import io.opentelemetry.api.metrics.ObservableLongMeasurement;
import io.opentelemetry.javaagent.extension.AgentListener;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import lombok.Getter;

import java.util.*;

import static com.tracelytics.util.HostTypeDetector.isLambda;


@AutoService(AgentListener.class)
public class LambdaTraceDecisionMetricCollector implements AutoCloseable, AgentListener {
    private final List<ObservableLongGauge> gauges = new LinkedList<>();

    @Getter
    private final ArrayDeque<SimpleMeasurementMetricsEntry> sampleRateQueue = new ArrayDeque<>();

    @Getter
    private final ArrayDeque<SimpleMeasurementMetricsEntry> sampleSourceQueue = new ArrayDeque<>();

    public void collect(Meter meter) {
        gauges.add(meter.gaugeBuilder("dummy-gauge")
                .ofLongs()
                .buildWithCallback(this::consumeTraceConfigs));

        gauges.add(meter.gaugeBuilder("RequestCount")
                .ofLongs()
                .buildWithCallback(observableLongMeasurement ->
                        observableLongMeasurement.record(TraceDecisionUtil.consumeMetricsData(TraceDecisionUtil.MetricType.THROUGHPUT))));

        gauges.add(meter.gaugeBuilder("TokenBucketExhaustionCount")
                .ofLongs()
                .buildWithCallback(observableLongMeasurement ->
                        observableLongMeasurement.record(TraceDecisionUtil.consumeMetricsData(TraceDecisionUtil.MetricType.TOKEN_BUCKET_EXHAUSTION))));

        gauges.add(meter.gaugeBuilder("TraceCount")
                .ofLongs()
                .buildWithCallback(observableLongMeasurement ->
                        observableLongMeasurement.record(TraceDecisionUtil.consumeMetricsData(TraceDecisionUtil.MetricType.TRACE_COUNT))));

        gauges.add(meter.gaugeBuilder("SampleCount")
                .ofLongs()
                .buildWithCallback(observableLongMeasurement ->
                        observableLongMeasurement.record(TraceDecisionUtil.consumeMetricsData(TraceDecisionUtil.MetricType.SAMPLE_COUNT))));

        gauges.add(meter.gaugeBuilder("ThroughTraceCount")
                .ofLongs()
                .buildWithCallback(observableLongMeasurement ->
                        observableLongMeasurement.record(TraceDecisionUtil.consumeMetricsData(TraceDecisionUtil.MetricType.THROUGH_TRACE_COUNT))));

        gauges.add(meter.gaugeBuilder("ThroughIgnoredCount")
                .ofLongs()
                .buildWithCallback(observableLongMeasurement ->
                        observableLongMeasurement.record(TraceDecisionUtil.consumeMetricsData(TraceDecisionUtil.MetricType.THROUGH_IGNORED_COUNT))));

        gauges.add(meter.gaugeBuilder("TriggeredTraceCount")
                .ofLongs()
                .buildWithCallback(observableLongMeasurement ->
                        observableLongMeasurement.record(TraceDecisionUtil.consumeMetricsData(TraceDecisionUtil.MetricType.TRIGGERED_TRACE_COUNT))));

        gauges.add(meter.gaugeBuilder("SampleRate")
                .ofLongs()
                .buildWithCallback(longMeasurement -> record(longMeasurement, sampleRateQueue)));

        gauges.add(meter.gaugeBuilder("SampleSource")
                .ofLongs()
                .buildWithCallback(longMeasurement -> record(longMeasurement, sampleSourceQueue)));
    }

    void record(ObservableLongMeasurement longMeasurement, ArrayDeque<SimpleMeasurementMetricsEntry> queue) {
        while (!queue.isEmpty()) {
            SimpleMeasurementMetricsEntry measurementMetricsEntry = queue.removeLast();
            AttributesBuilder attributesBuilder = Attributes.builder();
            measurementMetricsEntry.getTags()
                    .forEach((key, value) -> attributesBuilder.put(key, (String) value));
            longMeasurement.record((Integer) measurementMetricsEntry.getValue(), attributesBuilder.build());
        }
    }

    void consumeTraceConfigs(ObservableLongMeasurement ignore) {
        Map<String, TraceConfig> layerConfigs = TraceDecisionUtil.consumeLastTraceConfigs();
        Map<Map.Entry<String, Object>, Integer> layerSampleRate = new HashMap<>();
        Map<Map.Entry<String, Object>, Integer> layerSampleSource = new HashMap<>();

        for (Map.Entry<String, TraceConfig> layerConfig : layerConfigs.entrySet()) {
            layerSampleRate.put(new AbstractMap.SimpleEntry<>("layer", layerConfig.getKey()), layerConfig.getValue().getSampleRate());
            layerSampleSource.put(new AbstractMap.SimpleEntry<>("layer", layerConfig.getKey()), layerConfig.getValue().getSampleRateSourceValue());
        }

        convertToMetricsEntries(layerSampleRate, "SampleRate").forEach(sampleRateQueue::addFirst);
        convertToMetricsEntries(layerSampleSource, "SampleSource").forEach(sampleSourceQueue::addFirst);
    }

    private List<SimpleMeasurementMetricsEntry> convertToMetricsEntries(Map<Map.Entry<String, Object>, Integer> data, String keyName) {
        List<SimpleMeasurementMetricsEntry> entries = new ArrayList<>();
        for (Map.Entry<Map.Entry<String, Object>, Integer> metricsEntry : data.entrySet()) {
            Map.Entry<String, Object> singleTag = metricsEntry.getKey();
            Map<String, Object> tags = Collections.singletonMap(singleTag.getKey(), singleTag.getValue());
            entries.add(new SimpleMeasurementMetricsEntry(keyName, tags, metricsEntry.getValue()));
        }
        return entries;
    }

    @Override
    public void close() {
        gauges.forEach(ObservableLongGauge::close);
    }

    @Override
    public void afterAgent(AutoConfiguredOpenTelemetrySdk autoConfiguredOpenTelemetrySdk) {
        if (isLambda()) {
            collect(MeterProvider.getSamplingMetricsMeter());
        }
    }
}
