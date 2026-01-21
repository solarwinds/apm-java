plugins {
  `java-platform`
}

val otelAgentVersion = "2.23.0"
val otelSdkVersion = "1.57.0"

val mockitoVersion = "4.11.0"
val byteBuddyVersion = "1.15.10"
val joboeVersion = "10.0.27"

val opentelemetryJavaagentAlpha = "$otelAgentVersion-alpha"
val opentelemetryAlpha = "$otelSdkVersion-alpha"
val opentelemetrySemconv = "1.37.0"
val opentelemetrySemconvAlpha = "$opentelemetrySemconv-alpha"

val autoservice = "1.1.1"
val otelJavaContribVersion = "1.52.0-alpha"
val junit5 = "5.9.2"

rootProject.extra["otelAgentVersion"] = otelAgentVersion
rootProject.extra["otelSdkVersion"] = otelSdkVersion

javaPlatform {
  allowDependencies()
}

dependencies {
  constraints {
    api("org.mockito:mockito-core:$mockitoVersion")
    api("org.mockito:mockito-junit-jupiter:$mockitoVersion")
    api("org.mockito:mockito-inline:$mockitoVersion")

    api("javax.annotation:javax.annotation-api:1.3.2")
    api("org.junit.jupiter:junit-jupiter-api:$junit5")
    api("org.junit.jupiter:junit-jupiter-engine:$junit5")

    api("io.opentelemetry:opentelemetry-api:$otelSdkVersion")
    api("io.opentelemetry.javaagent:opentelemetry-javaagent:$otelAgentVersion")
    api("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure:$otelSdkVersion")

    api("io.opentelemetry.instrumentation:opentelemetry-instrumentation-api:$otelAgentVersion")
    api("io.opentelemetry.javaagent:opentelemetry-javaagent-tooling:$opentelemetryJavaagentAlpha")
    api("io.opentelemetry.javaagent:opentelemetry-javaagent-extension-api:$opentelemetryJavaagentAlpha")

    api("io.opentelemetry.instrumentation:opentelemetry-instrumentation-api-incubator:$opentelemetryJavaagentAlpha")
    api("io.opentelemetry.javaagent:opentelemetry-agent-for-testing:$opentelemetryJavaagentAlpha")
    api("io.opentelemetry.javaagent:opentelemetry-javaagent-bootstrap:$opentelemetryJavaagentAlpha")

    api("io.opentelemetry.javaagent:opentelemetry-testing-common:$opentelemetryJavaagentAlpha")
    api("io.opentelemetry.instrumentation:opentelemetry-instrumentation-annotations-support:$opentelemetryJavaagentAlpha")
    api("io.opentelemetry:opentelemetry-sdk-testing:$otelSdkVersion")

    api("io.opentelemetry:opentelemetry-sdk:$otelSdkVersion")
    api("io.opentelemetry.semconv:opentelemetry-semconv:$opentelemetrySemconv")
    api("org.junit-pioneer:junit-pioneer:2.1.0")

    api("net.bytebuddy:byte-buddy:${byteBuddyVersion}")
    api("com.google.auto.service:auto-service:$autoservice")

    api("org.projectlombok:lombok:1.18.42")
    api("com.solarwinds.joboe:core:$joboeVersion")
    api("com.solarwinds.joboe:metrics:$joboeVersion")

    api("com.solarwinds.joboe:config:$joboeVersion")
    api("com.solarwinds.joboe:logging:$joboeVersion")
    api("com.solarwinds.joboe:sampling:$joboeVersion")

    api("org.json:json:20250517")
    api("com.google.code.gson:gson:2.10.1")
    api("com.github.ben-manes.caffeine:caffeine:2.9.3")

    api("com.google.code.findbugs:annotations:3.0.1u2")
    api("io.opentelemetry.contrib:opentelemetry-span-stacktrace:$otelJavaContribVersion")
    api("io.opentelemetry.semconv:opentelemetry-semconv-incubating:$opentelemetrySemconvAlpha")

    api("io.opentelemetry:opentelemetry-api-incubator:$opentelemetryAlpha")
    api("io.opentelemetry:opentelemetry-exporter-otlp:$otelSdkVersion")
    api("io.opentelemetry:opentelemetry-sdk-extension-incubator:$opentelemetryAlpha")

    api("org.junit.jupiter:junit-jupiter-params:$junit5")

  }
}