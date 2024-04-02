package com.solarwinds.opentelemetry.extensions;

import static com.solarwinds.opentelemetry.extensions.SharedNames.TRANSACTION_NAME_KEY;

import com.solarwinds.joboe.logging.Logger;
import com.solarwinds.joboe.logging.LoggerFactory;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.metrics.LongHistogram;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.semconv.SemanticAttributes;

public class InboundMeasurementMetricsGenerator implements SpanProcessor {
  private LongHistogram responseTime;

  private static final Logger logger = LoggerFactory.getLogger();

  private void initializeMeasurements() {
    if (responseTime != null) {
      return;
    }

    Meter meter = MeterProvider.getRequestMetricsMeter();
    responseTime =
        meter.histogramBuilder("trace.service.response_time").setUnit("ms").ofLongs().build();
  }

  @Override
  public void onStart(Context parentContext, ReadWriteSpan span) {
    SpanContext parentSpanContext = Span.fromContext(parentContext).getSpanContext();
    if (!parentSpanContext.isValid()
        || parentSpanContext.isRemote()) { // then a root span of this service
      String transactionName = TransactionNameManager.getTransactionName(span.toSpanData());
      span.setAttribute(TRANSACTION_NAME_KEY, transactionName);
      logger.debug(
          String.format("Transaction name derived on root span start: %s", transactionName));
    }
  }

  @Override
  public boolean isStartRequired() {
    return true;
  }

  @Override
  public void onEnd(ReadableSpan span) {
    final SpanContext parentSpanContext = span.toSpanData().getParentSpanContext();
    if (!parentSpanContext.isValid() || parentSpanContext.isRemote()) {
      initializeMeasurements();
      final SpanData spanData = span.toSpanData();
      final String transactionName = TransactionNameManager.getTransactionName(spanData);

      boolean hasError = spanData.getStatus().getStatusCode() == StatusCode.ERROR;
      final Long status =
          spanData.getAttributes().get(SemanticAttributes.HTTP_RESPONSE_STATUS_CODE);

      final long duration =
          (spanData.getEndEpochNanos() - spanData.getStartEpochNanos()) / 1_000_000;
      if (!hasError && status != null) {
        hasError = status.intValue() > 499;
      }

      AttributesBuilder responseTimeAttr = Attributes.builder();
      if (span.getKind() == SpanKind.SERVER && status != null) {
        String key = "http.status_code";
        responseTimeAttr.put(key, status);
      }

      final String method = spanData.getAttributes().get(SemanticAttributes.HTTP_REQUEST_METHOD);
      if (span.getKind() == SpanKind.SERVER && method != null) {
        String methodKey = "http.method";
        responseTimeAttr.put(methodKey, method);
      }

      String errorKey = "sw.is_error";
      responseTimeAttr.put(errorKey, hasError);

      responseTime.record(
          duration, responseTimeAttr.put(TRANSACTION_NAME_KEY, transactionName).build());
    }
  }

  @Override
  public boolean isEndRequired() {
    return true;
  }
}
