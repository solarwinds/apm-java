/*
 * © SolarWinds Worldwide, LLC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.solarwinds.opentelemetry.extensions;

import static com.solarwinds.opentelemetry.extensions.SharedNames.LAYER_NAME_PLACEHOLDER;
import static com.solarwinds.opentelemetry.extensions.SharedNames.TRANSACTION_NAME_KEY;

import com.solarwinds.joboe.logging.Logger;
import com.solarwinds.joboe.logging.LoggerFactory;
import io.opentelemetry.api.common.AttributeKey;
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
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.internal.ExtendedSpanProcessor;
import io.opentelemetry.semconv.SemanticAttributes;

public class InboundMeasurementMetricsGenerator implements ExtendedSpanProcessor {
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

  @Override
  public void onEnding(ReadWriteSpan span) {
    SpanData spanData = span.toSpanData();
    final SpanContext parentSpanContext = spanData.getParentSpanContext();
    if (!parentSpanContext.isValid() || parentSpanContext.isRemote()) {
      span.setAttribute(TRANSACTION_NAME_KEY, TransactionNameManager.getTransactionName(spanData));
      span.setAttribute(
          AttributeKey.stringKey("TransactionName"),
          TransactionNameManager.getTransactionName(spanData));
    }

    span.setAttribute(
        "Layer", String.format(LAYER_NAME_PLACEHOLDER, span.getKind(), span.getName().trim()));
  }

  @Override
  public boolean isOnEndingRequired() {
    return true;
  }
}
