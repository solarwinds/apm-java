package com.appoptics.opentelemetry.extensions.initialize;

import com.appoptics.opentelemetry.extensions.AppOpticsPropertiesSupplier;
import com.appoptics.opentelemetry.extensions.AppOpticsTracerProviderCustomizer;
import com.google.auto.service.AutoService;
import com.tracelytics.joboe.config.InvalidConfigException;
import com.tracelytics.logging.Logger;
import com.tracelytics.logging.LoggerFactory;
import com.tracelytics.util.JavaRuntimeVersionChecker;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizer;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizerProvider;

import javax.annotation.Nonnull;

@AutoService({AutoConfigurationCustomizerProvider.class})
public class OtelAutoConfigurationCustomizerProviderImpl implements AutoConfigurationCustomizerProvider {
    private static final Logger logger = LoggerFactory.getLogger();

    private static boolean agentEnabled;

    static {
        try {
            agentEnabled = JavaRuntimeVersionChecker.isJdkVersionSupported();
            if (agentEnabled) {
                AppOpticsConfigurationLoader.load();
            } else {
                logger.warn(String.format("Unsupported Java runtime version: %s", System.getProperty("java.version")));
            }

        } catch (InvalidConfigException invalidConfigException) {
            logger.warn("Error loading agent config", invalidConfigException);
            agentEnabled = false;
        }

        if (!agentEnabled) {
            logger.warn("Solarwinds' extension is disabled");
        }
    }

    public static boolean isAgentEnabled() {
        return agentEnabled;
    }

    public static void setAgentEnabled(boolean agentEnabled) {
        OtelAutoConfigurationCustomizerProviderImpl.agentEnabled =
                OtelAutoConfigurationCustomizerProviderImpl.agentEnabled && agentEnabled;
    }

    @Override
    public void customize(@Nonnull AutoConfigurationCustomizer autoConfiguration) {
        autoConfiguration.addPropertiesSupplier(new AppOpticsPropertiesSupplier())
                .addTracerProviderCustomizer(new AppOpticsTracerProviderCustomizer());
    }

    @Override
    public int order() {
        // Here, we return Integer.MAX_VALUE to force our extension customization to execute last.
        // See https://github.com/appoptics/solarwinds-apm-java/pull/93#discussion_r1165987329 for more context
        return Integer.MAX_VALUE;
    }
}
