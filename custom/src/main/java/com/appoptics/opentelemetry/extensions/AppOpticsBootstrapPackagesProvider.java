package com.appoptics.opentelemetry.extensions;

import com.google.auto.service.AutoService;
import io.opentelemetry.javaagent.spi.BootstrapPackagesProvider;

import java.util.Arrays;
import java.util.List;

@AutoService(BootstrapPackagesProvider.class)
public class AppOpticsBootstrapPackagesProvider implements BootstrapPackagesProvider {

    @Override
    public List<String> getPackagePrefixes() {
        return Arrays.asList("com.tracelytics");
    }
}
