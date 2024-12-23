import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

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
  id("solarwinds.java-conventions")
  id("solarwinds.shadow-conventions")
  id("io.opentelemetry.instrumentation.muzzle-generation")
  id("io.opentelemetry.instrumentation.muzzle-check")
}

evaluationDependsOn(":testing:agent-for-testing")


val versions: Map<String, String> by rootProject.extra

dependencies {
  compileOnly("io.opentelemetry:opentelemetry-sdk:${versions["opentelemetry"]}")
  compileOnly("io.opentelemetry.instrumentation:opentelemetry-instrumentation-api:${versions["opentelemetryJavaagent"]}")
  compileOnly("io.opentelemetry.javaagent:opentelemetry-javaagent-extension-api:${versions["opentelemetryJavaagentAlpha"]}")

  compileOnly("net.bytebuddy:byte-buddy")
  compileOnly("com.google.auto.service:auto-service")
  annotationProcessor("com.google.auto.service:auto-service")


  // test dependencies
  testImplementation("io.opentelemetry.javaagent:opentelemetry-testing-common:${versions["opentelemetryJavaagentAlpha"]}")
  testImplementation("io.opentelemetry:opentelemetry-sdk-testing:${versions["opentelemetry"]}")
  testImplementation("org.assertj:assertj-core:3.19.0")

  add(
    "codegen",
    "io.opentelemetry.javaagent:opentelemetry-javaagent-tooling:${versions["opentelemetryJavaagentAlpha"]}"
  )
  add(
    "muzzleBootstrap",
    "io.opentelemetry.instrumentation:opentelemetry-instrumentation-annotations-support:${versions["opentelemetryJavaagentAlpha"]}"
  )
  add(
    "muzzleTooling",
    "io.opentelemetry.javaagent:opentelemetry-javaagent-extension-api:${versions["opentelemetryJavaagentAlpha"]}"
  )
  add(
    "muzzleTooling",
    "io.opentelemetry.javaagent:opentelemetry-javaagent-tooling:${versions["opentelemetryJavaagentAlpha"]}"
  )
}


tasks.named<ShadowJar>("shadowJar") {
  archiveFileName.set("agent-testing.jar")
  configurations = listOf(project.configurations.runtimeClasspath.get())
  mergeServiceFiles()
}

tasks.withType<Test>().configureEach {
  val shadowJar = tasks.shadowJar.get()
  val agentShadowJar = project(":testing:agent-for-testing").tasks.shadowJar

  dependsOn(shadowJar)
  dependsOn(agentShadowJar)

  inputs.file(shadowJar.archiveFile)
  jvmArgumentProviders.add(JavaagentProvider(agentShadowJar.flatMap { it.archiveFile }, shadowJar.archiveFile.get().asFile))

  // The sources are packaged into the testing jar so we need to make sure to exclude from the test
  // classpath, which automatically inherits them, to ensure our shaded versions are used.
  classpath = classpath.filter {
    if (layout.buildDirectory.file("resources/main") == it || layout.buildDirectory.file("classes/java/main") == it) {
      return@filter false
    }

    val lib = it.absoluteFile
    if (lib.name.startsWith("opentelemetry-javaagent-") && !lib.name.contains("extension-api")) {
      return@filter false
    }

    if (lib.name.startsWith("opentelemetry-") && lib.name.contains("-autoconfigure-")) {
      return@filter false
    }

    true
  }
}

tasks.register("generateInstrumentationVersionFile") {
  val name = "com.solarwinds.${project.name}"
  val version = rootProject.version.toString()
  inputs.property("instrumentation.name", name)

  inputs.property("instrumentation.version", version)
  val propertiesDir =
    layout.buildDirectory.dir("generated/instrumentationVersion/META-INF/io/opentelemetry/instrumentation")
  outputs.dir(propertiesDir)

  doLast {
    File(propertiesDir.get().asFile, "$name.properties").writeText("version=$version")
  }
}

sourceSets {
  main {
    output.dir("build/generated/instrumentationVersion", "builtBy" to "generateInstrumentationVersionFile")
  }
}

class JavaagentProvider(
  @InputFile
  @PathSensitive(PathSensitivity.RELATIVE)
  val agentJar: Provider<RegularFile>,

  @InputFile
  @PathSensitive(PathSensitivity.RELATIVE)
  val instrumentationJar: File,
) : CommandLineArgumentProvider {

  override fun asArguments(): Iterable<String> = listOf(
    "-javaagent:${file(agentJar).absolutePath}",
    "-Dotel.javaagent.experimental.initializer.jar=${instrumentationJar.absolutePath}",
    "-Dotel.javaagent.testing.additional-library-ignores.enabled=false",
    "-Dotel.javaagent.testing.fail-on-context-leak=true",
    "-Dsw.apm.service.key=gimme-a-token:test-app",
    "-Dsw.apm.sql.tag=true",
    "-Dsw.apm.sql.tag.prepared=true",
    // prevent sporadic gradle deadlocks, see SafeLogger for more details
    "-Dotel.javaagent.testing.transform-safe-logging.enabled=true",
    "--add-opens=java.base/java.lang=ALL-UNNAMED",
    "--add-opens=java.base/java.lang.invoke=ALL-UNNAMED",
    "-XX:+IgnoreUnrecognizedVMOptions",
    "-Dotel.javaagent.debug=true"
  )

}
