package com.appoptics.opentelemetry.extensions;

import com.appoptics.opentelemetry.core.RootSpan;
import com.tracelytics.ext.google.common.cache.Cache;
import com.tracelytics.ext.google.common.cache.CacheBuilder;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;

import java.util.concurrent.TimeUnit;

public class AppOpticsRootSpanProcessor implements SpanProcessor {
    private static Cache<String, Scope> allRootScopes = null; //has to lazy initialize this, otherwise jboss logmanager would have issues


    @Override
    public void onStart(Context parentContext, ReadWriteSpan span) {
        SpanContext parentSpanContext = Span.fromContext(parentContext).getSpanContext();
        if (!parentSpanContext.isValid() || parentSpanContext.isRemote()) { //then a root span of this service
            Scope scope = RootSpan.with(parentContext, span).makeCurrent();
            rootScopes().put(span.getSpanContext().getTraceId(), scope);
        }
    }

    @Override
    public boolean isStartRequired() {
        return true;
    }

    @Override
    public void onEnd(ReadableSpan span) {
        SpanContext parentSpanContext = span.toSpanData().getParentSpanContext();
        if (!parentSpanContext.isValid() || parentSpanContext.isRemote()) { //then a root span of this service
            Scope scope = rootScopes().getIfPresent(span.getSpanContext().getTraceId());
            if (scope != null) {
                scope.close();
            }
        }
    }

    private synchronized Cache<String, Scope> rootScopes() {
        if (allRootScopes == null) {
            allRootScopes = CacheBuilder.newBuilder().expireAfterWrite(1200L, TimeUnit.SECONDS).build();
        }
        return allRootScopes;
    }

    @Override
    public boolean isEndRequired() {
        return true;
    }
}
