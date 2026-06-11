package de.neuland.pug4j.compiler;

import org.apache.commons.text.StringEscapeUtils;

/**
 * HTML escaping with a fast path for strings that need no translation. {@code escapeHtml4}
 * translates the four markup characters and non-ASCII characters (ISO-8859-1 and HTML4 extended
 * entities), so input containing neither can be returned as-is without the translator's
 * StringWriter allocation.
 */
final class HtmlEscaping {

  private HtmlEscaping() {}

  static String escapeHtml4(String input) {
    if (input == null) {
      return null;
    }
    for (int i = 0; i < input.length(); i++) {
      char c = input.charAt(i);
      if (c == '&' || c == '<' || c == '>' || c == '"' || c > 127) {
        return StringEscapeUtils.escapeHtml4(input);
      }
    }
    return input;
  }
}
