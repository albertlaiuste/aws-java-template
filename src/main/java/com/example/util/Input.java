package com.example.util;

import java.util.Map;
import java.util.Optional;

public class Input {

  public static String validatePathParameter(Object pathParameters, String key)
      throws IllegalArgumentException {
    String parameter = Optional.ofNullable(pathParameters)
        .filter(Map.class::isInstance)
        .map(Map.class::cast)
        .map(map -> map.get(key))
        .filter(String.class::isInstance)
        .map(String.class::cast)
        .orElse(null);

    if (parameter == null || !parameter.matches("^[a-zA-Z0-9-_]{1,128}$")) {
      throw new IllegalArgumentException("Invalid Parameter Name");
    }

    return parameter;
  }
}
