package com.appoptics.opentelemetry.extensions.lambda;

import java.util.Collections;
import java.util.Map;
import java.util.function.Supplier;

import static com.tracelytics.util.HostTypeDetector.isLambda;

public class PropertiesSupplier implements Supplier<Map<String, String>> {
    private final Supplier<Map<String, String>> delegate;

    public PropertiesSupplier(Supplier<Map<String, String>> delegate) {
        this.delegate = delegate;
    }

    @Override
    public Map<String, String> get() {
        if(isLambda()){
            return Collections.emptyMap();
        }
        return delegate.get();
    }
}
