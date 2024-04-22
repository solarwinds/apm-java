package com.solarwinds.opentelemetry.instrumentation;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.util.List;

public class BackTraceCache {
  private static final Cache<List<StackTraceElement>, String> backTraceCache =
      Caffeine.newBuilder()
          .maximumSize(20)
          .expireAfterAccess(Duration.ofHours(1L))
          .build(); // 1 hour cache;

  static String getBackTraceString(List<StackTraceElement> stackTrace) {
    return backTraceCache.getIfPresent(stackTrace);
  }

  static void putBackTraceString(List<StackTraceElement> stackTrace, String stackTraceString) {
    backTraceCache.put(stackTrace, stackTraceString);
  }
}
