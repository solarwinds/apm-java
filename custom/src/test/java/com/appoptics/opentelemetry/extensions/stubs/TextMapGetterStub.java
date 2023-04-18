package com.appoptics.opentelemetry.extensions.stubs;

import io.opentelemetry.context.propagation.TextMapGetter;

import java.util.Map;

@SuppressWarnings("all")
public class TextMapGetterStub implements TextMapGetter<Map<String,String>> {
    @Override
    public Iterable<String> keys( Map<String, String> carrier) {
        return carrier.keySet();
    }

    @Override
    public String get(Map<String, String> carrier,  String key) {
        assert carrier != null;
        return carrier.get(key);
    }
}
