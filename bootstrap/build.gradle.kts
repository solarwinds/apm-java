plugins {
  id("solarwinds.java-conventions")
}

dependencies {
  compileOnly(project(":libs:config"))
  compileOnly(project(":libs:logging"))
  compileOnly(project(":libs:sampling"))

  compileOnly("io.opentelemetry:opentelemetry-api")
  compileOnly("io.opentelemetry.semconv:opentelemetry-semconv")
  implementation("com.github.ben-manes.caffeine:caffeine")
}

swoJava {
  minJavaVersionSupported.set(JavaVersion.VERSION_1_8)
}
