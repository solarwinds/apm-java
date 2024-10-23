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

package com.solarwinds.opentelemetry.instrumentation.hibernate.v6_0;

import static org.mockito.Mockito.when;

import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.instrumentation.testing.junit.AgentInstrumentationExtension;
import java.sql.PreparedStatement;
import org.hibernate.engine.jdbc.spi.JdbcCoordinator;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.jdbc.spi.SqlStatementLogger;
import org.hibernate.engine.spi.SessionEventListenerManager;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.resource.jdbc.ResourceRegistry;
import org.hibernate.resource.jdbc.spi.LogicalConnectionImplementor;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.exec.spi.JdbcSelect;
import org.hibernate.sql.results.jdbc.internal.DeferredResultSetAccess;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DrsaInstrumentationTest {

  @RegisterExtension
  public static AgentInstrumentationExtension testing = AgentInstrumentationExtension.create();

  @Mock private JdbcSelect jdbcSelectMock;

  @Mock private JdbcParameterBindings jdbcParameterBindingsMock;

  @Mock private ExecutionContext executionContextMock;

  @Mock private PreparedStatement preparedStatementMock;

  @Mock private SharedSessionContractImplementor sharedSessionContractImplementorMock;

  @Mock private JdbcServices jdbcServicesMock;

  @Mock private SqlStatementLogger sqlStatementLoggerMock;

  @Mock private JdbcCoordinator jdbcCoordinatorMock;

  @Mock private LogicalConnectionImplementor logicalConnectionMock;

  @Mock private ResourceRegistry resourceRegistryMock;

  @Mock private SessionEventListenerManager sessionEventListenerManagerMock;

  @Test
  void verifyDbContextInjectionSpanIsCreated() {
    final String sql = "select * from test";
    when(executionContextMock.getSession()).thenReturn(sharedSessionContractImplementorMock);
    when(sharedSessionContractImplementorMock.getJdbcServices()).thenReturn(jdbcServicesMock);
    when(jdbcServicesMock.getSqlStatementLogger()).thenReturn(sqlStatementLoggerMock);

    when(jdbcSelectMock.getSql()).thenReturn(sql);
    when(sharedSessionContractImplementorMock.getJdbcCoordinator()).thenReturn(jdbcCoordinatorMock);
    when(jdbcCoordinatorMock.getLogicalConnection()).thenReturn(logicalConnectionMock);

    when(logicalConnectionMock.getResourceRegistry()).thenReturn(resourceRegistryMock);
    when(sharedSessionContractImplementorMock.getEventListenerManager())
        .thenReturn(sessionEventListenerManagerMock);

    DeferredResultSetAccess deferredResultSetAccess =
        new DeferredResultSetAccess(
            jdbcSelectMock,
            jdbcParameterBindingsMock,
            executionContextMock,
            (_sql) -> preparedStatementMock);

    testing.runWithSpan("root", deferredResultSetAccess::getResultSet);
    testing.waitAndAssertTraces(
        trace ->
            trace.hasSpansSatisfyingExactly(
                span -> span.hasName("root").hasKind(SpanKind.INTERNAL),
                span -> span.hasName("sw.jdbc.context").hasKind(SpanKind.INTERNAL)));
  }
}
