/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package com.solarwinds.agents;

import java.nio.file.Path;
import java.util.Optional;

public interface AgentResolver {
  Optional<Path> resolve(Agent agent);
}
