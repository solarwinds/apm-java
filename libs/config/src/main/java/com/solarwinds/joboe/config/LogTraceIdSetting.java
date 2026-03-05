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

package com.solarwinds.joboe.config;

import lombok.Getter;

/**
 * Setting that contains scope information for each log injection category.
 *
 * <p>There are 2 categories right now - "autoInsert" and "mdc"
 *
 * @author pluk
 */
@Getter
public class LogTraceIdSetting {
  private final LogTraceIdScope autoInsertScope;
  private final LogTraceIdScope mdcScope;

  public LogTraceIdSetting(LogTraceIdScope autoInsertScope, LogTraceIdScope mdcScope) {
    this.autoInsertScope = autoInsertScope;
    this.mdcScope = mdcScope;
  }
}
