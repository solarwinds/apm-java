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

package com.solarwinds.joboe.core.ebson;

import com.google.common.base.Predicate;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.regex.Pattern;

enum DefaultPredicate implements Predicate<Class<?>> {
  DOUBLE {

    @Override
    public boolean apply(Class<?> input) {
      return input != null && Double.class.isAssignableFrom(input);
    }
  },

  STRING {

    @Override
    public boolean apply(Class<?> input) {
      return input != null && String.class.isAssignableFrom(input);
    }
  },

  EMBEDDED {

    @Override
    public boolean apply(Class<?> input) {
      return input != null && Map.class.isAssignableFrom(input);
    }
  },

  ARRAY {

    @Override
    public boolean apply(Class<?> input) {
      return input != null
          && !byte[].class.isAssignableFrom(input)
          && (Collection.class.isAssignableFrom(input) || input.isArray());
    }
  },

  BINARY {

    @Override
    public boolean apply(Class<?> input) {
      return GENERIC.apply(input);
    }
  },

  GENERIC {

    @Override
    public boolean apply(Class<?> input) {
      return input != null && byte[].class.isAssignableFrom(input);
    }
  },

  OBJECT_ID {

    @Override
    public boolean apply(Class<?> input) {
      return input != null && BsonObjectId.class.isAssignableFrom(input);
    }
  },

  BOOLEAN {

    @Override
    public boolean apply(Class<?> input) {
      return input != null && Boolean.class.isAssignableFrom(input);
    }
  },

  UTC_DATE_TIME {

    @Override
    public boolean apply(Class<?> input) {
      return input != null && Date.class.isAssignableFrom(input);
    }
  },

  NULL {

    @Override
    public boolean apply(Class<?> input) {
      return input == null;
    }
  },

  REGULAR_EXPRESSION {

    @Override
    public boolean apply(Class<?> input) {
      return input != null && Pattern.class.isAssignableFrom(input);
    }
  },

  INT32 {

    @Override
    public boolean apply(Class<?> input) {
      return input != null && Integer.class.isAssignableFrom(input);
    }
  },

  TIMESTAMP {

    @Override
    public boolean apply(Class<?> input) {
      return input != null && BsonTimestamp.class.isAssignableFrom(input);
    }
  },

  INT64 {

    @Override
    public boolean apply(Class<?> input) {
      return input != null && Long.class.isAssignableFrom(input);
    }
  };

  @Override
  public abstract boolean apply(Class<?> input);
}
