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

package com.solarwinds.agents;

import lombok.Value;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Value
public class Agent {

  public static final Agent SWO_JAVA_AGENT = new Agent( "swo-java-agent", "latest Solarwinds agent");

  String name;
  String description;
  URL url;
  List<String> additionalJvmArgs;

  public Agent(String name, String description) {
    this(name, description, null);
  }

  public Agent(String name, String description, String url) {
    this(name, description, url, Arrays.asList("-Dio.opentelemetry.context.enableStrictContext=true",
            "-Dotel.java.experimental.span-stacktrace.min.duration=0ms",
            "-Dsw.apm.span.stack.trace.filters=thread.id,os.description,http.request.method"));
  }

  public Agent(String name, String description, String url, List<String> additionalJvmArgs) {
    this.name = name;
    this.description = description;
    this.url = makeUrl(url);
    this.additionalJvmArgs = new ArrayList<>(additionalJvmArgs);
  }

  private static URL makeUrl(String url) {
    try {
      if (url == null) {
        return null;
      }
      return URI.create(url).toURL();
    } catch (MalformedURLException e) {
      throw new RuntimeException("Error parsing url", e);
    }
  }
}
