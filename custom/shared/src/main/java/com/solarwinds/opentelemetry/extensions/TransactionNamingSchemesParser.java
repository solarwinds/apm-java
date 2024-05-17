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

package com.solarwinds.opentelemetry.extensions;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.solarwinds.joboe.config.ConfigParser;
import com.solarwinds.joboe.config.InvalidConfigException;
import java.lang.reflect.Type;
import java.util.List;

public final class TransactionNamingSchemesParser
    implements ConfigParser<String, List<TransactionNamingScheme>> {
  private static final Gson gson = new GsonBuilder().create();

  @Override
  public List<TransactionNamingScheme> convert(String input) throws InvalidConfigException {
    try {
      Type type = new TypeToken<List<TransactionNamingScheme>>() {}.getType();
      return gson.fromJson(input, type);
    } catch (JsonSyntaxException e) {
      throw new InvalidConfigException(e);
    }
  }
}
