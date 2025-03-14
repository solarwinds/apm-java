plugins {
  id("solarwinds.instrumentation-conventions")
}

dependencies {
  implementation(project(":instrumentation:hibernate:hibernate-shared"))
  implementation(project(":instrumentation:instrumentation-shared"))

  compileOnly("org.hibernate:hibernate-core:6.0.0.Final")
  compileOnly("com.solarwinds.joboe:logging")

  compileOnly("io.opentelemetry:opentelemetry-sdk-trace")
  compileOnly("io.opentelemetry.semconv:opentelemetry-semconv")

  testImplementation("org.hibernate:hibernate-core:6.0.0.Final")
  testImplementation("com.h2database:h2:2.3.232")
}

tasks.withType<JavaCompile>().configureEach {
  dependsOn(":instrumentation:hibernate:hibernate-shared:byteBuddyJava")
}

swoJava {
  minJavaVersionSupported.set(JavaVersion.VERSION_11)
}
