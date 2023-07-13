package com.appoptics.opentelemetry.extensions.transaction;

import io.opentelemetry.api.common.Attributes;

public class DefaultNamingScheme extends NamingScheme{
    public DefaultNamingScheme(NamingScheme next) {
        super(next);
    }

    @Override
    public String createName(Attributes attributes) {
        return null;
    }
}
