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
import static com.solarwinds.opentelemetry.extensions.initialize.AutoConfigurationCustomizerProviderImpl.isAgentEnabled;

import com.solarwinds.joboe.core.Context;
import com.solarwinds.joboe.core.Event;
import com.solarwinds.joboe.core.EventImpl;
import com.solarwinds.joboe.core.EventReporter;
import com.solarwinds.joboe.logging.Logger;
import com.solarwinds.joboe.logging.LoggerFactory;
import com.solarwinds.joboe.sampling.Metadata;
import com.solarwinds.joboe.sampling.SamplingException;
import com.solarwinds.joboe.shaded.javax.annotation.Nonnull;
import com.solarwinds.opentelemetry.core.Constants;
import com.solarwinds.opentelemetry.core.Util;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.common.InstrumentationScopeInfo;
import io.opentelemetry.sdk.trace.data.EventData;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.opentelemetry.semconv.ExceptionAttributes;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Span exporter to be used with the OpenTelemetry auto agent */
public class SolarwindsSpanExporter implements SpanExporter {
  private final Logger logger = LoggerFactory.getLogger();

  @Override
  public CompletableResultCode export(@Nonnull Collection<SpanData> collection) {
    if (!isAgentEnabled()) {
      return CompletableResultCode.ofSuccess();
    }
    EventReporter eventReporter = ReporterProvider.getEventReporter();
    logger.debug("Started to export span data to the collector.");
    for (SpanData spanData : collection) {
      if (spanData.hasEnded()) {
        try {
          Metadata parentMetadata = null;
          if (spanData.getParentSpanContext().isValid()) {
            parentMetadata = Util.buildMetadata(spanData.getParentSpanContext());
          }

          final String w3cContext = Util.w3cContextToHexString(spanData.getSpanContext());
          final String spanName =
              String.format(LAYER_NAME_PLACEHOLDER, spanData.getKind(), spanData.getName().trim());

          final Metadata spanMetadata = new Metadata(w3cContext);
          spanMetadata.randomizeOpID(); // get around the metadata logic, this op id is not used
          // set spanMetadata into the current Context so that after the initial event, the metadata
          // does not need to be manually managed for event creation / linkage.  Context is updated
          // with the metadata of the last reported event.
          Context.setMetadata(spanMetadata);

          // create new event with an override metadata from the OTel spanData (w3cContext)
          // and link to OTel parent (if present).
          // do this with EventImpl instead of Context because we need to use the addEdge param.
          Event entryEvent;
          if (parentMetadata != null) {
            entryEvent = new EventImpl(parentMetadata, w3cContext, true);
          } else {
            entryEvent = new EventImpl(null, w3cContext, false);
          }

          InstrumentationScopeInfo scopeInfo = spanData.getInstrumentationScopeInfo();
          entryEvent.addInfo(
              "Label",
              "entry",
              "sw.span_kind",
              spanData.getKind().toString(),
              "otel.scope.name",
              scopeInfo.getName());

          entryEvent.addInfo("otel.scope.version", scopeInfo.getVersion());
          entryEvent.setTimestamp(spanData.getStartEpochNanos() / 1000);
          entryEvent.addInfo(getEventKvs(spanData.getAttributes()));
          entryEvent.report(eventReporter);

          for (EventData event : spanData.getEvents()) {
            if ("exception".equals(event.getName())) {
              reportErrorEvent(event);
            } else {
              reportInfoEvent(event);
            }
          }

          final Event exitEvent = Context.createEvent();
          exitEvent.addInfo(
              "Label",
              "exit",
              "Layer",
              spanName,
              "otel.status_code",
              spanData.getStatus().getStatusCode().toString(),
              "otel.status_description",
              spanData.getStatus().getDescription());
          exitEvent.setTimestamp(spanData.getEndEpochNanos() / 1000);
          exitEvent.report(eventReporter);
        } catch (SamplingException oboeException) {
          logger.error(
              String.format("Error reporting span: %s", spanData.getSpanId()), oboeException);
        } finally {
          // clear Context for the next OTel span, which will initialize it with w3cContext
          Context.clearMetadata();
        }
      }
    }

    logger.debug("Finished buffering " + collection.size() + " spans");
    return CompletableResultCode.ofSuccess();
  }

  private static final List<AttributeKey<?>> OPEN_TELEMETRY_ERROR_ATTRIBUTE_KEYS =
      Arrays.asList(
          ExceptionAttributes.EXCEPTION_MESSAGE,
          ExceptionAttributes.EXCEPTION_TYPE,
          ExceptionAttributes.EXCEPTION_STACKTRACE);

  private void reportErrorEvent(EventData eventData) {
    final Event event = Context.createEvent();
    final Attributes attributes = eventData.getAttributes();
    String message = attributes.get(ExceptionAttributes.EXCEPTION_MESSAGE);
    if (message == null) {
      message = "";
    }
    event.addInfo(
        "Label",
        "error",
        "Spec",
        "error",
        "ErrorClass",
        attributes.get(ExceptionAttributes.EXCEPTION_TYPE),
        "ErrorMsg",
        message,
        "Backtrace",
        attributes.get(ExceptionAttributes.EXCEPTION_STACKTRACE));

    final Map<AttributeKey<?>, Object> otherKvs = filterAttributes(attributes);
    OPEN_TELEMETRY_ERROR_ATTRIBUTE_KEYS.forEach(otherKvs.keySet()::remove);
    for (Map.Entry<AttributeKey<?>, Object> keyValue : otherKvs.entrySet()) {
      event.addInfo(keyValue.getKey().getKey(), keyValue.getValue());
    }
    event.setTimestamp(eventData.getEpochNanos() / 1000); // convert to micro second
    event.report(ReporterProvider.getEventReporter());
  }

  private void reportInfoEvent(EventData eventData) {
    final Event event = Context.createEvent();
    final Attributes attributes = eventData.getAttributes();
    event.addInfo("Label", "info", "sw.event_name", eventData.getName());
    final Map<AttributeKey<?>, Object> otherKvs = filterAttributes(attributes);
    for (Map.Entry<AttributeKey<?>, Object> keyValue : otherKvs.entrySet()) {
      event.addInfo(keyValue.getKey().getKey(), keyValue.getValue());
    }

    event.setTimestamp(eventData.getEpochNanos() / 1000); // convert to micro second
    event.report(ReporterProvider.getEventReporter());
  }

  private static Map<AttributeKey<?>, Object> filterAttributes(Attributes inputAttributes) {
    final Map<AttributeKey<?>, Object> result = new HashMap<>();
    for (Map.Entry<AttributeKey<?>, Object> keyValue : inputAttributes.asMap().entrySet()) {
      AttributeKey<?> key = keyValue.getKey();
      if (!key.getKey().startsWith(Constants.SW_INTERNAL_ATTRIBUTE_PREFIX)) {
        result.put(key, keyValue.getValue());
      }
    }
    return result;
  }

  @Override
  public CompletableResultCode flush() {
    return CompletableResultCode.ofSuccess();
  }

  @Override
  public CompletableResultCode shutdown() {
    return CompletableResultCode.ofSuccess();
  }

  private Map<String, ?> getEventKvs(Attributes inputAttributes) {
    final Map<AttributeKey<?>, Object> attributes = filterAttributes(inputAttributes);
    final Map<String, Object> tags = new HashMap<String, Object>();
    for (Map.Entry<AttributeKey<?>, Object> entry : attributes.entrySet()) {
      Object attributeValue = entry.getValue();
      final String attributeKey = entry.getKey().getKey();

      tags.put(attributeKey, attributeValue);
    }
    return tags;
  }

  @Override
  public void close() {}
}
