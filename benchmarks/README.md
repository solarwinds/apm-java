# Benchmarks

JMH microbenchmarks for the solarwinds-apm-java project, using the
[`me.champeau.jmh`](https://github.com/melix/jmh-gradle-plugin) Gradle plugin.

## Running

Run all benchmarks:

```shell
./gradlew :benchmarks:jmh
```

Run a specific benchmark by class name (regex matched against the fully
qualified benchmark name):

```shell
./gradlew :benchmarks:jmh -Pjmh.include=LoggerLazyLoggingBenchmark
```

Results are written in CSV format to `benchmarks/build/results/jmh/results.csv`.

## Adding a benchmark

Benchmark sources live under `src/jmh/java`. Add the module under test as a
`jmh` dependency in `build.gradle.kts`, then write a class annotated with
JMH's `@Benchmark` and related annotations (see
`LoggerLazyLoggingBenchmark` for an example).

## Existing benchmarks

- `LoggerLazyLoggingBenchmark` — compares eager (`debug(String)`) vs. lazy
  (`debug(Supplier<String>)`) logging calls, at both a disabled (`INFO`) and
  enabled (`DEBUG`) log level.
