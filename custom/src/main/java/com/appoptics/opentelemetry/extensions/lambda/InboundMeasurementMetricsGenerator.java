package com.appoptics.opentelemetry.extensions.lambda;

import com.appoptics.opentelemetry.extensions.TransactionNameManager;
import com.tracelytics.util.HttpUtils;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.LongHistogram;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.semconv.SemanticAttributes;
import javax.annotation.Nonnull;

public class InboundMeasurementMetricsGenerator implements SpanProcessor {
  private LongCounter requestCounter;

  private LongCounter requestErrorCounter;

  private LongHistogram responseTime;

  private void initializeMeasurements() {
    if (requestCounter != null) {
      return;
    }

    Meter meter = MeterProvider.getRequestMetricsMeter();
    requestCounter = meter.counterBuilder("trace.service.requests").build();

    requestErrorCounter = meter.counterBuilder("trace.service.errors").build();
    responseTime =
        meter.histogramBuilder("trace.service.response_time").setUnit("ms").ofLongs().build();
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
        hasError = HttpUtils.isServerErrorStatusCode(status.intValue());
      }

      AttributesBuilder requestCounterAttr = Attributes.builder();
      AttributesBuilder requestErrorCounterAttr = Attributes.builder();
      AttributesBuilder responseTimeAttr = Attributes.builder();

      if (status != null) {
        requestCounterAttr.put(SemanticAttributes.HTTP_RESPONSE_STATUS_CODE, status);
        requestErrorCounterAttr.put(SemanticAttributes.HTTP_RESPONSE_STATUS_CODE, status);
        responseTimeAttr.put(SemanticAttributes.HTTP_RESPONSE_STATUS_CODE, status);
      }

      final String method = spanData.getAttributes().get(SemanticAttributes.HTTP_REQUEST_METHOD);
      if (method != null) {
        String methodKey = "http.method";

        requestCounterAttr.put(methodKey, method);
        requestErrorCounterAttr.put(methodKey, method);
        responseTimeAttr.put(methodKey, method);
      }

      if (hasError) {
        requestCounterAttr.put("sw.is_error", "true");
        requestErrorCounterAttr.put("sw.is_error", "true");
        responseTimeAttr.put("sw.is_error", "true");

        requestErrorCounter.add(
            1, requestErrorCounterAttr.put("sw.transaction", transactionName).build());

      } else {
        requestCounterAttr.put("sw.is_error", "false");
        requestErrorCounterAttr.put("sw.is_error", "false");
        responseTimeAttr.put("sw.is_error", "false");
      }

      requestCounter.add(1, requestCounterAttr.put("sw.transaction", transactionName).build());
      responseTime.record(
          duration, responseTimeAttr.put("sw.transaction", transactionName).build());
    }
  }

  @Override
  public boolean isEndRequired() {
    return true;
  }
}
