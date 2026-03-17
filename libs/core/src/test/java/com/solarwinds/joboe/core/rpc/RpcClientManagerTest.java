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

package com.solarwinds.joboe.core.rpc;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;

public class RpcClientManagerTest {
  private static final String TEST_SERVER_CERT_LOCATION =
      "src/test/java/com/solarwinds/joboe/core/rpc/test-collector-public.pem";

  @Test
  public void testValidEnvironmentVariables() throws Exception {
    RpcClientManager.init("unit-test-collector1:1234", TEST_SERVER_CERT_LOCATION);

    // verify the fields
    Field field;
    field = RpcClientManager.class.getDeclaredField("collectorHost");
    field.setAccessible(true);
    assertEquals("unit-test-collector1", field.get(null));

    field = RpcClientManager.class.getDeclaredField("collectorPort");
    field.setAccessible(true);
    assertEquals(1234, field.get(null));

    field = RpcClientManager.class.getDeclaredField("collectorCertLocation");
    field.setAccessible(true);
    assertEquals(new File(TEST_SERVER_CERT_LOCATION).toURI().toURL(), field.get(null));

    RpcClientManager.init("unit-test-collector2", null);
    field = RpcClientManager.class.getDeclaredField("collectorHost");
    field.setAccessible(true);
    assertEquals("unit-test-collector2", field.get(null));

    field = RpcClientManager.class.getDeclaredField("collectorPort");
    field.setAccessible(true);
    assertEquals(RpcClientManager.DEFAULT_PORT, field.get(null));

    RpcClientManager.init(null, null); // revert
  }

  @Test
  public void testInvalidEnvironmentVariables() throws Exception {
    RpcClientManager.init("unit-test-collector:not-a-number", null);

    // verify the fields, port number should fallback to default
    Field field;
    field = RpcClientManager.class.getDeclaredField("collectorHost");
    field.setAccessible(true);
    assertEquals("unit-test-collector", field.get(null));

    field = RpcClientManager.class.getDeclaredField("collectorPort");
    field.setAccessible(true);
    assertEquals(RpcClientManager.DEFAULT_PORT, field.get(null));

    RpcClientManager.init(null, null); // revert
  }

  @Test
  public void testDefaultEnviromentVariables() throws Exception {
    RpcClientManager.init(null, null);

    // verify the fields
    Field field;
    field = RpcClientManager.class.getDeclaredField("collectorHost");
    field.setAccessible(true);
    assertEquals(RpcClientManager.DEFAULT_HOST, field.get(null));

    field = RpcClientManager.class.getDeclaredField("collectorPort");
    field.setAccessible(true);
    assertEquals(RpcClientManager.DEFAULT_PORT, field.get(null));
  }
}
