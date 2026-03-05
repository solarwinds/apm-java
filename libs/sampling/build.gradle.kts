plugins {
  id("solarwinds.java-conventions")
}

description = "sampling"

dependencies {
  implementation(project(":libs:logging"))
  compileOnly("io.opentelemetry:opentelemetry-api")
  implementation("com.github.ben-manes.caffeine:caffeine")

  testImplementation("io.opentelemetry:opentelemetry-api")
}
