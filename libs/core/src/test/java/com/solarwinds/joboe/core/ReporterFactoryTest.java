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

package com.solarwinds.joboe.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.solarwinds.joboe.core.rpc.Client;
import java.lang.reflect.Field;
import java.net.InetAddress;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ReporterFactoryTest {

  private static ReporterFactory tested;

  @Mock private Client clientMock;

  @BeforeAll
  static void setup() {
    tested = ReporterFactory.getInstance();
  }

  @Test
  public void testbuildNonDefaultUdpReporter() throws Exception {
    UDPReporter reporter = tested.createUdpReporter("localhost", 9999);

    Field addressField = reporter.getClass().getDeclaredField("addr");
    addressField.setAccessible(true);

    Field portField = reporter.getClass().getDeclaredField("port");
    portField.setAccessible(true);

    InetAddress address = (InetAddress) addressField.get(reporter);
    assertEquals(InetAddress.getByName("localhost"), address);
    assertEquals(9999, portField.get(reporter));
  }

  @Test
  void testCreateQueuingEventReporter() {
    assertNotNull(tested.createQueuingEventReporter(clientMock));
  }
}
