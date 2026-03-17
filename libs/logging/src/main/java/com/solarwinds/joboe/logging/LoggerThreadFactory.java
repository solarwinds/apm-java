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

package com.solarwinds.joboe.logging;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class LoggerThreadFactory implements ThreadFactory {
  private final String threadName;
  private static final Logger logger = LoggerFactory.getLogger();
  private final AtomicInteger count = new AtomicInteger(0);
  private final ThreadFactory threadFactory = Executors.defaultThreadFactory();

  private LoggerThreadFactory(String threadName) {
    String THREAD_NAME_PREFIX = "SolarwindsAPM";
    this.threadName =
        threadName != null ? THREAD_NAME_PREFIX + "-" + threadName : THREAD_NAME_PREFIX;
  }

  public static LoggerThreadFactory newInstance(String threadName) {
    return new LoggerThreadFactory(threadName);
  }

  @Override
  public Thread newThread(Runnable runnable) {
    Thread thread = threadFactory.newThread(runnable);
    thread.setDaemon(true);
    thread.setName(threadName + "-" + count.incrementAndGet());

    try {
      // Set contextClassLoader to null to avoid memory leak error message during tomcat shutdown
      // see http://wiki.apache.org/tomcat/MemoryLeakProtection#cclThreadSpawnedByWebApp
      // It is ok to set it to null as we do not need servlet container class loader for spawned
      // thread as they should only reference core sdk code or classes included in the agent jar
      thread.setContextClassLoader(null);
    } catch (SecurityException e) {
      logger.warn(
          "Cannot set the context class loader of System Monitor threads to null. Tomcat might display warning message of memory leak during shutdown");
    }

    return thread;
  }
}
