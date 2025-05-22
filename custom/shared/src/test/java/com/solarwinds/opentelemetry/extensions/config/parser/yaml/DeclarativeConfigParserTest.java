package com.solarwinds.opentelemetry.extensions.config.parser.yaml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.solarwinds.joboe.config.ConfigContainer;
import com.solarwinds.joboe.config.ConfigProperty;
import com.solarwinds.joboe.config.InvalidConfigException;
import com.solarwinds.joboe.logging.LogSetting;
import com.solarwinds.joboe.logging.Logger;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeclarativeConfigParserTest {

  @Mock private DeclarativeConfigProperties declarativeConfigPropertiesMock;

  @Test
  void throwForUnknownConfig() {
    DeclarativeConfigParser parser = new DeclarativeConfigParser(new ConfigContainer());
    Set<String> keys = new HashSet<>(Arrays.asList("unknown-key1", "key2"));

    when(declarativeConfigPropertiesMock.getPropertyKeys()).thenReturn(keys);
    assertThrows(InvalidConfigException.class, () -> parser.parse(declarativeConfigPropertiesMock));
  }

  @Test
  void testStringParsing() throws InvalidConfigException {
    ConfigContainer configContainer = new ConfigContainer();
    DeclarativeConfigParser parser = new DeclarativeConfigParser(configContainer);
    Set<String> keys = new HashSet<>(Collections.singletonList("agent.serviceKey"));

    when(declarativeConfigPropertiesMock.getPropertyKeys()).thenReturn(keys);
    when(declarativeConfigPropertiesMock.getString(eq("agent.serviceKey")))
        .thenReturn("token:service");

    parser.parse(declarativeConfigPropertiesMock);
    assertEquals("token:service", configContainer.get(ConfigProperty.AGENT_SERVICE_KEY));
  }

  @Test
  void testBooleanParsing() throws InvalidConfigException {
    ConfigContainer configContainer = new ConfigContainer();
    DeclarativeConfigParser parser = new DeclarativeConfigParser(configContainer);
    Set<String> keys = new HashSet<>(Collections.singletonList("agent.sqlTagPrepared"));

    when(declarativeConfigPropertiesMock.getPropertyKeys()).thenReturn(keys);
    when(declarativeConfigPropertiesMock.getBoolean(eq("agent.sqlTagPrepared"))).thenReturn(false);

    parser.parse(declarativeConfigPropertiesMock);
    assertEquals(false, configContainer.get(ConfigProperty.AGENT_SQL_TAG_PREPARED));
  }

  @Test
  void testIntegerParsing() throws InvalidConfigException {
    ConfigContainer configContainer = new ConfigContainer();
    DeclarativeConfigParser parser = new DeclarativeConfigParser(configContainer);
    Set<String> keys = new HashSet<>(Collections.singletonList("agent.collectorTimeout"));

    when(declarativeConfigPropertiesMock.getPropertyKeys()).thenReturn(keys);
    when(declarativeConfigPropertiesMock.getInt(eq("agent.collectorTimeout"))).thenReturn(10);

    parser.parse(declarativeConfigPropertiesMock);
    assertEquals(10, configContainer.get(ConfigProperty.AGENT_COLLECTOR_TIMEOUT));
  }

  @Test
  void testLongParsing() throws InvalidConfigException {
    ConfigContainer configContainer = new ConfigContainer();
    DeclarativeConfigParser parser = new DeclarativeConfigParser(configContainer);
    Set<String> keys = new HashSet<>(Collections.singletonList("agent.configFileWatchPeriod"));

    when(declarativeConfigPropertiesMock.getPropertyKeys()).thenReturn(keys);
    when(declarativeConfigPropertiesMock.getLong(eq("agent.configFileWatchPeriod")))
        .thenReturn(1000000L);

    parser.parse(declarativeConfigPropertiesMock);
    assertEquals(1000000L, configContainer.get(ConfigProperty.AGENT_CONFIG_FILE_WATCH_PERIOD));
  }

  @Test
  void testStructuredParsing() throws InvalidConfigException {
    ConfigContainer configContainer = new ConfigContainer();
    DeclarativeConfigParser parser = new DeclarativeConfigParser(configContainer);
    Set<String> keys = new HashSet<>(Collections.singletonList("agent.logging"));

    when(declarativeConfigPropertiesMock.getPropertyKeys()).thenReturn(keys);
    when(declarativeConfigPropertiesMock.getStructured(any(), any()))
        .thenReturn(declarativeConfigPropertiesMock);

    when(declarativeConfigPropertiesMock.getString(any())).thenReturn(null);
    when(declarativeConfigPropertiesMock.getString(eq("level"), any())).thenReturn("debug");
    parser.parse(declarativeConfigPropertiesMock);

    verify(declarativeConfigPropertiesMock, atLeastOnce())
        .getStructured(eq("agent.logging"), any());

    LogSetting logSetting = (LogSetting) configContainer.get(ConfigProperty.AGENT_LOGGING);
    assertEquals(Logger.Level.DEBUG, logSetting.getLevel());
  }
}
