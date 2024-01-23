package com.appoptics.opentelemetry.extensions;

import com.appoptics.opentelemetry.core.RootSpan;
import com.solarwinds.joboe.core.logging.Logger;
import com.solarwinds.joboe.core.logging.LoggerFactory;
import com.solarwinds.joboe.shaded.javax.annotation.Nonnull;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;

/** Span processor to keep track of the root span of a trace */
public class SolarwindsRootSpanProcessor implements SpanProcessor {

  private static final Logger logger = LoggerFactory.getLogger();

  @Override
  public void onStart(@Nonnull Context parentContext, @Nonnull ReadWriteSpan span) {
    SpanContext parentSpanContext = Span.fromContext(parentContext).getSpanContext();
    if (!parentSpanContext.isValid()
        || parentSpanContext.isRemote()) { // then a root span of this service
      RootSpan.setRootSpan(span);
      String transactionName = TransactionNameManager.getTransactionName(span.toSpanData());

      span.setAttribute("sw.transaction", transactionName);
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
    SpanContext parentSpanContext = span.toSpanData().getParentSpanContext();
    if (!parentSpanContext.isValid()
        || parentSpanContext.isRemote()) { // then a root span of this service
      RootSpan.clearRootSpan(span.getSpanContext().getTraceId());
    }
  }

  @Override
  public boolean isEndRequired() {
    return true;
  }
}