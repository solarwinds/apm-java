# Contributing

Pull requests for bug fixes are always welcome! Any security related issues please see [security](SECURITY.md)

Before submitting bug fix pull request, it is recommended to first
[open an issue](https://github.com/solarwinds/apm-java/issues/new)
and report the bug providing pertinent information like runtime environment specs and steps to reproduce the bug.

## Building

This project requires Java 17 to build and run tests. Newer JDK's may work, but this version is used in CI.

This project currently requires an internal library `com.solarwinds.joboe:<module>:x.x.x`. So building the agent maybe impossible in the interim. We plan to open source the library eventually after internal risk assessment.

### Building from source

Build using Java 17:

```bash
java -version
```

```bash
./build.sh
```

and then you can find the java agent artifact at

`agent/build/libs/solarwinds-apm-agent.jar` for the agent that exports telemetry via [apm-proto](https://github.com/solarwinds/apm-proto) and `agent-lambda/build/libs/solarwinds-apm-agent-lambda.jar` for the agent that exports telemetry via [OTLP](https://opentelemetry.io/docs/specs/otel/protocol/) and is built to run in aws lambda.


## Style guide

See [Style guide](https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/main/docs/contributing/style-guideline.md)

We have adopted the upstream style guide

## Running the tests

```bash
./gradlew test
```

Runs unit test.

See [smoke test](smoke-tests/README.md) for integration tests

## Writing instrumentation

Please contribute your instrumentation and general improvements to [upstream](https://github.com/open-telemetry/opentelemetry-java-instrumentation)
