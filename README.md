## Introduction
This repository contains AppOptics implementation that works with OpenTelemetry SDK and Auto agent. This is built on demo repo https://github.com/open-telemetry/opentelemetry-java-instrumentation/tree/main/examples/distro and added sub-projects by merging changes from https://github.com/appoptics/appoptics-opentelemetry-java (now archived)

Here is the summary of the sub-projects:
- agent : Builds the full OT auto agent with extra AppOptics components. This is simply a repackaging build script that pull OT agent and our sub-projects to construct a new auto agent
- appoptics-opentelemetry-sdk : Builds the SDK artifact which has exactly the same interface as our existing AppOptics java agent SDK https://github.com/librato/joboe/tree/master/api . This is to be used as a base for `appoptics-opentelemetry-sdk-shaded`
- appoptics-opentelemetry-sdk-shaded : Same as appoptics-opentelemetry-sdk but can be used with the OT agent with shaded class names
- custom : Extra AppOptics components, contains all custom functionality, SPI and other extensions (for example Sampler, Tracer Provider etc) to be loaded by OT's agent classloader
- core-bootstrap : Core AppOptics components that need to be made available to bootstrap classloader. This is important for `appoptics-opentelemetry-sdk` as the classes from `appoptics-opentelemetry-sdk` are loaded by app loader, which has no access to OT's agent classloader which loads `custom` 
- instrumentation : Additional instrumentation provided by us using the OT instrumentation framework (ByteBuddy)
- sdk-extensions : Builds the AO extension jar which runs with the original OT agent (vs the agent built from `agent` sub-project)
- sdk-extensions-bootstrap : Builds the AO extension jar dependencies that should be made available to bootstrap loader. It basically package the `core` and `metrics` from joboe

More details for each of the sub-projects are listed in [Sub-Projects](#sub-projects) section


## Build
#### Preparations
Since this project has dependencies on various internal artifacts from [joboe](https://github.com/librato/joboe), the build machine would need access to those artifacts. We currently do NOT have any internal maven server to host those artifacts. Hence the easiest way is to build and install those artifacts from joboe on the build machine:
1. Check out the dependency project (joboe) git clone https://github.com/librato/joboe.git
2. Navigate to the cloned joboe, check out the relevant branch `git checkout AO-16083-ot-v1` (this will change in the future if we merge this branch back to `develop`/`main`, in such case we will check out the joboe version referenced by this distro instead)
3. Build the joboe and install all the artifacts by executing `mvn clean install` at the project root (add -DskipTests flag to skip tests, possible to only build dependencies, core and metrics, as they are the only ones required by our OT implementation, though it might be easier to just build all)

#### Agent/Extensions Jars
Simply run `gradle build` at the root folder.

The agent should be built at `agent\build\libs\agent-1.0-SNAPSHOT-all.jar`.
The sdk-extensions jar should be built at `sdk-extensions\build\libs\sdk-extensions-1.0-SNAPSHOT-all.jar`.

#### SDK artifacts
To build the `appoptics-opentelemetry-sdk` and `appoptics-opentelemetry-sdk-shaded` artifacts, use
`gradle :appoptics-opentelemetry-sdk:publishToMavenLocal` and
`gradle :appoptics-opentelemetry-sdk-shaded:publishToMavenLocal`

The artifacts will be published to local maven repo.

## Usage
#### Agent Jar
Attach the agent to jvm process arg such as:
`-javaagent:"C:\Users\patson.luk\git\opentelemetry-custom-distro\agent\build\libs\agent-1.0-SNAPSHOT-all.jar" -Dotel.appoptics.service.key=<service key here>`

Upon successful initialization, the log should print such as:
```
[otel.javaagent 2021-06-30 13:04:07:759 -0700] [main] INFO com.appoptics.opentelemetry.extensions.AppOpticsTracerProviderConfigurer - Successfully initialized AppOptics OpenTelemetry extensions with service key ec3d********************************************************5468:ot
```
#### Extension Jar
1. Either download the auto agent directly from https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/latest/download/opentelemetry-javaagent-all.jar or build from source. To build from source:
  - Checkout https://github.com/open-telemetry/opentelemetry-java-instrumentation
Navigate to the project directory, then gradlew build -x test (-x test is for skipping tests, remove the option if tests are to be run). Use java 9 or above => export JAVA_HOME=<Java 9+ path>.
  - The java agent jar can be found in ...\git\opentelemetry-java-instrumentation\javaagent\build\libs
2. Attach the OT agent and our AO extension to the jvm process arg such as:
```
-javaagent:"C:\Users\patson.luk\Downloads\opentelemetry-javaagent-all-1.3.1.jar"
-Dotel.javaagent.experimental.initializer.jar="C:\Users\patson.luk\git\opentelemetry-custom-distro\sdk-extensions\build\libs\sdk-extensions-1.0-SNAPSHOT-all.jar"
-Dotel.traces.exporter=appoptics
-Dotel.traces.sampler=appoptics
-Dotel.metrics.exporter=none
-Dotel.propagators=tracecontext,baggage,appoptics
-Dotel.appoptics.service.key=<service key here>
-Xbootclasspath/a:"C:\Users\patson.luk\git\opentelemetry-custom-distro\sdk-extensions-bootstrap\build\libs\sdk-extensions-bootstrap-1.0-SNAPSHOT.jar"
```

Upon successful initialization, the log should print such as:
```
[otel.javaagent 2021-07-08 10:41:16:650 -0700] [main] INFO io.opentelemetry.javaagent.tooling.VersionLogger - opentelemetry-javaagent - version: 1.3.1
[otel.javaagent 2021-07-08 10:41:19:141 -0700] [main] INFO com.appoptics.opentelemetry.extensions.AppOpticsTracerProviderConfigurer - Successfully initialized AppOptics OpenTelemetry extensions with service key ec3d********************************************************5468:ot
```

**There are certain challenges for classloading with extension jar, more details in https://github.com/open-telemetry/opentelemetry-java-instrumentation/discussions/3350** 

#### SDK artifact
After the artifact `appoptics-opentelemetry-sdk-shaded` is built and published to local maven, it can be used by adding `dependency` to `pom.xml` such as:
```
<dependency>
	<groupId>com.appoptics.agent.java</groupId>
	<artifactId>appoptics-opentelemetry-sdk-shaded</artifactId>
	<version>1.0-SNAPSHOT</version>
</dependency>
 ```   

## Debug
Various flags can be enabled to enable debugging

#### AppOptics core logs
(WIP)

#### Muzzling
OT provides Muzzling which matches classes/fields/methods used by instrumentation vs the ones available on the running JVM. If there are any mismatch, the instrumentation will be silently disabled unless debugging flag such as below is provided in the JVM args:
```
-Dio.opentelemetry.javaagent.slf4j.simpleLogger.log.muzzleMatcher=DEBUG
```

## Sub Projects
#### agent
Repackages the OT original agent with our custom compoenents (such as Sampler, Tracer) and instrumentation. Custom shadowing (moving classes to `inst` folder and rename extension from `class` to `classdata`) is performed on sub project `custom` and `instrumentation` to make them available to the [OT agent classloader](https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/main/javaagent-bootstrap/src/main/java/io/opentelemetry/javaagent/bootstrap/AgentClassLoader.java).

This produces a new agent, that contains both the OT agent and our changes.

This approach is a middleground of the other 2 approaches:
1. AO as a pure OT extension, isolated from the OT agent - https://github.com/appoptics/appoptics-opentelemetry-java#opentelemetry-auto-agent
2. AO as a fork of OT java instrumentation agent - https://github.com/open-telemetry/opentelemetry-java-instrumentation

The advantage of this approach:
1. More control over the OT agent logic, for example we can have another layer of agent and modify the OT agent entry point by [changing the MANIFEST file](https://github.com/appoptics/opentelemetry-custom-distro/blob/master/agent/build.gradle#L48)
2. Since this is a separate repo from the OT java instrumentation, we have loose coupling here. Updates from OT java instrumentation and changes in this repo are less likely to have conflicts with eachother.
3. We can easily solve the classloading issue encountered in the [extension approach](https://github.com/appoptics/appoptics-opentelemetry-java/pull/5) as
  - Muzzling is relatively easy to invoke here, though we still need to manually copy the `buildsrc` to this project. Though there's [plan](https://github.com/open-telemetry/opentelemetry-java-instrumentation/discussions/3350#discussioncomment-919197) to export the Muzzle plugin so external repo/project like this can run Muzzle directly as a plugin (as opposed to using `buildsrc`)
  - We no longer need to explicitly append core/metrics dependencies to bootstrap classpath, we simply need to include them as separate dependencies in [agent gradle build](https://github.com/appoptics/opentelemetry-custom-distro/blob/master/agent/build.gradle#L17). Such that they will be copied to the final agent w/o muzzling (ensure the core/metrics dependencies are `compiledOnly` for `custom` and `instrumentation`, which are muzzled by moving into `inst` folder with `classfile` extension). Since the final agent is appended to the bootstrap classloader by the OT bootstrap logic, our `core`/`metrics` classes will also be avaialble in bootstrap w/o extra work
4. The startup jvm args are simplier as we can package most of the extra properties in [AppOpticsPropertySource](https://github.com/appoptics/opentelemetry-custom-distro/blob/master/custom/src/main/java/com/appoptics/opentelemetry/extensions/AppOpticsPropertySource.java)

The disadvantage of this approach:
1. Whenever OT provides a newer version of agent, we will need to rebuild the agent on this repo too if we want the updates.

#### appoptics-opentelemetry-sdk
An SDK artifact that has the same interfaces as our existing appoptics SDK (https://librato.github.io/java-agent-sdk-javadoc/com/appoptics/api/ext/package-summary.html), under the hood, this translates SDK calls into OT operations (spans), therefore it can work with application that has existing manual OT SDK instrumentation.

In fact this artifact is currently not working on its own (missing dependency on `custom` and `core-bootstrap` in the built artifact). There is no strong use case for this artifact, the more useful case is addressed by `appoptics-opentelemetry-sdk-shaded` which is built on top of this artifact.

#### appoptics-opentelemetry-sdk-shaded
Repackage `appoptics-opentelemetry-sdk` to [shaded name space](https://github.com/appoptics/opentelemetry-custom-distro/blob/master/gradle/shadow.gradle), for example `io.opentelemetry.api` -> `io.opentelemetry.javaagent.shaded.io.opentelemetry.api` so SDK calls works with the shaded classes in OT auto agent.

This is expected to be used with the enhanced OT auto agent repackaged by our sub-project `agent` or the pure OT agent with our extension jar from sub-projec `sdk-extension`.

The use case would be someone using our existing AppOptics agent with SDK calls can migrate to use our enhanced OT agent with this `appoptics-opentelemetry-sdk-shaded` artifact. There should be no code change except changes to build process to reference this new artifact instead of the existing appoptics SDK

#### custom
Our main implementation for OT SPI - which scans implementation using Java service loader. We provide our "implementation" to various OT services such as Sampler, Tracer to enable various AO specific features - AO sampling, profiling, detailed trace reporting/export etc. This also contains various intiailization code and resource files such as default config, SSL cert for gRPC to collector etc.

This is used by both the sub-projects `agent` and `sdk-extensions`

#### core-bootstrap 
Similar to `custom`, but this contains core AppOptics components that need to be made available to bootstrap classloader. This is important for `appoptics-opentelemetry-sdk` as classes from `appoptics-opentelemetry-sdk` are loaded by app loader, which has no access to OT's agent classloader which loads `custom` 

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


#### sdk-extensions
Repackages and builds the AO extension jar which runs with the original OT agent (vs the agent built from `agent` sub-project). Take note that since classes from this jar are appended via `-Dotel.javaagent.experimental.initializer.jar` which is loaded by application classloader (instead of OT agent classloader), no custom shadowing is performed (no putting in `inst` and renaming extension from `class` to `classdata`). Regular shadowing/shading is still applied (`io.opentelemetry.xyz` -> `io.opentelemetry.javaagent.shaded.io.opentelemetry.xyz`) as this is run with the OT original agent which some OT class references are still shaded. Take note that in order ot use this extension, it has to be augmented with `-Xbootclasspath` to append `sdk-extensions-bootstrap` jar to bootstrap class path (see [sdk-extensions-bootstrap](sdk-extensions-bootstrap) for details)

#### sdk-extensions-bootstrap
Repackages `core` and `metric` from joboe and append these classes to the bootstrap classpath. This is necessary as sdk-extension's instrumentation classes are injected into application classloader, these instrumentations reference our classes in joboe `core` and `metrics`. If the `core` classes are included in the sdk-extension via `-Dotel.javaagent.experimental.initializer.jar`, then they are unavailable to such classloader. 

More details are documented in https://github.com/appoptics/appoptics-opentelemetry-java/pull/5 . Take note that since the instrumentations are now "muzzled", it no longer triggers the same exception as seen in the PR, but the instrumentation would still fail with similar classloading failure:
```
[otel.javaagent 2021-07-08 11:25:46:278 -0700] [http-nio-8080-exec-1] WARN muzzleMatcher - -- com.appoptics.opentelemetry.instrumentation.AoStatementTracer:22 Missing class com.tracelytics.util.BackTraceUtil
```

As discussed in https://github.com/open-telemetry/opentelemetry-java-instrumentation/discussions/3350, one approach is to append the `core` and `metrics` classes to bootstrap classloader by using a separate jar, which is produced by this sub-project








