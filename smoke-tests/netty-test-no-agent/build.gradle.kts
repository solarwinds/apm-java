import java.net.URI

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
}

group = "com.solarwinds"
version = "unspecified"

repositories {
    mavenCentral()
    maven {
        url = URI.create("https://s01.oss.sonatype.org/content/repositories/snapshots/")
        credentials {
            username = System.getenv("SONATYPE_USERNAME")
            password = System.getenv("SONATYPE_TOKEN")
        }
    }
}

val sdkVersion = System.getenv("AGENT_VERSION")?.also { println("Using SDK version: $it") } ?: "2.6.0"
dependencies {
    implementation("io.netty:netty-common:4.1.94.Final")
    implementation("io.github.appoptics:solarwinds-otel-sdk:$sdkVersion-SNAPSHOT")
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}

application {
    mainClass.set("com.solarwinds.netty.NettyApp")
}
