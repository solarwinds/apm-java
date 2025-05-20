package com.solarwinds.opentelemetry.extensions.config.parsers.yaml;

import com.solarwinds.joboe.config.ConfigContainer;
import com.solarwinds.joboe.config.ConfigParser;
import com.solarwinds.joboe.config.ConfigProperty;
import com.solarwinds.joboe.config.InvalidConfigException;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;

@SuppressWarnings("unchecked")
public final class DeclarativeConfigParser {
  private final ConfigContainer configContainer;

  private static final Map<String, ConfigParser<DeclarativeConfigProperties, Object>> register =
      new HashMap<>();

  static {
    for (ConfigParser<?, ?> parser :
        ServiceLoader.load(ConfigParser.class, DeclarativeConfigParser.class.getClassLoader())) {
      register.put(parser.configKey(), (ConfigParser<DeclarativeConfigProperties, Object>) parser);
    }
  }

  public DeclarativeConfigParser(ConfigContainer configContainer) {
    this.configContainer = configContainer;
  }

  public void parse(DeclarativeConfigProperties declarativeConfigProperties)
      throws InvalidConfigException {
    Set<String> propertyKeys = declarativeConfigProperties.getPropertyKeys();
    for (String key : propertyKeys) {
      ConfigProperty configProperty = ConfigProperty.fromConfigFileKey(key);
      if (configProperty != null) {
        Object parsed = null;
        Class<?> typeClass = configProperty.getTypeClass();

        if (typeClass == String.class) {
          parsed = declarativeConfigProperties.getString(key);
        } else if (typeClass == Boolean.class) {
          parsed = declarativeConfigProperties.getBoolean(key);
        } else if (typeClass == Integer.class) {
          parsed = declarativeConfigProperties.getInt(key);
        }

        if (parsed == null && register.containsKey(key)) {
          ConfigParser<DeclarativeConfigProperties, Object> configParser = register.get(key);
          parsed = configParser.convert(declarativeConfigProperties);
        }

        configContainer.put(configProperty, parsed);
      }
    }
  }
}
