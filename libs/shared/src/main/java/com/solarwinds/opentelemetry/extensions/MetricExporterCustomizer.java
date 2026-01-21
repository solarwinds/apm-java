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

import static com.solarwinds.opentelemetry.extensions.SharedNames.SW_OTEL_PROXY_HOST_KEY;
import static com.solarwinds.opentelemetry.extensions.SharedNames.SW_OTEL_PROXY_PORT_KEY;

import com.solarwinds.opentelemetry.extensions.config.ProxyHelper;
import io.opentelemetry.exporter.otlp.http.metrics.OtlpHttpMetricExporter;
import io.opentelemetry.exporter.otlp.http.metrics.OtlpHttpMetricExporterBuilder;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.common.export.ProxyOptions;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import java.net.InetSocketAddress;
import java.util.Objects;
import java.util.function.BiFunction;

public class MetricExporterCustomizer
    implements BiFunction<MetricExporter, ConfigProperties, MetricExporter> {

  @Override
  public MetricExporter apply(MetricExporter metricExporter, ConfigProperties configProperties) {
    if (ProxyHelper.isProxyConfigured(configProperties)
        && metricExporter instanceof OtlpHttpMetricExporter) {
      OtlpHttpMetricExporterBuilder builder = ((OtlpHttpMetricExporter) metricExporter).toBuilder();
      return new DelegatingMetricExporter(
          builder
              .setProxyOptions(
                  ProxyOptions.create(
                      new InetSocketAddress(
                          Objects.requireNonNull(
                              configProperties.getString(SW_OTEL_PROXY_HOST_KEY)),
                          Objects.requireNonNull(configProperties.getInt(SW_OTEL_PROXY_PORT_KEY)))))
              .build());
    }

    return new DelegatingMetricExporter(metricExporter);
  }
}
