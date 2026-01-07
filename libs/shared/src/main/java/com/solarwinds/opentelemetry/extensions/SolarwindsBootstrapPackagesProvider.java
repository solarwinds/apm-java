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
import io.opentelemetry.javaagent.tooling.bootstrap.BootstrapPackagesBuilder;
import io.opentelemetry.javaagent.tooling.bootstrap.BootstrapPackagesConfigurer;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;

/**
 * This adds the Joboe core classes to the list which would always be loaded by the bootstrap
 * classloader, no matter what classloader is used initially. The Otel agent instruments all
 * classloaders and checks the class named to be loaded. It will load the class with the bootstrap
 * classloader if the class matches any of the prefix in the list above.
 */
@AutoService(BootstrapPackagesConfigurer.class)
public class SolarwindsBootstrapPackagesProvider implements BootstrapPackagesConfigurer {
  @Override
  public void configure(BootstrapPackagesBuilder builder, ConfigProperties config) {
    builder.add("com.solarwinds.joboe");
  }
}
