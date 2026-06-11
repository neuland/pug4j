package de.neuland.pug4j.filter;

import java.util.Map;

@FunctionalInterface
public interface Filter {
  String convert(String source, Map<String, Object> options, Map<String, Object> model);
}
