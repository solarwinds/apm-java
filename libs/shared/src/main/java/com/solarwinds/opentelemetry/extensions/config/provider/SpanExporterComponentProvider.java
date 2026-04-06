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

package com.solarwinds.opentelemetry.extensions.config.provider;

import static io.opentelemetry.exporter.otlp.internal.OtlpConfigUtil.DATA_TYPE_TRACES;

import com.google.auto.service.AutoService;
import com.solarwinds.joboe.config.ConfigManager;
import com.solarwinds.joboe.config.ConfigProperty;
import com.solarwinds.joboe.config.ProxyConfig;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.api.metrics.MeterProvider;
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter;
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporterBuilder;
import io.opentelemetry.exporter.otlp.internal.OtlpDeclarativeConfigUtil;
import io.opentelemetry.sdk.autoconfigure.spi.internal.ComponentProvider;
import io.opentelemetry.sdk.common.export.ProxyOptions;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import java.net.InetSocketAddress;

@AutoService(ComponentProvider.class)
public class SpanExporterComponentProvider implements ComponentProvider {

  public static final String COMPONENT_NAME = "swo/spanExporter";

  @Override
  public Class<SpanExporter> getType() {
    return SpanExporter.class;
  }

  @Override
  public String getName() {
    return COMPONENT_NAME;
  }

  @Override
  public SpanExporter create(DeclarativeConfigProperties config) {
    OtlpHttpSpanExporterBuilder builder = OtlpHttpSpanExporter.builder();

    OtlpDeclarativeConfigUtil.configureOtlpExporterBuilder(
        DATA_TYPE_TRACES,
        config,
        builder::setComponentLoader,
        builder::setEndpoint,
        builder::addHeader,
        builder::setCompression,
        builder::setTimeout,
        builder::setTrustedCertificates,
        builder::setClientTls,
        builder::setRetryPolicy,
        builder::setMemoryMode,
        true,
        builder::setInternalTelemetryVersion,
        () -> builder.setMeterProvider(MeterProvider::noop));

    ProxyConfig proxyConfig = (ProxyConfig) ConfigManager.getConfig(ConfigProperty.AGENT_PROXY);
    if (proxyConfig != null) {
      builder.setProxy(
          ProxyOptions.create(new InetSocketAddress(proxyConfig.getHost(), proxyConfig.getPort())));
    }

    return builder.build();
  }
}
