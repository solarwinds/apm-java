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

plugins{
    id("io.github.gradle-nexus.publish-plugin") version "2.0.0"
}

val swoAgentVersion = "2.11.0"
extra["swoAgentVersion"] = swoAgentVersion
group = "io.github.appoptics"
version = if (System.getenv("SNAPSHOT_BUILD").toBoolean()) "$swoAgentVersion-SNAPSHOT" else swoAgentVersion

subprojects {
    if (this.name != "dependencyManagement") {
        apply(plugin = "solarwinds.spotless-conventions")
        apply(plugin = "solarwinds.java-conventions")
    }
}

nexusPublishing {
    repositories {
        sonatype {
            password = System.getenv("SONATYPE_TOKEN")
            username = System.getenv("SONATYPE_USERNAME")

            nexusUrl = uri("https://s01.oss.sonatype.org/service/local/")
            snapshotRepositoryUrl = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
        }
    }
}