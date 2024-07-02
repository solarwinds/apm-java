import com.jayway.jsonpath.JsonPath
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

/*
 * © SolarWinds Worldwide, LLC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

plugins {
    java
    application
    id("de.undercouch.download") version "5.5.0"
}

buildscript {
  dependencies {
    classpath("com.jayway.jsonpath:json-path:2.9.0")
  }
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

fun getAgentUrlFromGitRelease(): String {
  val httpClient = HttpClient.newHttpClient()
  // Environment variables set by pipeline
  val agentVersion = System.getenv("AGENT_VERSION")
  val commitHash = System.getenv("AGENT_COMMIT_HASH")

  val version = "v$agentVersion.$commitHash" // The format used by pipeline to name test pre-releases
  val tagRequest = HttpRequest.newBuilder()
    .uri(URI.create("https://api.github.com/repos/solarwinds/apm-java/releases/tags/$version"))
    .header("Authorization", "Bearer " + System.getenv("GITHUB_TOKEN"))
    .header("Accept", "application/vnd.github.v3+json")
    .GET().build()

  val tagResponse = httpClient.send(tagRequest, HttpResponse.BodyHandlers.ofString())
  val releaseId = JsonPath.parse(tagResponse.body()).read<Int>("$.id")
  val request = HttpRequest.newBuilder()
    .uri(URI.create("https://api.github.com/repos/solarwinds/apm-java/releases/$releaseId"))
    .header("Authorization", "Bearer " + System.getenv("GITHUB_TOKEN"))
    .header("Accept", "application/vnd.github.v3+json")
    .GET().build()

  val httpResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
  // We're grabbing the first asset url because the pipeline only uploads the agent jar and nothing else.
  return JsonPath.parse(httpResponse.body()).read("$.assets[0].url")
}

application {
    mainClass.set("com.solarwinds.netty.NettyApp")
    applicationDefaultJvmArgs = listOf("-javaagent:${getAgentPath(swoAgentPath)}")
}

tasks.register<de.undercouch.gradle.tasks.download.Download>("downloadSwoAgent") {
    doNotTrackState("Runs everytime because new build needs to be downloaded")
    src(getAgentUrlFromGitRelease())
    dest(swoAgentPath)

    overwrite(true)
    header("Authorization", "Bearer " + System.getenv("GITHUB_TOKEN"))
    header("Accept", "application/octet-stream")
}

tasks.named("run", JavaExec::class) {
    setEnvironment("SW_APM_SERVICE_KEY" to "${System.getenv("SW_APM_SERVICE_KEY")}:netty-pipeline-test-app")
    dependsOn("downloadSwoAgent")
}
