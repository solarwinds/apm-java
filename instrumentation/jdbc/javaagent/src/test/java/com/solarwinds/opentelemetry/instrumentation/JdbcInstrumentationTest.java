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

package com.solarwinds.opentelemetry.instrumentation;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.instrumentation.testing.junit.AgentInstrumentationExtension;
import io.opentelemetry.sdk.trace.data.SpanData;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
@SuppressWarnings("all")
class JdbcInstrumentationTest {

  private static final AttributeKey<String> QUERY_TAG = AttributeKey.stringKey("sw.query_tag");

  @RegisterExtension
  private static final AgentInstrumentationExtension testing =
      AgentInstrumentationExtension.create();

  // Static so the container starts once for the whole class instead of once per test method,
  // which keeps startup cheap and avoids per-test container/network timeouts.
  @Container
  private static final MySQLContainer<?> mysql =
      new MySQLContainer<>(DockerImageName.parse("mysql:9.2.0"));

  private Connection connection;

  @BeforeEach
  void setup() throws SQLException {
    String url =
        String.format(
            "%s?user=%s&password=%s", mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword());
    connection = DriverManager.getConnection(url);

    // Opening the connection issues driver setup statements that are themselves instrumented and
    // emit a nondeterministic number of spans/traces. Drop them so the assertions below only see
    // the trace produced by the code under test.
    testing.clearData();
  }

  @AfterEach
  void teardown() throws SQLException {
    connection.close();
    testing.clearData();
  }

  @Test
  void verifyDbContextInjectionForStatementWhenQueryThrowsAnException() {
    testing.runWithSpan(
        "root",
        () -> {
          try (Statement statement = connection.createStatement()) {
            statement.execute("SELECT * FROM nonexistent_table WHERE name = 'JAVA'");
          } catch (Throwable ignore) {
          }
        });

    assertRootSpanHasQueryTag();
  }

  @Test
  void verifyDbContextInjectionForPrepareStatement() {
    testing.runWithSpan(
        "root",
        () -> {
          try (PreparedStatement preparedStatement =
              connection.prepareStatement("SELECT * FROM nonexistent_table WHERE name = ?")) {
            preparedStatement.setString(0, "JAVA");
          } catch (Throwable ignore) {
          }
        });

    assertRootSpanHasQueryTag();
  }

  // Assert the injection outcome (the "root" span carries sw.query_tag) by polling the flat list
  // of exported spans. This avoids pinning the exact number/grouping of driver-generated spans,
  // which is nondeterministic across driver/server versions and timing, and only waits until the
  // span we care about has been exported.
  private static void assertRootSpanHasQueryTag() {
    await()
        .atMost(Duration.ofSeconds(20))
        .untilAsserted(
            () ->
                assertTrue(
                    testing.spans().stream().anyMatch(JdbcInstrumentationTest::isTaggedRootSpan),
                    "expected a 'root' INTERNAL span carrying the sw.query_tag attribute"));
  }

  private static boolean isTaggedRootSpan(SpanData span) {
    return "root".equals(span.getName())
        && span.getKind() == SpanKind.INTERNAL
        && span.getAttributes().get(QUERY_TAG) != null;
  }
}
