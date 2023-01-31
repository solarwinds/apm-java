This benchmark stack is based on the OpenTelemetry's benchmark-overhead test suite: https://github.com/open-telemetry/opentelemetry-java-instrumentation/tree/main/benchmark-overhead

## What we have customized
We added the NH/AO agent to the benchmark stack, which makes it easier to compare the performance and resources consumption of the Otel agent, the NH/AO agent and no agent.


## How to run the benchmark
Usually the benchmark is triggered manually as a workflow in the Github Actions. You can also run it locally though.

### How to run the benchmark via Github Actions
Go to `Actions` and run the `Benchmark` workflow.

### How to run the benchmark locally
#### Prerequisites
 - A running docker service.
 - The PAT of a Github account that has access to the org `librato` and `appoptics`.

#### Steps
1. Define environment variables (`GP_USERNAME` and `GP_TOKEN`, which are the github account and personal access token you'd like to use. Pay attention that the token may be leaked if you don't protect it properly. You may consider creating a new token for the benchmark and destroying it after the local run.) 
2. Run `./gradlew test`.

### Where are the test results
#### The summary of the latest run
Go to https://github.com/appoptics/solarwinds-apm-java/blob/benchmark-results/benchmark/results/release/summary.txt

#### The metrics of all the recent runs
Check out this file: https://github.com/appoptics/solarwinds-apm-java/blob/benchmark-results/benchmark/results/release/results.csv

#### The report of the latest run
Under this directory: https://github.com/appoptics/solarwinds-apm-java/tree/benchmark-results/benchmark/build/reports/tests/test

