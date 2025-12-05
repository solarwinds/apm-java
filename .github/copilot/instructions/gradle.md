# Gradle File Instructions

When working with Gradle build files (*.gradle.kts) in this repository:

## File Format
- Use Kotlin DSL (`.gradle.kts`) for all Gradle files
- All Gradle files start with the SolarWinds license header

## Dependency Management
- Centralized dependency versions in `dependencyManagement` module
- Use `compileOnly` for dependencies provided at runtime by the agent
- Use `implementation` for dependencies packaged with the module
- Use `testImplementation` for test-only dependencies

## Java Version Configuration
- Use `swoJava` extension to set minimum Java version
- Default minimum: Java 8 (VERSION_1_8)
- Example:
  ```kotlin
  swoJava {
    minJavaVersionSupported.set(JavaVersion.VERSION_1_8)
  }
  ```

## Convention Plugins
- Apply common conventions via buildSrc plugins:
  - `solarwinds.java-conventions` - Java compilation and testing
  - `solarwinds.spotless-conventions` - Code formatting
  - `solarwinds.shadow-conventions` - JAR shading
  - `solarwinds.instrumentation-conventions` - Instrumentation modules


## Version Properties
- Agent version defined in root `build.gradle.kts`

