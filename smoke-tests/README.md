# Java APM Integration test(Smoke test)
This README aims to document how the integration test works and should be used as a reference for understanding what's going on at a high level.

## Directory descriptions
### k6: 

This directory contains two Javascript files that are used by the `grafana/k6` container to generate load to the applications under test and also performs validation of the tests by querying `https://my.na-01.cloud.solarwinds.com/common/graphql` endpoint for exported traces and metrics. The `basic.js` is the entrypoint of the test and contains the test and validate code. It implements and uses `grafana/k6` api. The api requires a single default function to be exposed via Javascript ESM `export` directive. The rest of the functions are helper functions that performs a particular test and validation. The names of the function were named to reflect what they're testing and validating. However, name alone might not reflect properly what its doing. The `names.js` file contains other helper code needed to for the tests.

### netty-test

This directory contains the code for a basic `netty` app that prints the number of interfaces on the host machine, sleeps for some time then exit. The purpose of the application to ensure that an application with the SWO agent installed starts up without a crash both on windows and linux hosts. This test makes sure that code changes in the agent are using platform independent Java APIs.

### spring-boot-webmvc

This directory contains code for an application that is used to generate distributed trace request. This ensures we're able to confirm that context propagation is working as expected and the custom transaction naming via SWO SDK is also working as expected. The application receives a request at its `greet/{name}` and turns the `name` path variable into a custom transaction name. While a request to the `distributed` endpoint causes a request to be initiated to the `petclinic` container thereby generating a distributed trace.

### src

This directory contains the test machinery code and the machinery is held together by the Java [testcontainers](https://testcontainers.com/) framework. The `main` subdirectory is empty as test is the only thing we care about here. The `test` subdirectory follows normal Java source directory convention. There two test classes `LambdaTest` and `SmokeTest` which test aws lambda specific features and core agent features respectively. There are also four other packages within the root package `com.solarwinds`. These include `agents`, `config`, `containers`, `results` and `utils`. We'll describe each next.

- **agent**: This package contains code that are used to download the agent jar. The implemented `AgentResolver`, `SwoAgentResolver` downloads the release candidate agent from the stage bucket. A new resolver can be implemented to download the agent from another location when the need arises.
- **config**: This package contains code that holds configuration data for the `grafana/k6` container. However, the data is not being used currently as the configuration is now set directly in [basic.js](k6/basic.js) script. The usefulness of the config now is that it delineates lambda and base agent test.
- **containers**: This package contains code that uses the [testcontainers](https://testcontainers.com/) apis to create containers used for running tests. All the container images, but `smt:webmvc` are publicly available. The `smt:webmvc` is build in the pipeline because it's based on the `spring-boot-webmvc` application.
- **results**: This package contains code for interpreting `k6` exported test results.
- **util**; This package contains utility classes for specifying results' file paths in and outside container and for capturing container logs.

## Required environment variables for test
- `SWO_HOST_URL`: This is swo endpoint where data can be viewed. The default is `https://my.na-01.cloud.solarwinds.com`
- `SWO_LOGIN_URL`: This the login URL used to get temporary credentials. The default is `https://swo.cloud.solarwinds.com/v1/login`.
- `SW_APM_COLLECTOR`: The collector endpoint. The default is `apm.collector.na-01.cloud.solarwinds.com`
- `SW_APM_SERVICE_KEY`: The SWO collector service key without the service name. So the API token only. The service name is set in the test.
- `SW_APM_SERVICE_KEY_AO`: Same as above, however for AppOptics
- `OTEL_EXPORTER_OTLP_ENDPOINT`: The SWO otel collector endpoint. The default is `https://otel.collector.na-01.cloud.solarwinds.com`
- `SWO_EMAIL`: The swo user email used to get temporary login credentials.
- `SWO_PWORD`: The swo user password.

## Viewing the data in SWO
To view test generated data in swo, use service names: `java-apm-smoke-test`, `java-apm-smoke-test-webmvc` and `lambda-e2e`



