package com.solarwinds.opentelemetry.extensions.provider;

import static com.solarwinds.opentelemetry.extensions.ApmResourceProvider.moduleKey;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.solarwinds.opentelemetry.extensions.config.provider.ResourceComponentProvider;
import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.sdk.resources.Resource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ResourceComponentProviderTest {

  private final ResourceComponentProvider tested = new ResourceComponentProvider();

  @Mock private DeclarativeConfigProperties declarativeConfigPropertiesMock;

  @Test
  void testName() {
    assertEquals("swo/resource", tested.getName());
  }

  @Test
  void getResource() {
    Resource resource = tested.create(declarativeConfigPropertiesMock);
    String attribute = resource.getAttribute(moduleKey);
    assertEquals("apm", attribute);
  }
}
