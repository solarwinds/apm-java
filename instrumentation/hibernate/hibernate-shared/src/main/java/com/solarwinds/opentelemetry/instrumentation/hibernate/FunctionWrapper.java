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

package com.solarwinds.opentelemetry.instrumentation.hibernate;

import com.solarwinds.opentelemetry.instrumentation.jdbc.shared.DbConstraintChecker;
import io.opentelemetry.context.Context;
import java.sql.PreparedStatement;
import java.util.function.Function;

public class FunctionWrapper implements Function<String, PreparedStatement> {
  private final Function<String, PreparedStatement> delegate;

  public FunctionWrapper(Function<String, PreparedStatement> delegate) {
    this.delegate = delegate;
  }

  @Override
  public PreparedStatement apply(String sql) {
    String comment = Commenter.generateComment(Context.current());
    if (comment != null
        && DbConstraintChecker.preparedSqlTagEnabled()
        && DbConstraintChecker.anyDbConfigured()) {
      return delegate.apply(String.format("%s %s", comment, sql));
    }

    return delegate.apply(sql);
  }
}
