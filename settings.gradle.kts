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

pluginManagement {
    repositories {
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.9.0"
}

rootProject.name = "solarwinds-apm-java"

include("agent")
include("solarwinds-otel-sdk")
include("bootstrap")
include("custom")
include("custom:lambda")
include("custom:shared")
include("agent-lambda")
include("instrumentation")
include("instrumentation:jdbc")
include("instrumentation:spring-webmvc:spring-webmvc-3.1")
include("instrumentation:servlet-5.0")
include("instrumentation:servlet-3.0")
include("instrumentation:spring-webmvc:spring-webmvc-6")
include("instrumentation:spring-webmvc")
include("instrumentation:hibernate:hibernate-shared")
include("instrumentation:hibernate:hibernate-6.0")
include("instrumentation:hibernate:hibernate-4.0")
include("instrumentation:instrumentation-shared")
include("testing")
include("testing:agent-for-testing")
include("testing:agent-test-extension")
include("dependencyManagement")

