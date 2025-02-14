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

import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.instrumentation.testing.junit.AgentInstrumentationExtension;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.junit.jupiter.MockitoExtension;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@ExtendWith(MockitoExtension.class)
class JdbcInstrumentationTest {

  @RegisterExtension
  private static final AgentInstrumentationExtension testing =
      AgentInstrumentationExtension.create();

  @Container public MySQLContainer<?> mysql = new MySQLContainer<>("mysql:9.2.0");

  private Connection connection;

  @BeforeEach
  void setup() throws SQLException {
    String url =
        String.format(
            "%s?user=%s&password=%s", mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword());
    connection = DriverManager.getConnection(url);
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

    testing.waitAndAssertTraces(
        trace ->
            // no real checks because we don't care about these spans
            trace.hasSpansSatisfyingExactly(span -> {}, span -> {}, span -> {}, span -> {}),
        trace ->
            trace.hasSpansSatisfyingExactly(
                span ->
                    span.hasName("root")
                        .hasKind(SpanKind.INTERNAL)
                        .hasAttributesSatisfying(
                            attributes ->
                                assertNotNull(
                                    attributes.get(AttributeKey.stringKey("sw.query_tag")))),
                span -> {}));
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

    testing.waitAndAssertTraces(
        trace ->
            // no real checks because we don't care about these spans
            trace.hasSpansSatisfyingExactly(span -> {}, span -> {}, span -> {}, span -> {}),
        trace ->
            trace.hasSpansSatisfyingExactly(
                span ->
                    span.hasName("root")
                        .hasKind(SpanKind.INTERNAL)
                        .hasAttributesSatisfying(
                            attributes ->
                                assertNotNull(
                                    attributes.get(AttributeKey.stringKey("sw.query_tag")))),
                span -> {}));
  }
}
