package com.appoptics.opentelemetry.extensions;

import com.appoptics.opentelemetry.core.Constants;
import com.solarwinds.joboe.config.ConfigManager;
import com.solarwinds.joboe.config.ConfigProperty;
import com.solarwinds.joboe.span.impl.MetricSpanReporter;
import com.solarwinds.joboe.span.impl.Span;
import com.solarwinds.metrics.MetricKey;
import com.solarwinds.metrics.MetricsEntry;
import com.solarwinds.metrics.histogram.Histogram;
import com.solarwinds.metrics.histogram.HistogramException;
import com.solarwinds.metrics.histogram.HistogramFactory;
import com.solarwinds.metrics.histogram.HistogramMetricsEntry;
import com.solarwinds.metrics.measurement.SummaryLongMeasurement;
import com.solarwinds.metrics.measurement.SummaryMeasurementMetricsEntry;
import com.solarwinds.monitor.SpanMetricsCollector;
import com.solarwinds.shaded.javax.annotation.Nonnull;
import com.solarwinds.util.HttpUtils;
import com.solarwinds.util.ServiceKeyUtils;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.semconv.SemanticAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Span processor to record inbound metrics */
public class AppOpticsInboundMetricsSpanProcessor implements SpanProcessor {
  private static final AttributeKey<Boolean> AO_METRICS_KEY =
      AttributeKey.booleanKey(Constants.SW_METRICS);
  public static final OpenTelemetryInboundMeasurementReporter MEASUREMENT_REPORTER =
      new OpenTelemetryInboundMeasurementReporter();
  public static final OpenTelemetryInboundHistogramReporter HISTOGRAM_REPORTER =
      new OpenTelemetryInboundHistogramReporter();

  public static final String serviceName;

  static {
    serviceName =
        ServiceKeyUtils.getServiceName(
            (String) ConfigManager.getConfig(ConfigProperty.AGENT_SERVICE_KEY));
  }

  public static SpanMetricsCollector buildSpanMetricsCollector() {
    SpanMetricsCollector spanMetricsCollector =
        new SpanMetricsCollector(MEASUREMENT_REPORTER, HISTOGRAM_REPORTER);
    spanMetricsCollector.setMetricFlushListener(TransactionNameManager::clearTransactionNames);
    return spanMetricsCollector;
  }

  @Override
  public void onStart(@Nonnull Context parentContext, @Nonnull ReadWriteSpan span) {}

  @Override
  public boolean isStartRequired() {
    return false;
  }

  @Override
  public void onEnd(ReadableSpan span) {
    final SpanContext parentSpanContext = span.toSpanData().getParentSpanContext();
    if (!parentSpanContext.isValid()
        || parentSpanContext.isRemote()) { // then a root span of this service
      final SpanData spanData = span.toSpanData();
      // this sometimes cause serious problem if NPE is throw. too expensive? We don't really have
      // to check as we always do inbound right now
      if (Boolean.TRUE.equals(spanData.getAttributes().get(AO_METRICS_KEY))) {
        MEASUREMENT_REPORTER.reportMetrics(spanData);
        HISTOGRAM_REPORTER.reportMetrics(spanData);
      }
    }
  }

  @Override
  public boolean isEndRequired() {
    return true;
  }

  private static class OpenTelemetryInboundMeasurementReporter extends MetricSpanReporter {
    public static final String MEASUREMENT_NAME_OLD = "TransactionResponseTime";
    public static final String MEASUREMENT_NAME = "ResponseTime";

    // cannot use Guava cache here, jboss issue...
    private final Map<MetricKey, SummaryLongMeasurement> measurements =
        new ConcurrentHashMap<MetricKey, SummaryLongMeasurement>();
    // private final String measurementName;

    protected OpenTelemetryInboundMeasurementReporter() {
      //            super(TRANSACTION_LATENCY_METRIC_NAME);
    }

    @Override
    public List<MetricsEntry<?>> consumeMetricEntries() {
      final Map<MetricKey, SummaryLongMeasurement> reportingMeasurements = consumeMeasurements();

      final List<MetricsEntry<?>> entries = new ArrayList<MetricsEntry<?>>();

      for (Map.Entry<MetricKey, SummaryLongMeasurement> entry : reportingMeasurements.entrySet()) {
        entries.add(new SummaryMeasurementMetricsEntry(entry.getKey(), entry.getValue()));
      }

      return entries;
    }

    public Map<MetricKey, SummaryLongMeasurement> consumeMeasurements() {
      // TODO concurrency
      final Map<MetricKey, SummaryLongMeasurement> consumedMeasurements =
          new HashMap<>(measurements);
      this.measurements.clear();
      return consumedMeasurements;
    }

    @Override
    protected void reportMetrics(com.solarwinds.joboe.span.impl.Span span, long l) {
      // not using the signature that takes OpenTracing span
    }

    private void reportMetrics(SpanData spanData) {
      final String transactionName = TransactionNameManager.getTransactionName(spanData);
      final Map<String, String> aoPrimaryKeys =
          Collections.singletonMap("TransactionName", transactionName);
      final Map<String, String> swoTags =
          new HashMap<String, String>() {
            {
              put("sw.transaction", transactionName);
            }
          };

      final Map<String, String> aoSecondaryKey = new HashMap<>();
      boolean hasError = spanData.getStatus().getStatusCode() == StatusCode.ERROR;
      final Long status =
          spanData.getAttributes().get(SemanticAttributes.HTTP_RESPONSE_STATUS_CODE);

      // special handling for status code
      if (!hasError && status != null) {
        hasError =
            HttpUtils.isServerErrorStatusCode(
                status.intValue()); // do not attempt to override the property if it's already
        // explicitly set
      }

      if (status != null) {
        aoSecondaryKey.put("HttpStatus", String.valueOf(status));
        swoTags.put("http.status_code", String.valueOf(status));
      }

      final String method = spanData.getAttributes().get(SemanticAttributes.HTTP_REQUEST_METHOD);
      if (method != null) {
        aoSecondaryKey.put("HttpMethod", method);
        swoTags.put("http.method", method);
      }

      if (hasError) {
        aoSecondaryKey.put("Errors", "true");
        swoTags.put("sw.is_error", "true");

      } else {
        swoTags.put("sw.is_error", "false");
      }

      final long duration = (spanData.getEndEpochNanos() - spanData.getStartEpochNanos()) / 1000;

      String collector = (String) ConfigManager.getConfig(ConfigProperty.AGENT_COLLECTOR);
      if (collector != null && collector.contains("appoptics.com")) {
        logger.debug("Sending metrics to AO");
        recordMeasurementEntryForAo(aoPrimaryKeys, aoSecondaryKey, duration);
      } else {
        logger.debug("Sending metrics to SWO");
        recordMeasurementEntryForSwo(swoTags, duration);
      }
    }

    protected void recordMeasurementEntryForAo(
        Map<String, String> primaryKeys, Map<String, String> secondaryKeys, long duration) {
      MetricKey metricKey = new MetricKey(MEASUREMENT_NAME_OLD, new HashMap<>(primaryKeys));
      this.measurements
          .computeIfAbsent(metricKey, k -> new SummaryLongMeasurement())
          .recordValue(duration);

      for (Map.Entry<String, String> optionalKey : secondaryKeys.entrySet()) {
        final Map<String, String> tags = new HashMap<>(primaryKeys);
        tags.put(optionalKey.getKey(), optionalKey.getValue());
        metricKey = new MetricKey(MEASUREMENT_NAME_OLD, tags);

        this.measurements
            .computeIfAbsent(metricKey, k -> new SummaryLongMeasurement())
            .recordValue(duration);
      }
    }

    protected void recordMeasurementEntryForSwo(Map<String, String> tags, long duration) {
      MetricKey measurementKey = new MetricKey(MEASUREMENT_NAME, new HashMap<>(tags));
      this.measurements
          .computeIfAbsent(measurementKey, k -> new SummaryLongMeasurement())
          .recordValue(duration);
    }
  }

  // TODO copied from Core, should improve to avoid code duplication
  private static class OpenTelemetryInboundHistogramReporter extends MetricSpanReporter {
    public static final String TRANSACTION_LATENCY_METRIC_NAME = "TransactionResponseTime";
    public static final String TRANSACTION_NAME_TAG_KEY = "TransactionName";
    private static final HistogramFactory.HistogramType HISTOGRAM_TYPE =
        HistogramFactory.HistogramType.HDR;

    private Map<MetricKey, Histogram> histograms = new ConcurrentHashMap<>();

    private OpenTelemetryInboundHistogramReporter() {}

    /**
     * Consumes and resets metric entries on this reporter
     *
     * @return a list of metric entries collected so since previous call to this method
     */
    public List<MetricsEntry<?>> consumeMetricEntries() {
      final Map<MetricKey, Histogram> reportingHistograms = consumeHistograms();

      final List<MetricsEntry<?>> entries = new ArrayList<MetricsEntry<?>>();
      for (Map.Entry<MetricKey, Histogram> entry : reportingHistograms.entrySet()) {
        entries.add(new HistogramMetricsEntry(entry.getKey(), entry.getValue()));
      }

      return entries;
    }

    public Map<MetricKey, Histogram> consumeHistograms() {
      final Map<MetricKey, Histogram> reportingHistograms = new HashMap<>(histograms);
      histograms.clear();

      return reportingHistograms;
    }

    /** Records the span duration as a histogram and a measurement */
    protected void reportMetrics(Span span, long duration) {
      // not used
    }

    public void reportMetrics(SpanData spanData) {
      final MetricKey serviceHistogramKey =
          new MetricKey(
              TRANSACTION_LATENCY_METRIC_NAME,
              null); // globally for all transactions within this service
      final Histogram serviceHistogram;
      serviceHistogram =
          histograms.computeIfAbsent(
              serviceHistogramKey,
              k -> HistogramFactory.buildHistogram(HISTOGRAM_TYPE, MAX_DURATION));

      final long duration = (spanData.getEndEpochNanos() - spanData.getStartEpochNanos()) / 1000;
      try {
        serviceHistogram.recordValue(duration);
      } catch (HistogramException e) {
        logger.debug("Failed to report metrics to service level histogram : " + e.getMessage(), e);
      }

      final String transactionName = TransactionNameManager.getTransactionName(spanData);

      if (transactionName != null) {
        // specifically for this transaction
        final MetricKey transactionHistogramKey =
            new MetricKey(
                TRANSACTION_LATENCY_METRIC_NAME,
                Collections.singletonMap("TransactionName", transactionName));
        final Histogram transactionHistogram =
            histograms.computeIfAbsent(
                transactionHistogramKey,
                k -> HistogramFactory.buildHistogram(HISTOGRAM_TYPE, MAX_DURATION));
        try {
          transactionHistogram.recordValue(duration);
        } catch (HistogramException e) {
          logger.debug(
              "Failed to report metrics to transaction histogram with metrics key ["
                  + transactionHistogramKey
                  + "] : "
                  + e.getMessage(),
              e);
        }
      }
    }
  }
}
