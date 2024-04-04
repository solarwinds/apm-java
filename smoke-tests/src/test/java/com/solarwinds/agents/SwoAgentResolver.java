package com.solarwinds.agents;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Optional;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class SwoAgentResolver implements AgentResolver {
  private static final String NH_URL = "https://agent-binaries.global.st-ssp.solarwinds.com/apm/java/latest/solarwinds-apm-agent.jar";

  private static final String NH_AGENT_JAR_NAME = "solarwinds-apm-agent.jar";


  public Optional<Path> resolve(Agent agent) {
    return Optional.ofNullable(downloadAgent(NH_URL, NH_AGENT_JAR_NAME));
  }
}
