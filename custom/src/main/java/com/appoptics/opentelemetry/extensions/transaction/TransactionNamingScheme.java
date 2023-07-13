package com.appoptics.opentelemetry.extensions.transaction;

import lombok.Value;

import java.util.List;

@Value
public class TransactionNamingScheme {
    String scheme;
    String delimiter;
    List<String> attributes;
}
