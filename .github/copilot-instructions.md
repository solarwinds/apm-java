# GitHub Copilot Instructions

## Priority Guidelines

When generating code for this repository:

1. **Version Compatibility**: Always respect Java 8 as the minimum supported version
2. **Context Files**: Prioritize patterns and standards defined in the `.github/copilot/instructions/` directory
3. **Codebase Patterns**: When context files don't provide specific guidance, scan the codebase for established patterns
4. **Architectural Consistency**: Maintain the multi-module Gradle architecture and OpenTelemetry extension patterns
5. **Code Quality**: Prioritize maintainability, security, and testability in all generated code

## Technology Stack

### Core Technologies
- **Languages**: Java (minimum Java 8), Kotlin (for Gradle build scripts)
- **Build System**: Gradle 8.x with Kotlin DSL
- **Framework**: OpenTelemetry Java Instrumentation
- **Testing**: JUnit, Mockito


## Project Architecture

### Module Structure
This is a **multi-module Gradle project** with the following architecture:

- **agent/** - Main Java agent for standard deployments (exports via apm-proto)
- **agent-lambda/** - Lambda-specific agent (exports via OTLP)
- **bootstrap/** - Bootstrap classes loaded early in JVM lifecycle
- **custom/** - Custom SolarWinds extensions and configurations
  - `custom/shared/` - Shared code between agent and agent-lambda
  - `custom/lambda/` - Lambda-specific extensions
- **instrumentation/** - Custom instrumentation modules
  - `jdbc/` - JDBC instrumentation
  - `spring-webmvc/` - Spring WebMVC instrumentation
  - `hibernate/` - Hibernate instrumentation
  - `servlet-3.0/`, `servlet-5.0/` - Servlet instrumentation
  - `instrumentation-shared/` - Shared instrumentation utilities
- **solarwinds-otel-sdk/** - Standalone SDK module
- **testing/** - Test utilities and extensions
- **buildSrc/** - Gradle convention plugins
- **dependencyManagement/** - Centralized dependency version management

This structure is subject to change. When in doubt, follow existing module patterns.

## Context Files

Reference these files in `.github/copilot/instructions/` for file-type specific guidance:

- **java.md** - Java code style, naming conventions, and patterns
- **test.md** - Test structure and patterns
- **gradle.md** - Gradle build file conventions

## Code Quality Standards

### Maintainability
- Write self-documenting code with clear, descriptive names
- Follow the naming conventions defined in context files
- Keep methods focused on single responsibilities
- Limit method complexity (avoid deeply nested logic)
- Extract magic numbers and strings to named constants

### Java 8 Compatibility
**CRITICAL**: This project supports Java 8 as the minimum version.

**Never use these Java 9+ features**:
- `var` keyword (use explicit types)
- Text blocks (use string concatenation or `String.format()`)
- Records (use regular classes)
- Sealed classes (use regular class hierarchies)
- Pattern matching (use instanceof with explicit casts)
- Switch expressions (use traditional switch statements)
- Private interface methods
- HTTP Client API (use existing HTTP libraries)
- Compact number formatting
- Stream API enhancements from Java 9+


### Security
- Validate and sanitize all external input (configuration files, environment variables)
- Avoid logging sensitive data (API keys, tokens, credentials)

### Testability
- Design classes with testing in mind (prefer composition to inheritance)
- Use constructor injection for dependencies (enables easy mocking)
- Keep business logic separate from I/O operations
- Write tests for all public methods

## Documentation Requirements

### JavaDoc Standards
Based on the codebase pattern, provide comprehensive documentation:

```java
/**
 * Sampler that uses trace decision logic from our joboe core (consult local and remote settings)
 *
 * <p>Also inject various Solarwinds specific sampling KVs into the {@code SamplingResult}
 */
public class SolarwindsSampler implements Sampler {
  /**
   * Converts a trace decision into an OpenTelemetry SamplingResult.
   *
   * @param traceDecision the trace decision from joboe core
   * @param xTraceOptions X-Trace options from the request
   * @param isParentRemote whether the parent span is from a remote process
   * @return the corresponding SamplingResult
   */
  public SamplingResult toOtSamplingResult(
      TraceDecision traceDecision, 
      XTraceOptions xTraceOptions, 
      boolean isParentRemote) {
    // implementation
  }
}
```

### Documentation Requirements
- Document all public classes with purpose and usage
- Document all public methods with parameters, return values, and exceptions
- Include `@param` for all parameters
- Include `@return` for non-void methods
- Include `@throws` for checked exceptions
- Use `{@code}` for inline code references
- Use `<p>` tags to separate paragraphs in JavaDoc

### Code Comments
- Use comments sparingly and reserve them for obtuse codes

## Testing Approach

### Unit Testing Standards
- Follow existing unit test structure and style

### Integration Testing
- Follow same pattern in `instrumentation/` modules for integration tests


## Code Formatting and Style

### Running Formatters
```bash
./gradlew spotlessApply  # Apply formatting
./gradlew spotlessCheck  # Check formatting
```

## Project-Specific Guidance

### Building the Project
- Requires Java 17 to build (toolchain configured)
- Supports Java 8+ at runtime
- Build command: `./build.sh` or `./gradlew build`
- Run tests: `./gradlew test`
- Format code: `./gradlew spotlessApply`

### When in Doubt
- **Consistency over convention**: Match existing code patterns over external best practices
- **Scan similar files**: Look for files with similar functionality as templates
- **Java 8 compatibility**: When unsure, use more verbose Java 8 compatible syntax
- **Ask before breaking changes**: Significant architectural changes should be discussed

## Additional Resources

- [OpenTelemetry Java Instrumentation](https://github.com/open-telemetry/opentelemetry-java-instrumentation)
- [SolarWinds APM Documentation](https://documentation.solarwinds.com/en/success_center/observability/content/configure/services/java/java.htm)
- [Project README](../README.md)
- [Contributing Guide](../CONTRIBUTING.md)

