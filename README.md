## Introduction
This repository is built on the exmaple in https://github.com/open-telemetry/opentelemetry-java-instrumentation/tree/main/examples/distro , which serves as a prototype of extending functionality of OpenTelemetry Java instrumentation agent.

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


## General structure

This repository has four main submodules:

* `custom` contains all custom functionality, SPI and other extensions - Currently we have all the AO tracer/sampler/span exporter here
* `agent` contains the main repackaging functionality and, optionally, an entry point to the agent. Currently we modified `gradle.build` to include `core`/`metrics` w/o muzzling
* `instrumentation` contains custom instrumentations - Currently we have the JDBC custom instrumentation which simply add backtrace to the existing OT JDBC span reporting

## Build
Simply run `gradle bulid` at the root folder.

The agent should be built at `agent\build\libs\agent-1.0-SNAPSHOT-all.jar`

## Usage
Attach the agent to jvm process arg such as:
`-javaagent:"C:\Users\patson.luk\git\opentelemetry-custom-distro\agent\build\libs\agent-1.0-SNAPSHOT-all.jar" -Dotel.appoptics.service.key=<service key here>`

Upon successful initialization, the log should print such as:
```
[otel.javaagent 2021-06-30 13:04:07:759 -0700] [main] INFO com.appoptics.opentelemetry.extensions.AppOpticsTracerProviderConfigurer - Successfully initialized AppOptics OpenTelemetry extensions with service key ec3d********************************************************5468:ot
```



