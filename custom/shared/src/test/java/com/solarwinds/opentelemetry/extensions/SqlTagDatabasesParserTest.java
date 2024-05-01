package com.solarwinds.opentelemetry.extensions;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.solarwinds.joboe.config.InvalidConfigException;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SqlTagDatabasesParserTest {

  @InjectMocks private SqlTagDatabasesParser tested;

  @Test
  void returnSetGivenCommaSeparatedString() throws InvalidConfigException {
    final String arg = "mysql,postgresql";
    final Set<String> expected =
        new HashSet<>() {
          {
            add("mysql");
            add("postgresql");
          }
        };

    assertEquals(expected, tested.convert(arg));
  }
}
