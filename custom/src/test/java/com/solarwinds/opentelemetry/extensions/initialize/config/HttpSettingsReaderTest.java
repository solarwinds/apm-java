package com.solarwinds.opentelemetry.extensions.initialize.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.solarwinds.joboe.config.ConfigManager;
import com.solarwinds.joboe.config.ConfigProperty;
import com.solarwinds.joboe.config.InvalidConfigException;
import com.solarwinds.joboe.sampling.Settings;
import com.solarwinds.opentelemetry.extensions.ResourceCustomizer;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.semconv.incubating.HostIncubatingAttributes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HttpSettingsReaderTest {

  @Mock private HttpSettingsReaderDelegate delegate;

  @Mock private Settings mockSettings;

  @InjectMocks private HttpSettingsReader tested;

  @BeforeEach
  void setUp() {
    ConfigManager.reset();
  }

  @Test
  void testGetSettings_UrlWithHttps() throws InvalidConfigException {
    String collectorUrl = "https://apm.collector.na-01.cloud.solarwinds.com";
    String serviceKey = "test-api-key:test-service";
    String hostname = "test-hostname";

    String expectedUrl =
        "https://apm.collector.na-01.cloud.solarwinds.com/v1/settings/test-service/test-hostname";
    String expectedTokenHeader = "Bearer test-api-key";
    ConfigManager.setConfig(ConfigProperty.AGENT_COLLECTOR, collectorUrl);

    ConfigManager.setConfig(ConfigProperty.AGENT_SERVICE_KEY, serviceKey);
    try (MockedStatic<ResourceCustomizer> resourceCustomizerMock =
        mockStatic(ResourceCustomizer.class)) {

      Resource mockResource = mock(Resource.class);
      resourceCustomizerMock.when(ResourceCustomizer::getResource).thenReturn(mockResource);
      when(mockResource.getAttribute(HostIncubatingAttributes.HOST_NAME)).thenReturn(hostname);

      when(delegate.fetchSettings(expectedUrl, expectedTokenHeader)).thenReturn(mockSettings);
      Settings result = tested.getSettings();

      assertNotNull(result);
      assertEquals(mockSettings, result);
      verify(delegate).fetchSettings(expectedUrl, expectedTokenHeader);
    }
  }

  @Test
  void testGetSettings_UrlWithoutHttps() throws InvalidConfigException {
    String collectorUrl = "apm.collector.na-01.cloud.solarwinds.com";
    String serviceKey = "test-api-key:test-service";
    String hostname = "test-hostname";

    String expectedUrl =
        "https://apm.collector.na-01.cloud.solarwinds.com/v1/settings/test-service/test-hostname";
    String expectedTokenHeader = "Bearer test-api-key";
    ConfigManager.setConfig(ConfigProperty.AGENT_COLLECTOR, collectorUrl);

    ConfigManager.setConfig(ConfigProperty.AGENT_SERVICE_KEY, serviceKey);
    try (MockedStatic<ResourceCustomizer> resourceCustomizerMock =
        mockStatic(ResourceCustomizer.class)) {
      Resource mockResource = mock(Resource.class);
      resourceCustomizerMock.when(ResourceCustomizer::getResource).thenReturn(mockResource);
      when(mockResource.getAttribute(HostIncubatingAttributes.HOST_NAME)).thenReturn(hostname);

      when(delegate.fetchSettings(expectedUrl, expectedTokenHeader)).thenReturn(mockSettings);
      Settings result = tested.getSettings();

      assertNotNull(result);
      assertEquals(mockSettings, result);
      verify(delegate).fetchSettings(expectedUrl, expectedTokenHeader);
    }
  }

  @Test
  void testGetSettings_UrlWithHttp() throws InvalidConfigException {
    String collectorUrl = "http://apm.collector.na-01.cloud.solarwinds.com";
    String serviceKey = "test-api-key:test-service";
    String hostname = "test-hostname";

    String expectedUrl =
        "http://apm.collector.na-01.cloud.solarwinds.com/v1/settings/test-service/test-hostname";
    String expectedTokenHeader = "Bearer test-api-key";
    ConfigManager.setConfig(ConfigProperty.AGENT_COLLECTOR, collectorUrl);

    ConfigManager.setConfig(ConfigProperty.AGENT_SERVICE_KEY, serviceKey);
    try (MockedStatic<ResourceCustomizer> resourceCustomizerMock =
        mockStatic(ResourceCustomizer.class)) {

      Resource mockResource = mock(Resource.class);
      resourceCustomizerMock.when(ResourceCustomizer::getResource).thenReturn(mockResource);
      when(mockResource.getAttribute(HostIncubatingAttributes.HOST_NAME)).thenReturn(hostname);

      when(delegate.fetchSettings(expectedUrl, expectedTokenHeader)).thenReturn(mockSettings);
      Settings result = tested.getSettings();

      assertNotNull(result);
      assertEquals(mockSettings, result);
      verify(delegate).fetchSettings(expectedUrl, expectedTokenHeader);
    }
  }

  @Test
  void testGetSettings_WithDefaultValues() {
    String hostname = "test-hostname";
    String expectedUrl =
        "https://apm.collector.na-01.cloud.solarwinds.com/v1/settings/unknown-java/test-hostname";
    String expectedTokenHeader = "Bearer ";

    try (MockedStatic<ResourceCustomizer> resourceCustomizerMock =
        mockStatic(ResourceCustomizer.class)) {

      Resource mockResource = mock(Resource.class);
      resourceCustomizerMock.when(ResourceCustomizer::getResource).thenReturn(mockResource);
      when(mockResource.getAttribute(HostIncubatingAttributes.HOST_NAME)).thenReturn(hostname);

      when(delegate.fetchSettings(expectedUrl, expectedTokenHeader)).thenReturn(mockSettings);
      Settings result = tested.getSettings();

      assertNotNull(result);
      assertEquals(mockSettings, result);
      verify(delegate).fetchSettings(expectedUrl, expectedTokenHeader);
    }
  }

  @Test
  void testGetSettings_WithCustomServiceKey() throws InvalidConfigException {
    String collectorUrl = "custom.collector.com";
    String serviceKey = "my-api-key:my-service";
    String hostname = "prod-server";

    String expectedUrl = "https://custom.collector.com/v1/settings/my-service/prod-server";
    String expectedTokenHeader = "Bearer my-api-key";
    ConfigManager.setConfig(ConfigProperty.AGENT_COLLECTOR, collectorUrl);

    ConfigManager.setConfig(ConfigProperty.AGENT_SERVICE_KEY, serviceKey);
    try (MockedStatic<ResourceCustomizer> resourceCustomizerMock =
        mockStatic(ResourceCustomizer.class)) {

      Resource mockResource = mock(Resource.class);
      resourceCustomizerMock.when(ResourceCustomizer::getResource).thenReturn(mockResource);
      when(mockResource.getAttribute(HostIncubatingAttributes.HOST_NAME)).thenReturn(hostname);

      when(delegate.fetchSettings(expectedUrl, expectedTokenHeader)).thenReturn(mockSettings);
      Settings result = tested.getSettings();

      assertNotNull(result);
      assertEquals(mockSettings, result);
      verify(delegate).fetchSettings(expectedUrl, expectedTokenHeader);
    }
  }

  @Test
  void testGetSettings_DelegateThrowsException() throws InvalidConfigException {
    String collectorUrl = "https://apm.collector.na-01.cloud.solarwinds.com";
    String serviceKey = "test-api-key:test-service";
    String hostname = "test-hostname";
    String expectedUrl =
        "https://apm.collector.na-01.cloud.solarwinds.com/v1/settings/test-service/test-hostname";
    String expectedTokenHeader = "Bearer test-api-key";

    ConfigManager.setConfig(ConfigProperty.AGENT_COLLECTOR, collectorUrl);
    ConfigManager.setConfig(ConfigProperty.AGENT_SERVICE_KEY, serviceKey);

    try (MockedStatic<ResourceCustomizer> resourceCustomizerMock =
        mockStatic(ResourceCustomizer.class)) {
      Resource mockResource = mock(Resource.class);
      resourceCustomizerMock.when(ResourceCustomizer::getResource).thenReturn(mockResource);
      when(mockResource.getAttribute(HostIncubatingAttributes.HOST_NAME)).thenReturn(hostname);

      when(delegate.fetchSettings(expectedUrl, expectedTokenHeader))
          .thenThrow(new RuntimeException("Network error"));

      assertThrows(RuntimeException.class, () -> tested.getSettings());
      verify(delegate).fetchSettings(expectedUrl, expectedTokenHeader);
    }
  }

  @Test
  void testGetSettings_UrlWithPortAndPath() throws InvalidConfigException {
    String collectorUrl = "https://custom.collector.com:8080/api";
    String serviceKey = "test-key:test-app";
    String hostname = "server1";
    String expectedUrl = "https://custom.collector.com:8080/api/v1/settings/test-app/server1";
    String expectedTokenHeader = "Bearer test-key";

    ConfigManager.setConfig(ConfigProperty.AGENT_COLLECTOR, collectorUrl);
    ConfigManager.setConfig(ConfigProperty.AGENT_SERVICE_KEY, serviceKey);

    try (MockedStatic<ResourceCustomizer> resourceCustomizerMock =
        mockStatic(ResourceCustomizer.class)) {
      Resource mockResource = mock(Resource.class);
      resourceCustomizerMock.when(ResourceCustomizer::getResource).thenReturn(mockResource);
      when(mockResource.getAttribute(HostIncubatingAttributes.HOST_NAME)).thenReturn(hostname);

      when(delegate.fetchSettings(expectedUrl, expectedTokenHeader)).thenReturn(mockSettings);
      Settings result = tested.getSettings();

      assertNotNull(result);
      assertEquals(mockSettings, result);
      verify(delegate).fetchSettings(expectedUrl, expectedTokenHeader);
    }
  }
}
