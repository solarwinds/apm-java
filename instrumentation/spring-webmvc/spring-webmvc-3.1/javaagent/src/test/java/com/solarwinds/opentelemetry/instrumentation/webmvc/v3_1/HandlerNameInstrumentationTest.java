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

package com.solarwinds.opentelemetry.instrumentation.webmvc.v3_1;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.instrumentation.testing.junit.AgentInstrumentationExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletConfig;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@ExtendWith(MockitoExtension.class)
class HandlerNameInstrumentationTest {

  @RegisterExtension
  static final AgentInstrumentationExtension testing = AgentInstrumentationExtension.create();

  @Test
  void verifyHandlerNameIsStampedOnServerSpan() {
    testing.runWithSpan(
        "server",
        () -> {
          try {
            AnnotationConfigWebApplicationContext appContext =
                new AnnotationConfigWebApplicationContext();
            appContext.register(TestConfig.class);

            DispatcherServlet dispatcherServlet = new DispatcherServlet(appContext);
            dispatcherServlet.init(new MockServletConfig());

            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/greet");
            MockHttpServletResponse response = new MockHttpServletResponse();

            try {
              dispatcherServlet.service(request, response);
            } finally {
              dispatcherServlet.destroy();
              appContext.close();
            }
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
        });

    testing.waitAndAssertTraces(
        trace ->
            trace.hasSpansSatisfyingExactly(
                span ->
                    span.hasName("server")
                        .hasKind(SpanKind.INTERNAL)
                        .hasAttributesSatisfying(
                            attributes ->
                                assertEquals(
                                    "TestController.greet",
                                    attributes.get(AttributeKey.stringKey("HandlerName"))))));
  }

  @EnableWebMvc
  @org.springframework.context.annotation.Configuration
  static class TestConfig {
    @org.springframework.context.annotation.Bean
    public TestController testController() {
      return new TestController();
    }
  }

  @RestController
  static class TestController {
    @GetMapping("/greet")
    public String greet() {
      return "hello";
    }
  }
}
