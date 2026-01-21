plugins {
  id("solarwinds.instrumentation-conventions")
}

dependencies {
  compileOnly(project(":libs:shared"))
  compileOnly("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure")
}
