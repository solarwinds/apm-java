package com.appoptics.opentelemetry.extensions.transaction;


import com.tracelytics.ext.google.gson.Gson;
import com.tracelytics.ext.google.gson.GsonBuilder;
import com.tracelytics.ext.google.gson.JsonSyntaxException;
import com.tracelytics.ext.google.gson.reflect.TypeToken;
import com.tracelytics.joboe.config.ConfigParser;
import com.tracelytics.joboe.config.InvalidConfigException;

import java.lang.reflect.Type;
import java.util.List;

public final class TransactionNamingSchemesParser implements ConfigParser<String, List<TransactionNamingScheme>> {
    private static final Gson gson = new GsonBuilder().create();

    @Override
    public List<TransactionNamingScheme> convert(String input) throws InvalidConfigException {
        try {
            Type type = new TypeToken<List<TransactionNamingScheme>>() {
            }.getType();
            return gson.fromJson(input, type);
        } catch (JsonSyntaxException e) {
            throw new InvalidConfigException(e);
        }
    }
}
