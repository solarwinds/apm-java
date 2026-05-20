plugins {
  id("solarwinds.instrumentation-conventions")
}

dependencies {
  compileOnly(project(":libs:shared"))
  compileOnly(project(":libs:core"))
  compileOnly(project(":libs:sampling"))
  compileOnly(project(":bootstrap"))
  compileOnly("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure")
  compileOnly("io.opentelemetry.javaagent:opentelemetry-javaagent-extension-api")
}
