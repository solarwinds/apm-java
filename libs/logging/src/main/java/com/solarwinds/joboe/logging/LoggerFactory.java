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

import lombok.Getter;

/**
 * Factory that returns logger instance. Take note that this is very similar to other logging
 * frameworks (commons.logging, log4j etc) factory and should be replaced by those factory if we
 * later on switch to those frameworks
 *
 * @author Patson Luk
 */
public class LoggerFactory {
  // Only allow a single logger. We do not need multiple loggers as we handle everything using the
  // same handlers at this moment
  // Take note that we do not instantiate this in the init routine as we wish to make the logger
  // always available (even before init is run)
  // Just that code that uses the logger before calling init will not follow the configuration
  // defined in the configration file <code>ConfigProperty.AGENT_DEBUG</code>

  @Getter private static final Logger logger = Logger.INSTANCE;

  /** Initialize the logger factory. This is used to set the logging level of the logger */
  public static void init(LoggerConfiguration config) {
    logger.configure(config);
  }
}
