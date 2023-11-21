package com.appoptics.opentelemetry.core;

import com.tracelytics.ext.google.common.cache.Cache;
import com.tracelytics.ext.google.common.cache.CacheBuilder;
import java.util.concurrent.TimeUnit;

public class CustomTransactionNameDict {
  private static final Cache<String, String> dict =
      CacheBuilder.newBuilder()
          .maximumSize(1000)
          .expireAfterAccess(1200, TimeUnit.SECONDS)
          .<String, String>build(); // 20 mins cache

  public static void set(String id, String name) {
    dict.put(id, name);
  }

  public static String get(String id) {
    return dict.getIfPresent(id);
  }
}
