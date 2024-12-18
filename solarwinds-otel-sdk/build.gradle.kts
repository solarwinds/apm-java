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
  id("signing")
  id("maven-publish")
  id("solarwinds.java-conventions")
  id("solarwinds.shadow-conventions")
}

base.archivesName = "solarwinds-otel-sdk"
val swoAgentVersion: String by rootProject.extra

dependencies {
  compileOnly(project(":bootstrap"))
  compileOnly("io.opentelemetry:opentelemetry-sdk")

  compileOnly("io.opentelemetry:opentelemetry-api")
  compileOnly("io.opentelemetry:opentelemetry-context")
  compileOnly("io.opentelemetry.javaagent:opentelemetry-javaagent-bootstrap")
}

tasks {
  shadowJar {
    archiveClassifier = null
  }
}

publishing {
  publications {
    register<MavenPublication>("mavenJava") {
      pom {
        name.set(base.archivesName)
        description.set("Solarwinds Java Instrumentation libraries.")
        url.set("www.solarwinds.com")

        scm {
          connection.set("scm:git:git@github.com:solarwinds/apm-java.git")
          developerConnection.set("scm:git:git@github.com:solarwinds/apm-java.git")
          url.set("git@github.com:solarwinds/apm-java.git")
        }

        developers {
          developer {
            id.set("APM")
            name.set("The APM Library Team")
          }
        }

        licenses {
          license {
            name.set("Apache License, Version 2.0")
          }
        }

        groupId = "io.github.appoptics"
        artifactId = base.archivesName.get()
        val snapshotVersion = System.getenv("AGENT_VERSION")

        version = if (System.getenv("SNAPSHOT_BUILD").toBoolean()) "$snapshotVersion-SNAPSHOT" else swoAgentVersion
        from(components["java"])
      }
    }
  }
}

signing {
  setRequired {
    gradle.taskGraph.allTasks.any { (it.javaClass == PublishToMavenRepository::class) }
  }

  val signingKey = System.getenv("GPG_PRIVATE_KEY")
  val signingPassword = System.getenv("GPG_PRIVATE_KEY_PASSPHRASE")
  useInMemoryPgpKeys(signingKey, signingPassword)

  sign(publishing.publications["mavenJava"])
}

swoJava {
  minJavaVersionSupported.set(JavaVersion.VERSION_1_8)
}
