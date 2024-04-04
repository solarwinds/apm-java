package com.solarwinds.opentelemetry.extensions;

import java.util.List;
import lombok.Value;

@Value
public class TransactionNamingScheme {
  String scheme;
  String delimiter;
  List<String> attributes;
}
