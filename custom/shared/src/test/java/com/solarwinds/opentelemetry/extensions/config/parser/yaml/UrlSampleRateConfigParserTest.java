package com.solarwinds.opentelemetry.extensions.config.parser.yaml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.solarwinds.joboe.config.InvalidConfigException;
import com.solarwinds.joboe.sampling.TraceConfigs;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.sdk.extension.incubator.fileconfig.DeclarativeConfiguration;
import java.io.IOException;
import java.io.InputStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UrlSampleRateConfigParserTest {
  private final UrlSampleRateConfigParser tested = new UrlSampleRateConfigParser();

  @Test
  void testConvert() throws InvalidConfigException {
    try (InputStream resourceAsStream =
        UrlSampleRateConfigParserTest.class.getResourceAsStream("/sdk-config.yaml")) {
      DeclarativeConfigProperties configProperties =
          DeclarativeConfiguration.toConfigProperties(resourceAsStream);
      DeclarativeConfigProperties declarativeConfigProperties =
          configProperties
              .getStructured("instrumentation/development", DeclarativeConfigProperties.empty())
              .getStructured("java", DeclarativeConfigProperties.empty())
              .getStructured("solarwinds", DeclarativeConfigProperties.empty());

      TraceConfigs traceConfigs = tested.convert(declarativeConfigProperties);
      assertNotNull(traceConfigs);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @ParameterizedTest
  @MethodSource("paths")
  void testConvertWithException(String configPath) {
    try (InputStream resourceAsStream =
        UrlSampleRateConfigParserTest.class.getResourceAsStream(configPath)) {
      DeclarativeConfigProperties configProperties =
          DeclarativeConfiguration.toConfigProperties(resourceAsStream);

      DeclarativeConfigProperties declarativeConfigProperties =
          configProperties
              .getStructured("instrumentation/development", DeclarativeConfigProperties.empty())
              .getStructured("java", DeclarativeConfigProperties.empty())
              .getStructured("solarwinds", DeclarativeConfigProperties.empty());

      assertThrows(InvalidConfigException.class, () -> tested.convert(declarativeConfigProperties));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  void configKey() {
    assertEquals("agent.urlSampleRates", tested.configKey());
  }

  private Stream<Arguments> paths() {
    return Stream.of(
        Arguments.of("/sdk-config-bad-transaction-settings-0.yaml"),
        Arguments.of("/sdk-config-bad-transaction-settings-1.yaml"));
  }
}
