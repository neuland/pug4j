package de.neuland.pug4j.filter;

import java.util.Map;

public class CustomTestFilter implements Filter {
  @Override
  public String convert(String source, Map<String, Object> attributes, Map<String, Object> model) {

    Object foo = attributes.get("foo");
    if (foo != null) {
      String test = null;
      if (foo instanceof String) test = (String) foo;
      if ("foo bar".equals(source) && "bar".equals(test)) {
        return "bar baz";
      }
    }
    return source;
  }
}
