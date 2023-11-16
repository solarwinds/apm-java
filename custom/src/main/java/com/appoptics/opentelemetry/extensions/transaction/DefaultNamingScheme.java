package com.appoptics.opentelemetry.extensions.transaction;

import com.tracelytics.joboe.config.ConfigManager;
import com.tracelytics.joboe.config.ConfigProperty;
import io.opentelemetry.api.common.Attributes;

public class DefaultNamingScheme extends NamingScheme{
    public DefaultNamingScheme(NamingScheme next) {
        super(next);
    }

    @Override
    public String createName(Attributes attributes) {
        return ConfigManager.getConfigOptional(ConfigProperty.AGENT_TRANSACTION_NAME, null);
    }
}
