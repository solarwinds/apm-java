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

import com.google.auto.service.AutoService;
import com.solarwinds.joboe.shaded.google.gson.Gson;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSpanExporterProvider;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@AutoService(ConfigurableSpanExporterProvider.class)
public class DumpSpanExporter implements SpanExporter, ConfigurableSpanExporterProvider {
  Gson gson = new Gson();

  Map<String, List<SpanData>> traces = new HashMap<>();

  @Override
  public CompletableResultCode export(Collection<SpanData> collection) {
    collection.forEach(
        spanData ->
            traces
                .computeIfAbsent(spanData.getTraceId(), (key) -> new ArrayList<>())
                .add(spanData));
    return CompletableResultCode.ofSuccess();
  }

  @Override
  public CompletableResultCode flush() {
    System.out.printf("Flush -> Chubi Spans: %s%n", traces);
    return CompletableResultCode.ofSuccess();
  }

  @Override
  public CompletableResultCode shutdown() {
    System.out.printf("Shutdown -> Chubi Spans: %s%n", traces);
    return CompletableResultCode.ofSuccess();
  }

  @Override
  public SpanExporter createExporter(ConfigProperties configProperties) {
    return new DumpSpanExporter();
  }

  @Override
  public String getName() {
    return "dump";
  }
}
