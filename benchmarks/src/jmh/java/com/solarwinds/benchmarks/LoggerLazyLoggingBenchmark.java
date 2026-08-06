/*
 * © SolarWinds Worldwide, LLC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.solarwinds.benchmarks;

import com.solarwinds.joboe.logging.LogSetting;
import com.solarwinds.joboe.logging.Logger;
import com.solarwinds.joboe.logging.LoggerConfiguration;
import com.solarwinds.joboe.logging.LoggerFactory;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

/**
 * Compares eager {@code debug(String)} against lazy {@code debug(Supplier<String>)} logging.
 *
  * <p>Two scenarios are driven by the {@link #loggerLevel} parameter:
 *
 * <ul>
 *   <li><b>INFO</b> — the {@code DEBUG} level is <em>disabled</em>. This is where lazy logging pays
 *       off: the eager call still builds the message via {@code String.format} and throws it away,
 *       while the lazy call skips construction entirely (the supplier's {@code get()} is never
 *       invoked).
 *   <li><b>DEBUG</b> — the {@code DEBUG} level is <em>enabled</em>. Here the message is built in
 *       both cases, so this measures the overhead lazy logging <em>adds</em> (the capturing-lambda
 *       allocation) when the work cannot be skipped. This is the cost that justified removing the
 *       unused supplier overloads on always-enabled levels (info/warn/error/fatal).
 * </ul>
 *
 * <p>Both stdout and stderr streams are disabled on the logger so that the benchmark measures
 * message construction and dispatch (the {@code shouldLog} guard, lambda allocation, formatting)
 * rather than console I/O throughput.
 *
 * <p>Run with:
 *
 * <pre>{@code
 * ./gradlew :benchmarks:jmh -Pjmh.include=LoggerLazyLoggingBenchmark
 * }</pre>
 */
@Fork(2)
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@Warmup(iterations = 5, time = 1)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Measurement(iterations = 100, time = 1)
public class LoggerLazyLoggingBenchmark {

  /** "INFO" leaves DEBUG disabled (the win); "DEBUG" enables it (the cost). */
  @Param({"INFO", "DEBUG"})
  public String loggerLevel;

  private Logger logger;

  private int statusCode;

  private long durationNanos;

  private String payload;

  @Setup(Level.Trial)
  public void setUp() {
    Logger.Level level = Logger.Level.fromLabel(loggerLevel.toLowerCase(Locale.ROOT));
    // stdoutEnabled=false, stderrEnabled=false => the logger's output stream is a no-op, so we do
    // not measure console I/O even in the DEBUG (enabled) scenario.
    LogSetting logSetting = new LogSetting(level, false, false, null, null, null);
    LoggerFactory.init(LoggerConfiguration.builder().logSetting(logSetting).build());
    logger = LoggerFactory.getLogger();

    statusCode = 200;
    durationNanos = 1_234_567_890L;
    payload = "instance-identity-document";
  }

  /** Eager, {@code String.format}: format runs unconditionally, before {@code debug} checks. */
  @Benchmark
  public void eagerFormat() {
    logger.debug(
        String.format(
            "Retrieved metadata: status=%d durationNanos=%d payload=%s",
            statusCode, durationNanos, payload));
  }

  /** Lazy, {@code String.format}: the supplier is only evaluated when DEBUG is enabled. */
  @Benchmark
  public void lazyFormat() {
    logger.debug(
        () ->
            String.format(
                "Retrieved metadata: status=%d durationNanos=%d payload=%s",
                statusCode, durationNanos, payload));
  }

  /**
   * Eager, string concatenation: the {@code +} expression runs unconditionally, before {@code
   * debug} checks the level.
   */
  @Benchmark
  public void eagerConcat() {
    logger.debug(
        "Retrieved metadata: status="
            + statusCode
            + " durationNanos="
            + durationNanos
            + " payload="
            + payload);
  }

  /** Lazy, string concatenation: the supplier is only evaluated when DEBUG is enabled. */
  @Benchmark
  public void lazyConcat() {
    logger.debug(
        () ->
            "Retrieved metadata: status="
                + statusCode
                + " durationNanos="
                + durationNanos
                + " payload="
                + payload);
  }
}
