package com.appoptics.opentelemetry.extensions.transaction;

import com.solarwinds.joboe.config.ConfigParser;
import com.solarwinds.joboe.config.InvalidConfigException;
import com.solarwinds.shaded.google.gson.Gson;
import com.solarwinds.shaded.google.gson.GsonBuilder;
import com.solarwinds.shaded.google.gson.JsonSyntaxException;
import com.solarwinds.shaded.google.gson.reflect.TypeToken;
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
