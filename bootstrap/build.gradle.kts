dependencies {
  compileOnly("com.solarwinds.joboe:config")
  compileOnly("com.solarwinds.joboe:logging")
  compileOnly("com.solarwinds.joboe:sampling")

  compileOnly("io.opentelemetry:opentelemetry-api")
  compileOnly("io.opentelemetry.semconv:opentelemetry-semconv")
  implementation("com.github.ben-manes.caffeine:caffeine")
}

swoJava {
  minJavaVersionSupported.set(JavaVersion.VERSION_1_8)
}
