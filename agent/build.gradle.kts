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

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
  id("signing")
  id("solarwinds.shadow-conventions")
}

base.archivesName = "solarwinds-apm-agent"
val swoAgentVersion: String by rootProject.extra

// this configuration collects libs that will be placed in the bootstrap classloader
val bootstrapLibs: Configuration by configurations.creating {
  isCanBeResolved = true
  isCanBeConsumed = false
}
// this configuration collects libs that will be placed in the agent classloader, isolated from the instrumented application code
val javaagentLibs: Configuration by configurations.creating {
  isCanBeResolved = true
  isCanBeConsumed = false
}
// this configuration stores the upstream agent dep that"s extended by this project
val upstreamAgent: Configuration by configurations.creating {
  isCanBeResolved = true
  isCanBeConsumed = false
}

dependencies {
  javaagentLibs(project(":custom"))
  javaagentLibs(project(":custom:shared"))
  javaagentLibs(project(":instrumentation"))

  bootstrapLibs(project(":bootstrap"))
  bootstrapLibs("com.solarwinds.joboe:core")
  bootstrapLibs("com.solarwinds.joboe:metrics")

  upstreamAgent("io.opentelemetry.javaagent:opentelemetry-javaagent")
}

fun isolateClasses(jars: Iterable<File>): CopySpec {
  return copySpec {
    jars.forEach {
      from(zipTree(it)) {
        into("inst")
        rename("^(.*)\\.class\$", "\$1.classdata")
      }
    }
  }
}

tasks {
  // building the final javaagent jar is done in 3 steps:

  // 1. all distro specific javaagent libs are relocated
  val relocateJavaagentLibs by registering(ShadowJar::class) {
    configurations = listOf(javaagentLibs)

    duplicatesStrategy = DuplicatesStrategy.FAIL

    archiveFileName.set("javaagentLibs-relocated.jar")

    mergeServiceFiles()
    exclude("**/module-info.class")

    // exclude known bootstrap dependencies - they can"t appear in the inst/ directory
    dependencies {
      exclude("org.slf4j:slf4j-api")
      exclude("io.opentelemetry:opentelemetry-api")
      exclude("io.opentelemetry:opentelemetry-api-logs")
      exclude("io.opentelemetry:opentelemetry-context")
      exclude("io.opentelemetry:opentelemetry-semconv")
    }
  }

  // 2. the distro javaagent libs are then isolated - moved to the inst/ directory
  // having a separate task for isolating javaagent libs is required to avoid duplicates with the upstream javaagent
  // duplicatesStrategy in shadowJar won"t be applied when adding files with with(CopySpec) because each CopySpec has
  // its own duplicatesStrategy
  val isolateJavaagentLibs by registering(Copy::class) {
    dependsOn(relocateJavaagentLibs)
    with(isolateClasses(relocateJavaagentLibs.get().outputs.files))

    into(layout.buildDirectory.dir("isolated/javaagentLibs"))
  }

  // 3. the relocated and isolated javaagent libs are merged together with the bootstrap libs (which undergo relocation
  // in this task) and the upstream javaagent jar; duplicates are removed
  shadowJar {
    configurations = listOf(bootstrapLibs, upstreamAgent)
    dependsOn(isolateJavaagentLibs)
    from(isolateJavaagentLibs.get().outputs)

    archiveClassifier = null
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    mergeServiceFiles {
      include("inst/META-INF/services/*")
    }

    exclude("**/module-info.class")
    exclude("inst/com/solarwinds/opentelemetry/core/**")
    exclude("com/solarwinds/joboe/shaded/google/errorprone/annotations/**")

    manifest {
      attributes["Main-Class"] = "io.opentelemetry.javaagent.OpenTelemetryAgent"
      attributes["Agent-Class"] = "io.opentelemetry.javaagent.OpenTelemetryAgent"
      attributes["Premain-Class"] = "io.opentelemetry.javaagent.OpenTelemetryAgent"
      attributes["Can-Redefine-Classes"] = "true"
      attributes["Can-Retransform-Classes"] = "true"
      attributes["Implementation-Vendor"] = "SolarWinds Inc."
      attributes["Implementation-Version"] = swoAgentVersion
    }
  }

  assemble {
    dependsOn(shadowJar)
  }
}
