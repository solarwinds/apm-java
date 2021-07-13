package com.appoptics.opentelemetry.extensions;

import com.appoptics.opentelemetry.core.Constants;
import com.tracelytics.joboe.span.impl.MetricSpanReporter;
import com.tracelytics.joboe.span.impl.Span;
import com.tracelytics.metrics.MetricKey;
import com.tracelytics.metrics.MetricsEntry;
import com.tracelytics.metrics.histogram.Histogram;
import com.tracelytics.metrics.histogram.HistogramException;
import com.tracelytics.metrics.histogram.HistogramFactory;
import com.tracelytics.metrics.histogram.HistogramMetricsEntry;
import com.tracelytics.metrics.measurement.SummaryLongMeasurement;
import com.tracelytics.metrics.measurement.SummaryMeasurementMetricsEntry;
import com.tracelytics.monitor.metrics.SpanMetricsCollector;
import com.tracelytics.util.HttpUtils;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Span processor to record inbound metrics
 */
public class AppOpticsInboundMetricsSpanProcessor implements SpanProcessor {
    private static final AttributeKey<Boolean> AO_METRICS_KEY = AttributeKey.booleanKey(Constants.AO_METRICS);
    public static final OpenTelemetryInboundMeasurementReporter measurementReporter = new OpenTelemetryInboundMeasurementReporter();
    public static final OpenTelemetryInboundHistogramReporter histogramReporter = new OpenTelemetryInboundHistogramReporter();

    public static SpanMetricsCollector buildSpanMetricsCollector() {
        return new SpanMetricsCollector(measurementReporter, histogramReporter);
    }

    @Override
    public void onStart(Context parentContext, ReadWriteSpan span) {

    }

    @Override
    public boolean isStartRequired() {
        return false;
    }

    @Override
    public void onEnd(ReadableSpan span) {
        SpanContext parentSpanContext = span.toSpanData().getParentSpanContext();
        if (!parentSpanContext.isValid() || parentSpanContext.isRemote()) { //then a root span of this service
            SpanData spanData = span.toSpanData();
            if (spanData.getAttributes().get(AO_METRICS_KEY)) { //this sometimes cause serious problem if NPE is throw. too expensive? We don't really have to check as we always do inbound right now
                measurementReporter.reportMetrics(spanData);
                histogramReporter.reportMetrics(spanData);
            }
        }
    }

    @Override
    public boolean isEndRequired() {
        return true;
    }

    private static class OpenTelemetryInboundMeasurementReporter extends MetricSpanReporter {
        public static final String measurementName = "TransactionResponseTime";
        private Map<MetricKey, SummaryLongMeasurement> measurements = new ConcurrentHashMap<MetricKey, SummaryLongMeasurement>(); //cannot use Guava cache here, jboss issue...
        //private final String measurementName;

        protected OpenTelemetryInboundMeasurementReporter() {
//            super(TRANSACTION_LATENCY_METRIC_NAME);
        }

        @Override
        protected void reportMetrics(com.tracelytics.joboe.span.impl.Span span, long l) {
            //not using the signature that takes OpenTracing span
        }

        @Override
        public List<MetricsEntry<?>> consumeMetricEntries() {
            Map<MetricKey, SummaryLongMeasurement> reportingMeasurements = consumeMeasurements();

            List<MetricsEntry<?>> entries = new ArrayList<MetricsEntry<?>>();

            for (Map.Entry<MetricKey, SummaryLongMeasurement> entry : reportingMeasurements.entrySet()) {
                entries.add(new SummaryMeasurementMetricsEntry(entry.getKey(), entry.getValue()));
            }

            return entries;
        }

        public Map<MetricKey, SummaryLongMeasurement> consumeMeasurements() {
            //TODO concurrency
            Map<MetricKey, SummaryLongMeasurement> consumedMeasurements = new HashMap<>(measurements);
            this.measurements.clear();
            return consumedMeasurements;
        }


        private void reportMetrics(SpanData spanData) {
            String transactionName = TransactionNameManager.getTransactionName(spanData);

            Map<String, String> primaryKeys = Collections.singletonMap("TransactionName", transactionName);
            //boolean hasError = spanData.getAttributes() TODO
            boolean hasError = false;

            Map<String, String> optionalKeys = new HashMap<String, String>();

            Long status = spanData.getAttributes().get(SemanticAttributes.HTTP_STATUS_CODE);
            //special handling for status code
            if (!hasError && status != null) {
                hasError = HttpUtils.isServerErrorStatusCode(status.intValue()); //do not attempt to override the property if it's already explicitly set
            }

            if (status != null) {
                optionalKeys.put("HttpStatus", String.valueOf(status));
            }

            String method = (String) spanData.getAttributes().get(SemanticAttributes.HTTP_METHOD);
            if (method != null) {
                optionalKeys.put("HttpMethod", method);
            }


            if (hasError) {
                optionalKeys.put("Errors", "true");
            }

//        optionalKeys.putAll(span.getSpanPropertyValue(SpanProperty.METRIC_TAGS));

            long duration = (spanData.getEndEpochNanos() - spanData.getStartEpochNanos()) / 1000;
            recordMeasurementEntry(primaryKeys, optionalKeys, duration);
        }

        protected void recordMeasurementEntry(Map<String, String> primaryKeys, Map<String, String> optionalKeys, long duration) {
            MetricKey measurementKey = new MetricKey(this.measurementName, new HashMap(primaryKeys));
            this.measurements.computeIfAbsent(measurementKey, k -> new SummaryLongMeasurement()).recordValue(duration);
            if (optionalKeys != null) {
                Iterator iterator = optionalKeys.entrySet().iterator();

                while(iterator.hasNext()) {
                    Map.Entry<String, String> optionalKey = (Map.Entry)iterator.next();
                    Map<String, String> tags = new HashMap(primaryKeys);
                    tags.put((String)optionalKey.getKey(), (String)optionalKey.getValue());
                    measurementKey = new MetricKey(this.measurementName, tags);
                    this.measurements.computeIfAbsent(measurementKey, k -> new SummaryLongMeasurement()).recordValue(duration);
                }
            }

        }
    }

    //TODO copied from Core, should improve to avoid code duplication
    private static class OpenTelemetryInboundHistogramReporter extends MetricSpanReporter {
        public static final String TRANSACTION_LATENCY_METRIC_NAME = "TransactionResponseTime";
        public static final String TRANSACTION_NAME_TAG_KEY = "TransactionName";
        private static final HistogramFactory.HistogramType HISTOGRAM_TYPE = HistogramFactory.HistogramType.HDR;

        private Map<MetricKey, Histogram> histograms = new ConcurrentHashMap<>();

        private OpenTelemetryInboundHistogramReporter() {

        }

//        private static LoadingCache<MetricKey, Histogram> createHistogramCache() {
//            return CacheBuilder.newBuilder().build(new CacheLoader<MetricKey, Histogram> () {
//                @Override
//                public Histogram load(MetricKey key) throws Exception {
//                    return HistogramFactory.buildHistogram(HISTOGRAM_TYPE, MAX_DURATION);
//                }
//            });
//        }

        /**
         * Records the span duration as a histogram and a measurement
         */
        protected void reportMetrics(Span span, long duration) {
            //not used
        }

        /**
         * Consumes and resets metric entries on this reporter
         * @return  a list of metric entries collected so since previous call to this method
         */
        public List<MetricsEntry<?>> consumeMetricEntries() {
            Map<MetricKey, Histogram> reportingHistograms = consumeHistograms();

            List<MetricsEntry<?>> entries = new ArrayList<MetricsEntry<?>>();
            for (Map.Entry<MetricKey, Histogram> entry : reportingHistograms.entrySet()) {
                entries.add(new HistogramMetricsEntry(entry.getKey(), entry.getValue()));
            }

            return entries;
        }

        public Map<MetricKey, Histogram> consumeHistograms() {
            Map<MetricKey, Histogram> reportingHistograms = new HashMap<>(histograms);
            histograms.clear();

            return reportingHistograms;
        }

        public void reportMetrics(SpanData spanData) {
            MetricKey serviceHistogramKey = new MetricKey(TRANSACTION_LATENCY_METRIC_NAME, null); //globally for all transactions within this service
            Histogram serviceHistogram;
            serviceHistogram = histograms.computeIfAbsent(serviceHistogramKey, k -> HistogramFactory.buildHistogram(HISTOGRAM_TYPE, MAX_DURATION));

            long duration = (spanData.getEndEpochNanos() - spanData.getStartEpochNanos()) / 1000;
            try {
                serviceHistogram.recordValue(duration);
            } catch (HistogramException e) {
                logger.debug("Failed to report metrics to service level histogram : " + e.getMessage(), e);
            }

            String transactionName = TransactionNameManager.getTransactionName(spanData);

            if (transactionName != null) {
                MetricKey transactionHistogramKey = new MetricKey(TRANSACTION_LATENCY_METRIC_NAME, Collections.singletonMap("TransactionName", transactionName)); //specifically for this transaction
                Histogram transactionHistogram = histograms.computeIfAbsent(transactionHistogramKey, k -> HistogramFactory.buildHistogram(HISTOGRAM_TYPE, MAX_DURATION));
                try {
                    transactionHistogram.recordValue(duration);
                } catch (HistogramException e) {
                    logger.debug("Failed to report metrics to transaction histogram with metrics key [" + transactionHistogramKey + "] : " + e.getMessage(), e);
                }
            }
        }
    }



}
