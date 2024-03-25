package com.solarwinds.agents;

import java.nio.file.Path;
import java.util.Optional;

public class SwoLambdaAgentResolver implements AgentResolver {
  private static final String NH_URL = "https://agent-binaries.global.st-ssp.solarwinds.com/apm/java/latest/solarwinds-apm-agent-lambda.jar";

  private static final String NH_AGENT_JAR_NAME = "solarwinds-apm-agent-lambda.jar";

  public Optional<Path> resolve(Agent agent) {
    return Optional.ofNullable(downloadAgent(NH_URL, NH_AGENT_JAR_NAME));
  }
}
