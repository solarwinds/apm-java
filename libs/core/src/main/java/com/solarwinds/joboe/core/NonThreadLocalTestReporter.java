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

import com.solarwinds.joboe.logging.Logger;
import com.solarwinds.joboe.logging.LoggerFactory;
import java.util.Deque;
import java.util.LinkedList;

/**
 * Mock reporter similar to {@link TestReporter}, only that this one does not work in thread local
 * manner
 *
 * @author pluk
 */
class NonThreadLocalTestReporter extends TestReporter {
  private final Logger logger = LoggerFactory.getLogger();

  private final Deque<byte[]> byteBufferList = new LinkedList<byte[]>();

  NonThreadLocalTestReporter() {}

  @Override
  public synchronized void reset() {
    byteBufferList.clear();
  }

  @Override
  public synchronized void send(Event event) {
    try {
      byte[] buf = event.toBytes();
      logger.debug("Sent " + buf.length + " bytes");
      byteBufferList.add(buf);
    } catch (BsonBufferException e) {
      logger.error("Failed to send events : " + e.getMessage(), e);
    }
  }

  @Override
  public synchronized byte[] getLastSent() {
    return byteBufferList.getLast();
  }

  @Override
  public synchronized Deque<byte[]> getBufList() {
    return new LinkedList<>(byteBufferList); // return a clone
  }
}
