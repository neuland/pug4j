package de.neuland.pug4j.expression;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Scans a template expression for a leading {@code var}/{@code let}/{@code const} declaration
 * without parsing JavaScript. Tracks bracket depth and string literals, so initializers containing
 * {@code ==}, arrow functions, commas inside calls, or declaration keywords inside strings are
 * handled correctly. Only a declaration at the very start of the expression is recognized, by
 * design — no full JS parsing.
 *
 * <p>Used to register declared names for pug scoping (see {@link
 * AbstractExpressionHandler#saveLocalVariableName}) and to rewrite declarations into plain
 * assignments for the GraalJS handler.
 */
final class DeclarationScanner {

  static final class Declarator {
    /** Declared names; empty if not extractable (complex destructuring pattern). */
    final List<String> names;

    final boolean destructuring;
    final boolean hasInitializer;

    /** Original declarator text, trimmed. */
    final String source;

    private Declarator(
        List<String> names, boolean destructuring, boolean hasInitializer, String source) {
      this.names = names;
      this.destructuring = destructuring;
      this.hasInitializer = hasInitializer;
      this.source = source;
    }
  }

  static final class Result {
    final String keyword;

    /** Exclusive end of the declaration statement within the original expression. */
    final int boundary;

    final List<Declarator> declarators;

    private Result(String keyword, int boundary, List<Declarator> declarators) {
      this.keyword = keyword;
      this.boundary = boundary;
      this.declarators = declarators;
    }
  }

  private DeclarationScanner() {}

  /** Returns the parsed leading declaration, or {@code null} if the expression has none. */
  static Result scan(String expression) {
    int len = expression.length();
    int i = 0;
    while (i < len && Character.isWhitespace(expression.charAt(i))) {
      i++;
    }
    String keyword = null;
    for (String kw : new String[] {"var", "let", "const"}) {
      if (expression.startsWith(kw, i)
          && i + kw.length() < len
          && Character.isWhitespace(expression.charAt(i + kw.length()))) {
        keyword = kw;
        break;
      }
    }
    if (keyword == null) {
      return null;
    }
    int declStart = i + keyword.length();
    while (declStart < len && Character.isWhitespace(expression.charAt(declStart))) {
      declStart++;
    }
    if (declStart >= len) {
      return null;
    }

    int boundary = len;
    List<Integer> commas = new ArrayList<>();
    int depth = 0;
    char inString = 0;
    scan:
    for (int p = declStart; p < len; p++) {
      char c = expression.charAt(p);
      if (inString != 0) {
        if (c == '\\') {
          p++;
        } else if (c == inString) {
          inString = 0;
        }
        continue;
      }
      switch (c) {
        case '\'':
        case '"':
        case '`':
          inString = c;
          break;
        case '(':
        case '[':
        case '{':
          depth++;
          break;
        case ')':
        case ']':
        case '}':
          depth--;
          break;
        case ',':
          if (depth == 0) {
            commas.add(p);
          }
          break;
        case ';':
        case '\n':
          if (depth == 0) {
            boundary = p;
            break scan;
          }
          break;
        default:
      }
    }

    List<Declarator> declarators = new ArrayList<>();
    int segStart = declStart;
    List<Integer> segEnds = new ArrayList<>(commas);
    segEnds.add(boundary);
    for (int segEnd : segEnds) {
      String segment = expression.substring(segStart, segEnd).trim();
      if (!segment.isEmpty()) {
        declarators.add(parseDeclarator(segment));
      }
      segStart = segEnd + 1;
    }
    if (declarators.isEmpty()) {
      return null;
    }
    return new Result(keyword, boundary, declarators);
  }

  private static Declarator parseDeclarator(String segment) {
    char first = segment.charAt(0);
    if (first == '{' || first == '[') {
      int closing = findMatchingBracket(segment);
      List<String> names =
          closing < 0
              ? Collections.emptyList()
              : extractFlatPatternNames(segment.substring(1, closing));
      boolean init = closing >= 0 && hasInitializer(segment, closing + 1);
      return new Declarator(names, true, init, segment);
    }
    int p = 0;
    while (p < segment.length() && isIdentifierPart(segment.charAt(p), p == 0)) {
      p++;
    }
    String name = segment.substring(0, p);
    List<String> names = name.isEmpty() ? Collections.emptyList() : List.of(name);
    return new Declarator(names, false, hasInitializer(segment, p), segment);
  }

  private static boolean hasInitializer(String segment, int from) {
    int p = from;
    while (p < segment.length() && Character.isWhitespace(segment.charAt(p))) {
      p++;
    }
    if (p >= segment.length() || segment.charAt(p) != '=') {
      return false;
    }
    char next = p + 1 < segment.length() ? segment.charAt(p + 1) : 0;
    return next != '=' && next != '>';
  }

  private static int findMatchingBracket(String segment) {
    char open = segment.charAt(0);
    char close = open == '{' ? '}' : ']';
    int depth = 0;
    char inString = 0;
    for (int p = 0; p < segment.length(); p++) {
      char c = segment.charAt(p);
      if (inString != 0) {
        if (c == '\\') {
          p++;
        } else if (c == inString) {
          inString = 0;
        }
        continue;
      }
      if (c == '\'' || c == '"' || c == '`') {
        inString = c;
      } else if (c == open) {
        depth++;
      } else if (c == close) {
        depth--;
        if (depth == 0) {
          return p;
        }
      }
    }
    return -1;
  }

  /**
   * Extracts names from a flat destructuring pattern body like {@code a, b} or {@code a: x, b}.
   * Returns an empty list for nested patterns, defaults, or anything else it cannot read —
   * conservative under-registration, never a wrong name.
   */
  private static List<String> extractFlatPatternNames(String patternBody) {
    List<String> names = new ArrayList<>();
    for (String part : patternBody.split(",")) {
      String candidate = part.trim();
      if (candidate.isEmpty()) {
        continue;
      }
      if (candidate.indexOf('{') >= 0 || candidate.indexOf('[') >= 0 || candidate.indexOf('=') >= 0) {
        return Collections.emptyList();
      }
      int colon = candidate.indexOf(':');
      if (colon >= 0) {
        candidate = candidate.substring(colon + 1).trim();
      }
      if (candidate.startsWith("...")) {
        candidate = candidate.substring(3).trim();
      }
      if (!isIdentifier(candidate)) {
        return Collections.emptyList();
      }
      names.add(candidate);
    }
    return names;
  }

  private static boolean isIdentifier(String s) {
    if (s.isEmpty() || !isIdentifierPart(s.charAt(0), true)) {
      return false;
    }
    for (int p = 1; p < s.length(); p++) {
      if (!isIdentifierPart(s.charAt(p), false)) {
        return false;
      }
    }
    return true;
  }

  private static boolean isIdentifierPart(char c, boolean start) {
    if (c == '$' || c == '_' || (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z')) {
      return true;
    }
    return !start && c >= '0' && c <= '9';
  }
}
