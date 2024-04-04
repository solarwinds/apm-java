package com.solarwinds.opentelemetry.core;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;

public class CustomTransactionNameDict {
  private static final Cache<String, String> dict =
      Caffeine.newBuilder()
          .maximumSize(1000)
          .expireAfterAccess(Duration.ofMinutes(20))
          .<String, String>build(); // 20 mins cache

  public static void set(String id, String name) {
    dict.put(id, name);
  }

  public static String get(String id) {
    return dict.getIfPresent(id);
  }
}
