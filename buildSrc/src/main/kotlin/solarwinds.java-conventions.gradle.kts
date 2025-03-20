import com.solarwinds.instrumentation.gradle.SolarwindsJavaExtension
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
  checkstyle
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

val swoJava = extensions.create<SolarwindsJavaExtension>("swoJava")

java {
  toolchain {
    languageVersion.set(
      swoJava.minJavaVersionSupported.map {
        val defaultJavaVersion = swoJava.maxJavaVersionSupported.getOrElse(JavaVersion.VERSION_17).majorVersion.toInt()
        JavaLanguageVersion.of(it.majorVersion.toInt().coerceAtLeast(defaultJavaVersion))
      }
    )
  }

  // See https://docs.gradle.org/current/userguide/upgrading_version_5.html, Automatic target JVM version
  disableAutoTargetJvm()
  withJavadocJar()
  withSourcesJar()
}

dependencies {
  dependencyManagementConfiguration(platform(project(":dependencyManagement")))

  testImplementation(project(":bootstrap"))
  testCompileOnly("com.google.auto.service:auto-service")
  testImplementation("io.opentelemetry.semconv:opentelemetry-semconv-incubating")

  testImplementation("org.mockito:mockito-core")
  testImplementation("org.mockito:mockito-inline")
  testImplementation("org.mockito:mockito-junit-jupiter")

  testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
  testImplementation("org.junit.jupiter:junit-jupiter-api")
  testImplementation("javax.annotation:javax.annotation-api")

  testImplementation("io.opentelemetry.javaagent:opentelemetry-javaagent-tooling")
  testImplementation("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure")
  testImplementation("io.opentelemetry.javaagent:opentelemetry-javaagent-extension-api")

  testImplementation("io.opentelemetry:opentelemetry-sdk-testing")
  testImplementation("io.opentelemetry.semconv:opentelemetry-semconv")
  testImplementation("io.opentelemetry.instrumentation:opentelemetry-instrumentation-api")

  testImplementation("com.solarwinds.joboe:core")
  testImplementation("com.solarwinds.joboe:metrics")
  testImplementation("org.junit-pioneer:junit-pioneer")

  testImplementation("org.junit.jupiter:junit-jupiter-params")
}

tasks {
  withType<Test>().configureEach {
    useJUnitPlatform()
    testLogging {
      events("passed", "skipped", "failed")
    }
    environment("SW_APM_SERVICE_KEY", "token:name")
  }

  withType<JavaCompile>().configureEach {
    with(options) {
      release.set(swoJava.minJavaVersionSupported.get().majorVersion.toInt())
      compilerArgs.addAll(
        listOf(
          "-Xlint:all",
          // disable annotation ownership warnings
          "-Xlint:-processing",
          "-Werror"
        )
      )

      encoding = "UTF-8"
    }
  }

  named<Javadoc>("javadoc") {
    with(options as StandardJavadocDocletOptions) {
      source = "8"
      encoding = "UTF-8"
      docEncoding = "UTF-8"
      charSet = "UTF-8"
      breakIterator(true)

      addStringOption("Xdoclint:none", "-quiet")
      // non-standard option to fail on warnings, see https://bugs.openjdk.java.net/browse/JDK-8200363
      addStringOption("Xwerror", "-quiet")
    }
  }
}

checkstyle {
  configFile = file("$rootDir/checkstyle.xml")
}