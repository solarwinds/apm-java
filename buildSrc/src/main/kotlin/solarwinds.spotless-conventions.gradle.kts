import com.diffplug.gradle.spotless.SpotlessTask
import com.github.gmazzo.buildconfig.BuildConfigTask

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
  id("com.diffplug.spotless")
  id("com.github.gmazzo.buildconfig")
}


spotless {
  java {
    target("src/*/java/**/*.java")
    googleJavaFormat()
    formatAnnotations()
  }

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
  }

  format("misc") {
    // not using "**/..." to help keep spotless fast
    target(
      ".gitignore",
      ".editorconfig",
      "*.md",
      "*.sh"
    )
    indentWithSpaces()
    trimTrailingWhitespace()
    endWithNewline()
  }
}


tasks.named("check").configure {
  dependsOn(tasks.named("spotlessCheck"))
}
