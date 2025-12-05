# Java File Instructions

When working with Java files in this repository:

## Java Version Compatibility
- Use: traditional try-catch, explicit types, classic class definitions
- Avoid: var keyword, text blocks, records, sealed classes, pattern matching
- Do NOT use Java 9+ features unless module explicitly supports higher version
- **Minimum supported: Java 8** (enforced in build configuration)

## Logging
- Exception: SDK module uses `java.util.logging.Logger`
- Logger field: `private static final Logger logger = LoggerFactory.getLogger();`
- Use `com.solarwinds.joboe.logging.Logger` for custom code


## Code Style
- Use 2 new lines between field assignment/declaration at class.
- Use 2 new lines between every 3 statements/expressions. A single statement/expression that spans more than 3 lines should be separated from the following statement or expression by 2 new lines.
- Use `./gradlew spotlessApply` to format code
- Follow standard Java conventions (naming, braces, indentation, etc.)



