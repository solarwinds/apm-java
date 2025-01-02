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

dependencies {
  compileOnly(project(":custom:shared"))
  compileOnly("com.google.code.gson:gson")
  compileOnly("org.projectlombok:lombok")

  annotationProcessor("org.projectlombok:lombok")
  compileOnly("com.solarwinds.joboe:config")
  compileOnly("com.solarwinds.joboe:sampling")

  compileOnly("com.solarwinds.joboe:logging")
  annotationProcessor("com.google.auto.service:auto-service")
  compileOnly("com.google.auto.service:auto-service-annotations")

  compileOnly("io.opentelemetry:opentelemetry-api")
  compileOnly("io.opentelemetry.semconv:opentelemetry-semconv")
  compileOnly("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure")

  compileOnly("io.opentelemetry.javaagent:opentelemetry-javaagent-tooling")
  compileOnly("io.opentelemetry.instrumentation:opentelemetry-instrumentation-api")
  compileOnly("io.opentelemetry.javaagent:opentelemetry-javaagent-extension-api")

  compileOnly(project(":bootstrap"))
  testImplementation(project(":custom:shared"))
}

swoJava {
  minJavaVersionSupported.set(JavaVersion.VERSION_1_8)
}
