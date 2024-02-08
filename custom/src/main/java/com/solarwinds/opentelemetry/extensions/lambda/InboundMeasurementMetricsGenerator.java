package com.solarwinds.opentelemetry.extensions.lambda;

import com.solarwinds.opentelemetry.extensions.TransactionNameManager;
import com.solarwinds.joboe.core.util.HttpUtils;
import com.solarwinds.joboe.shaded.javax.annotation.Nonnull;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
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
  private LongHistogram responseTime;

  private void initializeMeasurements() {
    if (responseTime != null) {
      return;
    }

    Meter meter = MeterProvider.getRequestMetricsMeter();
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

      AttributesBuilder responseTimeAttr = Attributes.builder();
      if (status != null) {
        String key = "http.status_code";
        responseTimeAttr.put(key, status);
      }

      final String method = spanData.getAttributes().get(SemanticAttributes.HTTP_REQUEST_METHOD);
      if (method != null) {
        String methodKey = "http.method";
        responseTimeAttr.put(methodKey, method);
      }

      String errorKey = "sw.is_error";
      String transactionKey = "sw.transaction";
      if (hasError) {
        responseTimeAttr.put(errorKey, "true");
      } else {
        responseTimeAttr.put(errorKey, "false");
      }

      responseTime.record(duration, responseTimeAttr.put(transactionKey, transactionName).build());
    }
  }

  @Override
  public boolean isEndRequired() {
    return true;
  }
}
