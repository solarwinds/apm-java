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
