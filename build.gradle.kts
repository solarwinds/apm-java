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

val swoAgentVersion = "3.1.3"
extra["swoAgentVersion"] = swoAgentVersion
group = "com.solarwinds"
version = if (System.getenv("SNAPSHOT_BUILD").toBoolean()) "$swoAgentVersion-SNAPSHOT" else swoAgentVersion

allprojects {
    plugins.withId("java"){
        val cleanListedDependencies by tasks.registering {
            val vulnerableDependencies = listOf(
                "org.springframework:spring-webmvc:*",
                "org.apache.struts:*:*"
            )

            doLast {
                val gradleCache = System.getProperty("user.home") + "/.gradle/caches/modules-2/files-2.1"
                val mavenLocal = System.getProperty("user.home") + "/.m2/repository"

                vulnerableDependencies.forEach { dep ->
                    val (group, name, version) = dep.split(":")
                    val groupPath = group.replace(".", "/")

                    if (name == "*") {
                        // Delete all dependencies under this group
                        val gradleGroupPath = file("$gradleCache/$group")
                        val mavenGroupPath = file("$mavenLocal/$groupPath")

                        if (gradleGroupPath.exists()) {
                            gradleGroupPath.deleteRecursively()
                        }

                        if (mavenGroupPath.exists()) {
                            mavenGroupPath.deleteRecursively()
                        }

                    } else {
                        val gradlePath = file("$gradleCache/$group/$name")
                        val mavenPath = file("$mavenLocal/$groupPath/$name")

                        if (version == "*") {
                            // Delete all versions of a specific artifact
                            if (gradlePath.exists()) {
                                gradlePath.deleteRecursively()
                            }

                            if (mavenPath.exists()) {
                                mavenPath.deleteRecursively()
                            }

                        } else {
                            // Delete only a specific version
                            val gradleVersionPath = file("$gradlePath/$version")
                            val mavenVersionPath = file("$mavenPath/$version")
                            if (gradleVersionPath.exists()) {
                                gradleVersionPath.deleteRecursively()
                            }

                            if (mavenVersionPath.exists()) {
                                mavenVersionPath.deleteRecursively()
                            }
                        }
                    }
                }
            }
        }

        tasks.withType<Test> {
            finalizedBy(tasks.named("cleanListedDependencies"))
        }
    }
}

nexusPublishing {
    repositories {
        sonatype {
            password = System.getenv("CENTRAL_TOKEN")
            username = System.getenv("CENTRAL_USERNAME")

            nexusUrl = uri("https://ossrh-staging-api.central.sonatype.com/service/local/")
            snapshotRepositoryUrl = uri("https://central.sonatype.com/repository/maven-snapshots/")
        }
    }
}