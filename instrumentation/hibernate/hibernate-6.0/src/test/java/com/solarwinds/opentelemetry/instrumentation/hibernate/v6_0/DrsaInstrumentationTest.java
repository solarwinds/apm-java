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

import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.instrumentation.testing.junit.AgentInstrumentationExtension;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.query.Query;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DrsaInstrumentationTest {

  @RegisterExtension
  private static final AgentInstrumentationExtension testing =
      AgentInstrumentationExtension.create();

  private SessionFactory sessionFactory;

  @BeforeEach
  public void setUp() {
    Configuration configuration = new Configuration();
    configuration
        .setProperty("hibernate.dialect", "org.hibernate.dialect.H2Dialect")
        .setProperty("hibernate.connection.driver_class", "org.h2.Driver")
        .setProperty("hibernate.connection.url", "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1")
        .setProperty("hibernate.connection.username", "sa")
        .setProperty("hibernate.connection.password", "")
        .setProperty("hibernate.hbm2ddl.auto", "create")
        .setProperty("hibernate.show_sql", "true");

    sessionFactory = configuration.addAnnotatedClass(Dev.class).buildSessionFactory();

    Session session = sessionFactory.openSession();
    Transaction transaction = session.beginTransaction();

    Dev dev = new Dev();
    dev.setName("cleverchuk");

    session.persist(dev);
    transaction.commit();
    session.close();
  }

  @AfterEach
  public void tearDown() {
    sessionFactory.close();
  }

  @Test
  void verifyDbContextInjectionSpanIsCreated() {
    testing.runWithSpan(
        "root",
        () -> {
          Session session = sessionFactory.openSession();
          session.beginTransaction();
          CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();

          CriteriaQuery<Dev> createQuery = criteriaBuilder.createQuery(Dev.class);
          Root<Dev> root = createQuery.from(Dev.class);
          createQuery
              .select(root)
              .where(criteriaBuilder.equal(root.get("name"), "cleverchuk"))
              .orderBy(criteriaBuilder.desc(root.get("name")));

          Query<Dev> query = session.createQuery(createQuery);
          query.getResultList();
          session.getTransaction().commit();

          session.close();
        });

    testing.waitAndAssertTraces(
        trace ->
            trace.hasSpansSatisfyingExactly(
                span -> span.hasName("root").hasKind(SpanKind.INTERNAL),
                span -> span.hasName("sw.hibernate.context").hasKind(SpanKind.INTERNAL)));
  }
}
