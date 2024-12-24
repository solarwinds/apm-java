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
  java
  id("com.github.jarmstrong.buildconfig")
}

repositories {
  mavenLocal()
  mavenCentral()
  maven {
    url = uri("https://plugins.gradle.org/m2/")
  }

  maven {
    name = "sonatype"
    url = uri("https://oss.sonatype.org/content/repositories/snapshots")
  }

  maven {
    url = uri("https://maven.pkg.github.com/solarwinds/joboe")
    credentials {
      username = System.getenv("GITHUB_USERNAME")
      password = System.getenv("GITHUB_TOKEN")
    }
  }
}

evaluationDependsOn(":dependencyManagement")
val dependencyManagementConfiguration = configurations.create("dependencyManagement") {
  isCanBeConsumed = false
  isCanBeResolved = false
  isVisible = false
}

afterEvaluate {
  configurations.configureEach {
    if (isCanBeResolved && !isCanBeConsumed) {
      // propagate constraints
      extendsFrom(dependencyManagementConfiguration)
    }
  }
}

dependencies {
  dependencyManagementConfiguration(platform(project(":dependencyManagement")))
  testImplementation(project(":bootstrap"))

  testImplementation("org.mockito:mockito-core")
  testImplementation("org.mockito:mockito-inline")
  testImplementation("org.mockito:mockito-junit-jupiter")

  testImplementation("javax.annotation:javax.annotation-api")
  testImplementation("org.junit.jupiter:junit-jupiter-api")
  testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")

  testImplementation("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure")
  testImplementation("io.opentelemetry.javaagent:opentelemetry-javaagent-extension-api")
  testImplementation("io.opentelemetry.javaagent:opentelemetry-javaagent-tooling")

  testImplementation("io.opentelemetry.instrumentation:opentelemetry-instrumentation-api")
  testImplementation("io.opentelemetry.semconv:opentelemetry-semconv")
  testImplementation("io.opentelemetry:opentelemetry-sdk-testing")

  testImplementation("com.solarwinds.joboe:core")
  testImplementation("com.solarwinds.joboe:metrics")
  testImplementation("org.junit-pioneer:junit-pioneer")
}

tasks.withType<Test>().configureEach {
  useJUnitPlatform()
  testLogging {
    events("passed", "skipped", "failed")
  }
  environment("SW_APM_SERVICE_KEY", "token:name")
}
