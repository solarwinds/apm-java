plugins {
  id("solarwinds.instrumentation-conventions")
}

dependencies {
  implementation(project(":instrumentation:instrumentation-shared"))
  compileOnly("io.opentelemetry:opentelemetry-sdk-trace")
  compileOnly("io.opentelemetry.semconv:opentelemetry-semconv")
}

tasks.withType<JavaCompile>().configureEach {
  dependsOn(":instrumentation:instrumentation-shared:byteBuddyJava")
}

swoJava {
  minJavaVersionSupported.set(JavaVersion.VERSION_1_8)
}
