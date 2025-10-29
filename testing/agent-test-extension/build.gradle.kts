plugins {
  id("solarwinds.instrumentation-conventions")
}

dependencies {
  compileOnly(project(":custom:shared"))
  compileOnly("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure")
}
