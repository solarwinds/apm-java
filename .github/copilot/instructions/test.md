# Test File Instructions

When writing test files in this repository:

## Test Framework
- Use codebase provided framework
- Always check the existing tests for reference

## Test File Naming
- Test class name: `{ClassUnderTest}Test.java`
- Example: `SolarwindsSampler.java` → `SolarwindsSamplerTest.java`

## Test Method Naming
- Use descriptive names with pattern: `should{ExpectedBehavior}When{StateOrCondition}`
- Alternative: `verify{What}Given{Condition}` or `return{What}Given{Condition}`
- Examples:
  - `shouldReturnTrueWhenConditionMet()`
  - `verifyThatLocalTraceDecisionMachineryIsUsedWhenSpanIsRoot()`
  - `returnSamplingResultGivenTraceDecisionIsSampled()`

## Mock Field Naming
- Pattern: `{descriptiveName}Mock`
- Examples: `traceDecisionMock`, `spanContextMock`, `metricExporterMock`
- **Tested class field name**: `tested` (not `testedClass` or other variations)


## Parameterized Tests
- Use `@ParameterizedTest` with `@MethodSource` for multiple test cases
- Provide descriptive test names using `@MethodSource` method name
- Example:
  ```java
  @ParameterizedTest
  @MethodSource("provideTestCases")
  void shouldHandleDifferentInputs(String input, String expected) {
    // test implementation
  }
  
  private static Stream<Arguments> provideTestCases() {
    return Stream.of(
      Arguments.of("input1", "expected1"),
      Arguments.of("input2", "expected2")
    );
  }
  ```

## Test Coverage Goals
- Write tests for all public methods
- Cover branches

## Test Organization
- Use `@BeforeEach` for common setup
- Use `@AfterEach` for cleanup
- Keep tests independent and isolated
- No shared state between tests
- No nested test classes
- No inheritance between test classes
- No use of reflection

