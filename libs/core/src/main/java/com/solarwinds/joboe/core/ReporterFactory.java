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

import com.solarwinds.joboe.core.rpc.Client;
import com.solarwinds.joboe.logging.Logger;
import com.solarwinds.joboe.logging.LoggerFactory;
import java.io.IOException;
import lombok.Getter;

/**
 * Provide methods to create {@link EventReporter}
 *
 * @author pluk
 */
public class ReporterFactory {
  private static final Logger logger = LoggerFactory.getLogger();

  @Getter(lazy = true)
  private static final ReporterFactory instance = new ReporterFactory();

  private ReporterFactory() {}

  /**
   * Builds a {@link UDPReporter}. Take note that this might create a singleton if the system has
   * restrictions on UDP bind address/port, in such an environment, the singleton will have the
   * host/port set to the first call to this method, any other calls following with different host
   * and port would NOT reset the host/port
   *
   * @param host Destination host
   * @param port Destination port
   * @return
   * @throws IOException
   */
  UDPReporter createUdpReporter(String host, Integer port) throws IOException {
    if (host == null || port == null) {
      logger.error("Cannot build UDPReporter. Host and/or port params are null!");
      return null;
    }
    return new UDPReporter(host, port);
  }

  /**
   * Builds a {@link TestReporter}, take note that this reporter collects events from all threads
   *
   * @return
   */
  public TestReporter createTestReporter() {
    return createTestReporter(false);
  }

  /**
   * Builds a {@link TestReporter}, which works in thread local manner according to the parameter
   * isThreadLocal
   *
   * @param isThreadLocal whether the <code>TestReporter</code> built should work in thread local
   *     manner
   * @return
   */
  public TestReporter createTestReporter(boolean isThreadLocal) {
    return isThreadLocal ? new TestReporter() : new NonThreadLocalTestReporter();
  }

  public QueuingEventReporter createQueuingEventReporter(Client client) {
    return new QueuingEventReporter(client);
  }
}
