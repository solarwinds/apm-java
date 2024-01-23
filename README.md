[![Benchmark](https://github.com/appoptics/solarwinds-apm-java/actions/workflows/benchmark.yml/badge.svg)](https://github.com/appoptics/solarwinds-apm-java/actions/workflows/benchmark.yml)  [![Push](https://github.com/appoptics/solarwinds-apm-java/actions/workflows/push.yml/badge.svg)](https://github.com/appoptics/solarwinds-apm-java/actions/workflows/push.yml)  [![Java CI with Gradle](https://github.com/appoptics/solarwinds-apm-java/actions/workflows/release.yml/badge.svg)](https://github.com/appoptics/solarwinds-apm-java/actions/workflows/release.yml)
## Introduction
This repository contains Solarwinds APM implementation that works with OpenTelemetry SDK and Auto agent. This is built on demo repo https://github.com/open-telemetry/opentelemetry-java-instrumentation/tree/main/examples/distro and added sub-projects by merging changes from https://github.com/appoptics/appoptics-opentelemetry-java (now archived)

Here is the summary of the sub-projects:
- agent : Builds the full OT auto agent with extra Solarwinds APM components. This is simply a repackaging build script that pull OT agent and our sub-projects to construct a new auto agent
- appoptics-opentelemetry-sdk : (most of which are archived as we have decided not to maintain backward-compatibility with AppOptics) Current it has only one API: the agent checker.
- custom : Extra Solarwinds APM components, contains all custom functionality, SPI and other extensions (for example Sampler, Tracer Provider etc) to be loaded by OT's agent classloader
- core-bootstrap : Core Solarwinds APM components that need to be made available to bootstrap classloader. This is important for `appoptics-opentelemetry-sdk` as the classes from `appoptics-opentelemetry-sdk` are loaded by app loader, which has no access to OT's agent classloader which loads `custom` 
- instrumentation : Additional instrumentation provided by us using the OT instrumentation framework (ByteBuddy)

More details for each of the sub-projects are listed in [Sub-Projects](#sub-projects) section


## Build
#### Preparations
Since this project has dependencies on various internal artifacts from [joboe](https://github.com/librato/joboe), the build machine would need access to those artifacts. Currently the Joboe core libraries for the OpenTelemetry custom distro are in the `otel` branch of the `Joboe` repo and are published to the Github Packages. 

#### Agent Jars
Simply run `gradle build` at the root folder. (or run `gradle publishToMavenLocal` to publish to local maven repo)

The agent should be built at `agent\build\libs\solarwinds-apm-agent.jar`.

## Usage
#### Agent Jar
Attach the agent to jvm process arg such as:
`-javaagent:"_the_path_to_the_jar_file" -Dotel.solarwinds.service.key=<service key here>`

The service key can also be defined via the environment variable `SW_APM_SERVICE_KEY` or in the config file.

Upon successful initialization, the log should print such as:
```
[otel.javaagent 2021-06-30 13:04:07:759 -0700] [main] INFO com.appoptics.opentelemetry.extensions.SolarwindsTracerProviderCustomizer - Successfully initialized Solarwinds APM OpenTelemetry extensions with service key ec3d********************************************************5468:ot
```

#### SDK
The custom distro supports the standard OpenTelemetry APIs/SDKs. In addition to that, it also has an API to check if the agent is ready to use (and wait for a specified duration before it is ready). This is useful when you need to create traces manually.

The API can be called as below, which waits for at most 10 seconds before the agent is ready.
```AgentChecker.waitUntilAgentReady(10, TimeUnit.SECONDS))```

## Debug
Various flags can be enabled to enable debugging. You can define the environment variable `SW_APM_DEBUG_LEVEL` to `debug` to print verbose logs. The OpenTelemetry core agent also provides an JVM argument `-Dotel.javaagent.debug=true` to enable detailed logs.

#### Muzzling
OT provides Muzzling which matches classes/fields/methods used by instrumentation vs the ones available on the running JVM. If there are any mismatch, the instrumentation will be silently disabled unless debugging flag such as below is provided in the JVM args:
```
-Dio.opentelemetry.javaagent.slf4j.simpleLogger.log.muzzleMatcher=DEBUG
```

## Sub Projects
#### agent
Repackages the OT original agent with our custom compoenents (such as Sampler, Tracer) and instrumentation. Custom shadowing (moving classes to `inst` folder and rename extension from `class` to `classdata`) is performed on sub project `custom` and `instrumentation` to make them available to the [OT agent classloader](https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/main/javaagent-bootstrap/src/main/java/io/opentelemetry/javaagent/bootstrap/AgentClassLoader.java).

This produces a new agent, that contains both the OT agent and our changes.

The advantage of this approach:
1. More control over the OT agent logic, for example we can have another layer of agent and modify the OT agent entry point by [changing the MANIFEST file](https://github.com/appoptics/solarwinds-apm-java/blob/master/agent/build.gradle#L48)
2. Since this is a separate repo from the OT java instrumentation, we have loose coupling here. Updates from OT java instrumentation and changes in this repo are less likely to have conflicts with eachother.
3. We can easily solve the classloading issue encountered in the [extension approach](https://github.com/appoptics/appoptics-opentelemetry-java/pull/5) as
  - Muzzling is relatively easy to invoke here, though we still need to manually copy the `buildsrc` to this project. Though there's [plan](https://github.com/open-telemetry/opentelemetry-java-instrumentation/discussions/3350#discussioncomment-919197) to export the Muzzle plugin so external repo/project like this can run Muzzle directly as a plugin (as opposed to using `buildsrc`)
  - We no longer need to explicitly append core/metrics dependencies to bootstrap classpath, we simply need to include them as separate dependencies in [agent gradle build](https://github.com/appoptics/solarwinds-apm-java/blob/master/agent/build.gradle#L17). Such that they will be copied to the final agent w/o muzzling (ensure the core/metrics dependencies are `compiledOnly` for `custom` and `instrumentation`, which are muzzled by moving into `inst` folder with `classfile` extension). Since the final agent is appended to the bootstrap classloader by the OT bootstrap logic, our `core`/`metrics` classes will also be avaialble in bootstrap w/o extra work
4. The startup jvm args are simplier as we can package most of the extra properties in [AppOpticsPropertySource](https://github.com/appoptics/solarwinds-apm-java/blob/master/custom/src/main/java/com/appoptics/opentelemetry/extensions/AppOpticsPropertySource.java)

The disadvantage of this approach:
1. Whenever OT provides a newer version of agent, we will need to rebuild the agent on this repo too if we want the updates.

#### appoptics-opentelemetry-sdk
Currently it has only one API: the agent ready checker.

#### custom
Our main implementation for OT SPI - which scans implementation using Java service loader. We provide our "implementation" to various OT services such as Sampler, Tracer to enable various AO specific features - AO sampling, profiling, detailed trace reporting/export etc. This also contains various intiailization code and resource files such as default config, SSL cert for gRPC to collector etc.

This is used by both the sub-projects `agent` and `sdk-extensions`

#### core-bootstrap 
Similar to `custom`, but this contains core Solarwinds APM components that need to be made available to bootstrap classloader. This is important for `appoptics-opentelemetry-sdk` as classes from `appoptics-opentelemetry-sdk` are loaded by app loader, which has no access to OT's agent classloader which loads `custom` 

#### instrumentation
Our custom instrumentation added on top of the existing OT auto agent instrumentation. 

We follow the practice of exiting OT auto agent instrumentation which contains:
- `InstrumentationModule` - declares a list of `TypeInstrumentation` for a particular module (for example JDBC). Take note that we need to override isHelperClass
```
@Override
public boolean isHelperClass(String className) {
  return className.startsWith("com.appoptics.opentelemetry.");
}
``` 
so the muzzle plugin will inject our `Tracer` to the application classloader
- `TypeInstrumentation`- declares instrumentaiton point of what "Type" (class) and what criteria (method name match, annotations on method) should instrumentation (as Advices) be applied to
- `Advice` - Advices defines what code to be injected to the instrumetnation points. Take note that even though Advices look very much like regular java code, they are actually translated to bytecode and injected on first classloading. Which means debug breakpoints on advice does not work. And code in advice also has various restrictions, see [here](https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/main/docs/contributing/writing-instrumentation-module.md#advice-classes) for details. Therefore advice code is usually left simple and mostly delegate to `Tracer` which is regular java code
- `Tracer` - Code that performs actual instrumentation action (ie extract useful KVs, construct and report spans etc)

A very good description of instrumentation with bytebuddy/OT can be found in [here](https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/main/docs/contributing/writing-instrumentation-module.md#advice-classes)







