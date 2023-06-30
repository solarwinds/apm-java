package com.appoptics.opentelemetry.instrumentation.guidewire;

import com.google.auto.service.AutoService;
import io.opentelemetry.javaagent.extension.instrumentation.InstrumentationModule;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;

import java.util.Collections;
import java.util.List;

@AutoService(InstrumentationModule.class)
public class GuidewireInstrumentationModule extends InstrumentationModule {
    private static final String NAME = "sw-guidewire";

    public GuidewireInstrumentationModule() {
        super(NAME);
    }

    @Override
    public List<TypeInstrumentation> typeInstrumentations() {
        return Collections.singletonList(new GuidewireTypeInstrumentation());
    }

    @Override
    public boolean isHelperClass(String className) {
        return className.startsWith("com.appoptics.opentelemetry.instrumentation.guidewire");
    }
}
