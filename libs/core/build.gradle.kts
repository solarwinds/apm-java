plugins {
  id("solarwinds.java-conventions")
}

description = "core"

dependencies {
  implementation(project(":libs:logging"))
  implementation(project(":libs:config"))
  implementation(project(":libs:sampling"))

  implementation("io.grpc:grpc-netty:1.80.0")
  implementation("io.grpc:grpc-stub:1.80.0")
  implementation("io.grpc:grpc-protobuf:1.80.0")

  compileOnly("org.json:json")
  compileOnly("io.opentelemetry:opentelemetry-api")
  compileOnly("io.opentelemetry:opentelemetry-context")

  implementation("javax.xml.bind:jaxb-api:2.3.1")

  implementation("com.solarwinds:apm-proto:1.0.8") {
    exclude(group = "com.google.guava", module = "guava")
    exclude(group = "io.grpc")
  }

  compileOnly("com.google.auto.service:auto-service")
  annotationProcessor("com.google.auto.service:auto-service")

  testImplementation("org.json:json")
  testImplementation("io.opentelemetry:opentelemetry-api")
  testImplementation("io.opentelemetry:opentelemetry-context")
}

sourceSets {
  main { java { exclude("**/*.template") } }
}

tasks.withType<Javadoc>().configureEach {
  exclude("**/*.template")
}

tasks.named<JavaCompile>("compileJava") {
  // Disable AutoService verify check to prevent rawtypes warnings for generic service provider interfaces
  options.compilerArgs.add("-Averify=false")
}

tasks.withType<JavaCompile>().configureEach {
  // Suppress warnings from migrated/vendored legacy code (hdrHistogram, ebson, etc.)
  options.compilerArgs.addAll(
    listOf(
      "-Xlint:-serial",
      "-Xlint:-rawtypes",
      "-Xlint:-dep-ann",
      "-Xlint:-deprecation"
    )
  )
}
