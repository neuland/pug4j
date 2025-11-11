package de.neuland.pug4j.filter;

import java.util.Map;

public class PlainFilter implements Filter {

  @Override
  public String convert(String source, Map<String, Object> attributes, Map<String, Object> model) {
    return source;
  }
}
