plugins {
  id("solarwinds.instrumentation-conventions")
}

dependencies {
  compileOnly("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure")
}
