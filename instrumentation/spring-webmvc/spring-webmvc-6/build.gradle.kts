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

plugins {
  id("solarwinds.instrumentation-conventions")
}

muzzle {
  pass {
    group.set("org.springframework")
    module.set("spring-webmvc")
    versions.set("[6.0.0,)")
    // these versions depend on org.springframework:spring-web which has a bad dependency on
    // javax.faces:jsf-api:1.1 which was released as pom only
    skip("1.2.1", "1.2.2", "1.2.3", "1.2.4")
    // 3.2.1.RELEASE has transitive dependencies like spring-web as "provided" instead of "compile"
    skip("3.2.1.RELEASE")
    extraDependency("jakarta.servlet:jakarta.servlet-api:6.1.0")
    assertInverse.set(true)
  }
}

dependencies {
  compileOnly("org.springframework:spring-webmvc:6.0.0")
  compileOnly("jakarta.servlet:jakarta.servlet-api:6.1.0")
  compileOnly("io.opentelemetry.instrumentation:opentelemetry-instrumentation-api-incubator")
}

swoJava {
  minJavaVersionSupported.set(JavaVersion.VERSION_17)
}
