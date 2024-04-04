package com.solarwinds.opentelemetry.extensions;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class JsonSettings {
  private Map<String, Object> arguments;

  private String flags;

  private String layer;

  private long timestamp;

  private long ttl;

  private short type;

  private long value;
}
