package com.solarwinds.containers;

import org.testcontainers.containers.GenericContainer;

public interface Container {
    GenericContainer<?> build();
}
