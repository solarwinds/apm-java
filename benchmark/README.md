This benchmark stack is based on the OpenTelemetry's benchmark-overhead test suite: https://github.com/open-telemetry/opentelemetry-java-instrumentation/tree/main/benchmark-overhead

Usually the benchmark is triggered manually as a workflow in the Github Actions. You can also run it locally though.

## How to Run the benchmark locally
### Prerequisites
 - A running docker service

### Steps
1. Define environment variables (`GP_USERNAME` and `GP_TOKEN`, which are the github account and personal access token you'd like to use. Pay attention that the token may be leaked if you don't protect it properly. You may consider creating a new token for the benchmark and destroying it after the local run.) 
2. Run `./gradlew test`
