package com.appoptics.opentelemetry.extensions.lambda;

import com.appoptics.opentelemetry.extensions.TransactionNameManager;
import com.solarwinds.shaded.javax.annotation.Nonnull;
import com.solarwinds.util.HttpUtils;
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
        String key = "http.status_code";
        requestCounterAttr.put(key, status);
        requestErrorCounterAttr.put(key, status);
        responseTimeAttr.put(key, status);
      }

      final String method = spanData.getAttributes().get(SemanticAttributes.HTTP_REQUEST_METHOD);
      if (method != null) {
        String methodKey = "http.method";

        requestCounterAttr.put(methodKey, method);
        requestErrorCounterAttr.put(methodKey, method);
        responseTimeAttr.put(methodKey, method);
      }

      String errorKey = "sw.is_error";
      String transactionKey = "sw.transaction";
      if (hasError) {
        requestCounterAttr.put(errorKey, "true");
        requestErrorCounterAttr.put(errorKey, "true");
        responseTimeAttr.put(errorKey, "true");

        requestErrorCounter.add(
            1, requestErrorCounterAttr.put(transactionKey, transactionName).build());

      } else {
        requestCounterAttr.put(errorKey, "false");
        requestErrorCounterAttr.put(errorKey, "false");
        responseTimeAttr.put(errorKey, "false");
      }

      requestCounter.add(1, requestCounterAttr.put(transactionKey, transactionName).build());
      responseTime.record(duration, responseTimeAttr.put(transactionKey, transactionName).build());
    }
  }

  @Override
  public boolean isEndRequired() {
    return true;
  }
}
