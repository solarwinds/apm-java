plugins {
  `kotlin-dsl`
  id("com.diffplug.spotless") version "6.25.0"
}

spotless {
  kotlinGradle {
    ktlint().editorConfigOverride(
      mapOf(
        "indent_size" to "2",
        "continuation_indent_size" to "2",
        "max_line_length" to "160",
        "insert_final_newline" to "true",
        // ktlint does not break up long lines, it just fails on them
        "ktlint_standard_max-line-length" to "disabled",
        // ktlint makes it *very* hard to locate where this actually happened
        "ktlint_standard_trailing-comma-on-call-site" to "disabled",
        // depends on ktlint_standard_wrapping
        "ktlint_standard_trailing-comma-on-declaration-site" to "disabled",
        // also very hard to find out where this happens
        "ktlint_standard_wrapping" to "disabled"
      )
    )
    target("**/*.gradle.kts")
  }
}

tasks.named("assemble").configure {
  dependsOn(tasks.named("spotlessApply"))
}

tasks.named("check").configure {
  dependsOn(tasks.named("spotlessCheck"))
}

repositories {
  gradlePluginPortal()
  maven {
    url = uri("https://oss.sonatype.org/content/repositories/snapshots")
  }
}

dependencies {
  implementation(gradleApi())
  implementation("com.diffplug.spotless:spotless-plugin-gradle:6.25.0")

  implementation("io.opentelemetry.instrumentation:gradle-plugins:2.10.0-alpha")
  implementation("com.gradleup.shadow:shadow-gradle-plugin:8.3.5")
  implementation("com.github.gmazzo.buildconfig:com.github.gmazzo.buildconfig.gradle.plugin:5.5.1")
}
