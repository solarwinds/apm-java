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

package com.solarwinds.opentelemetry.extensions.config;

import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.common.ComponentLoader;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Resolves the solarwinds agent configuration from a declarative config tree.
 *
 * <p>The configuration may live under the top-level {@code distribution.solarwinds} node (shared
 * across languages in a multi-language deployment) and/or under the language-specific {@code
 * instrumentation/development.java.solarwinds} node. When both are present they are merged per key:
 * the distribution node takes precedence for any key it defines and the instrumentation node fills
 * in the rest. A node counts as present when it exists in the tree, even if it carries no
 * properties; only a node that is entirely absent is skipped (see {@link #resolve}).
 *
 * <p>This is the single source of truth for that precedence so every consumer resolves the same
 * effective config for a given tree.
 */
public final class SolarwindsConfigResolver {

  private SolarwindsConfigResolver() {}

  /**
   * Returns the effective solarwinds config, merging the distribution and instrumentation nodes per
   * key, or {@code null} if neither node is present.
   *
   * <p>Presence — not emptiness — decides the outcome: an absent node ({@code getStructured}
   * returns {@code null}) is ignored, whereas a present-but-empty node is honored. When both nodes
   * are present the result is a per-key merge in which the distribution node wins for every key it
   * defines and the instrumentation node supplies the rest; an empty distribution node therefore
   * simply defers every key to the instrumentation node.
   */
  public static DeclarativeConfigProperties resolve(DeclarativeConfigProperties configProperties) {
    DeclarativeConfigProperties distribution = getDistributionConfig(configProperties);
    DeclarativeConfigProperties instrumentation = getInstrumentationConfig(configProperties);

    if (distribution != null && instrumentation != null) {
      return new MergedConfigProperties(distribution, instrumentation);
    }

    if (distribution != null) {
      return distribution;
    }

    return instrumentation;
  }

  /**
   * Returns the top-level {@code distribution.solarwinds} node, or {@code null} if it is absent.
   */
  public static DeclarativeConfigProperties getDistributionConfig(
      DeclarativeConfigProperties configProperties) {
    return configProperties
        .getStructured("distribution", DeclarativeConfigProperties.empty())
        .getStructured("solarwinds");
  }

  /**
   * Returns the {@code instrumentation/development.java.solarwinds} node, or {@code null} if it is
   * absent.
   */
  public static DeclarativeConfigProperties getInstrumentationConfig(
      DeclarativeConfigProperties configProperties) {
    return configProperties
        .getStructured("instrumentation/development", DeclarativeConfigProperties.empty())
        .getStructured("java", DeclarativeConfigProperties.empty())
        .getStructured("solarwinds");
  }

  /**
   * Read-only view over two solarwinds config nodes that resolves each key from the primary node
   * when present and falls back to the secondary node otherwise, giving a per-key merge rather than
   * an all-or-nothing choice between the nodes.
   */
  private static final class MergedConfigProperties implements DeclarativeConfigProperties {
    private final DeclarativeConfigProperties primary;
    private final DeclarativeConfigProperties fallback;

    MergedConfigProperties(
        DeclarativeConfigProperties primary, DeclarativeConfigProperties fallback) {
      this.primary = primary;
      this.fallback = fallback;
    }

    @Override
    public String getString(String name) {
      String value = primary.getString(name);
      return value != null ? value : fallback.getString(name);
    }

    @Override
    public Boolean getBoolean(String name) {
      Boolean value = primary.getBoolean(name);
      return value != null ? value : fallback.getBoolean(name);
    }

    @Override
    public Integer getInt(String name) {
      Integer value = primary.getInt(name);
      return value != null ? value : fallback.getInt(name);
    }

    @Override
    public Long getLong(String name) {
      Long value = primary.getLong(name);
      return value != null ? value : fallback.getLong(name);
    }

    @Override
    public Double getDouble(String name) {
      Double value = primary.getDouble(name);
      return value != null ? value : fallback.getDouble(name);
    }

    @Override
    public <T> List<T> getScalarList(String name, Class<T> scalarType) {
      List<T> value = primary.getScalarList(name, scalarType);
      return value != null ? value : fallback.getScalarList(name, scalarType);
    }

    @Override
    public DeclarativeConfigProperties getStructured(String name) {
      DeclarativeConfigProperties value = primary.getStructured(name);
      return value != null ? value : fallback.getStructured(name);
    }

    @Override
    public List<DeclarativeConfigProperties> getStructuredList(String name) {
      List<DeclarativeConfigProperties> value = primary.getStructuredList(name);
      return value != null ? value : fallback.getStructuredList(name);
    }

    @Override
    public Set<String> getPropertyKeys() {
      Set<String> keys = new HashSet<>(primary.getPropertyKeys());
      keys.addAll(fallback.getPropertyKeys());
      return keys;
    }

    @Override
    public ComponentLoader getComponentLoader() {
      return primary.getComponentLoader();
    }
  }
}
