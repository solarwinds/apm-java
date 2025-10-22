plugins {
  id("solarwinds.instrumentation-conventions")
}

dependencies {
  implementation(project(":instrumentation:hibernate:hibernate-shared"))
  implementation(project(":instrumentation:instrumentation-shared"))

  compileOnly("org.hibernate:hibernate-core:7.1.4.Final")
  compileOnly("io.opentelemetry:opentelemetry-sdk-trace")
  compileOnly("io.opentelemetry.semconv:opentelemetry-semconv")

  testImplementation("org.hibernate:hibernate-core:7.1.4.Final")
  testImplementation("com.h2database:h2:2.3.232")
}

tasks.withType<JavaCompile>().configureEach {
  dependsOn(":instrumentation:hibernate:hibernate-shared:byteBuddyJava")
}

swoJava {
  minJavaVersionSupported.set(JavaVersion.VERSION_1_8)
}
