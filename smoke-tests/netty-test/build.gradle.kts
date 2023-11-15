plugins {
    java
    application
    id("de.undercouch.download") version "5.5.0"
}

group = "com.solarwinds"
version = "unspecified"

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.netty:netty-common:4.1.94.Final")
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}

val swoAgentPath = project.buildDir.toString() + "/swo/solarwinds-apm-agent.jar"

fun getAgentPath(downloadPath: String) = if (System.getenv("AGENT_PATH") == null) downloadPath
else System.getenv("AGENT_PATH")

application {
    mainClass.set("com.solarwinds.netty.NettyApp")
    applicationDefaultJvmArgs = listOf("-javaagent:${getAgentPath(swoAgentPath)}")
}

tasks.register<de.undercouch.gradle.tasks.download.Download>("downloadSwoAgent") {
    doNotTrackState("Runs everytime since new build needs to be downloaded")
    src(System.getenv("AGENT_DOWNLOAD_URL"))
    dest(swoAgentPath)
    overwrite(true)
}

tasks.named("run", JavaExec::class) {
    setEnvironment("SW_APM_SERVICE_KEY" to "${System.getenv("SW_APM_SERVICE_KEY")}:netty-pipeline-test-app")
    dependsOn("downloadSwoAgent")
}