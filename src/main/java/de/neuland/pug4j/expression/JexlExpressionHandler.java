package de.neuland.pug4j.expression;

import de.neuland.pug4j.exceptions.ExpressionException;
import de.neuland.pug4j.jexl3.PugJexlArithmetic;
import de.neuland.pug4j.jexl3.PugJexlBuilder;
import de.neuland.pug4j.jexl3.RecordWrapperUberspect;
import de.neuland.pug4j.model.PugModel;
import java.util.Map;
import org.apache.commons.jexl3.*;
import org.apache.commons.jexl3.introspection.JexlPermissions;
import org.apache.commons.jexl3.introspection.JexlUberspect;
import org.apache.commons.logging.LogFactory;

public class JexlExpressionHandler extends AbstractExpressionHandler {

  private static final int MAX_ENTRIES = 5000;
  private JexlEngine jexl;
  private final JexlExpressionHandlerOptions options;
  private final RecordWrapperUberspect pugUberspect =
      new RecordWrapperUberspect(
          LogFactory.getLog(JexlExpressionHandler.class),
          (op, obj) -> {
            // RecordWrapper should be treated as the underlying record object (POJO), not as a Map
            // This allows JEXL to call methods on the wrapped record
            if (obj instanceof de.neuland.pug4j.model.RecordWrapper) {
              return JexlUberspect.POJO;
            }
            if (obj instanceof Map) {
              return JexlUberspect.MAP;
            }
            if (op == JexlOperator.ARRAY_GET) {
              return JexlUberspect.MAP;
            } else if (op == JexlOperator.ARRAY_SET) {
              return JexlUberspect.MAP;
            } else {
              return JexlUberspect.POJO;
            }
          },
          JexlPermissions.UNRESTRICTED);

  private final PugJexlArithmetic pugJexlArithmetic = new PugJexlArithmetic(false);

  public JexlExpressionHandler() {
    this(new JexlExpressionHandlerOptions());
  }

  /**
   * Creates a handler with a specific expression cache size.
   *
   * @param cacheSize the cache size (0 to disable, positive value to enable with specific size)
   * @throws IllegalArgumentException if cacheSize is negative
   */
  public JexlExpressionHandler(int cacheSize) {
    this(optionsWithCacheSize(cacheSize));
  }

  public JexlExpressionHandler(JexlExpressionHandlerOptions options) {
    this.options = options;
    jexl = getJexlEngine(options);
  }

  private static JexlExpressionHandlerOptions optionsWithCacheSize(int cacheSize) {
    if (cacheSize < 0) {
      throw new IllegalArgumentException("cacheSize must be non-negative");
    }
    JexlExpressionHandlerOptions options = new JexlExpressionHandlerOptions();
    options.setCache(cacheSize);
    return options;
  }

  private JexlEngine getJexlEngine(JexlExpressionHandlerOptions options) {
    return getPugJexlBuilder(options).create();
  }

  private JexlBuilder getPugJexlBuilder(JexlExpressionHandlerOptions options) {
    return new PugJexlBuilder()
        .arithmetic(pugJexlArithmetic)
        .uberspect(pugUberspect)
        .safe(true)
        .silent(false)
        .strict(false)
        .cacheThreshold(options.getCacheThreshold())
        .cache(options.getCache())
        .debug(options.isDebug());
  }

  public Boolean evaluateBooleanExpression(String expression, PugModel model)
      throws ExpressionException {
    return BooleanUtil.convert(evaluateExpression(expression, model));
  }

  public Object evaluateExpression(String expression, PugModel model) throws ExpressionException {
    try {
      saveLocalVariableName(expression, model);
      expression = removeVar(expression);
      JexlScript e = jexl.createScript(expression);
      MapContext jexlContext = new MapContext(model);
      return e.execute(jexlContext);
    } catch (JexlException e) {
      throw new ExpressionException(expression, e);
    }
  }

  /**
   * Replaces {@code var}/{@code let}/{@code const} declaration keywords with {@code ;} so the
   * declared variables resolve through the model context instead of becoming JEXL script locals.
   * Keywords inside string literals are left untouched, and a keyword must start at a word boundary
   * ({@code myvar x} is not a declaration).
   */
  private static String removeVar(String expression) {
    if (!expression.contains("var ")
        && !expression.contains("let ")
        && !expression.contains("const ")) {
      return expression;
    }
    StringBuilder result = new StringBuilder(expression.length());
    char inString = 0;
    for (int i = 0; i < expression.length(); i++) {
      char c = expression.charAt(i);
      if (inString != 0) {
        result.append(c);
        if (c == '\\' && i + 1 < expression.length()) {
          result.append(expression.charAt(++i));
        } else if (c == inString) {
          inString = 0;
        }
        continue;
      }
      if (c == '\'' || c == '"' || c == '`') {
        inString = c;
        result.append(c);
        continue;
      }
      int keywordLength = declarationKeywordLength(expression, i);
      if (keywordLength > 0) {
        result.append(';');
        i += keywordLength - 1; // skip keyword and the following space; loop adds one more
      } else {
        result.append(c);
      }
    }
    return result.toString();
  }

  /**
   * Length of a declaration keyword incl. trailing space at {@code i}, or 0 if none starts here.
   */
  private static int declarationKeywordLength(String expression, int i) {
    if (i > 0 && Character.isJavaIdentifierPart(expression.charAt(i - 1))) {
      return 0;
    }
    if (expression.startsWith("var ", i) || expression.startsWith("let ", i)) {
      return 4;
    }
    if (expression.startsWith("const ", i)) {
      return 6;
    }
    return 0;
  }

  public void assertExpression(String expression) throws ExpressionException {
    try {
      jexl.createExpression(expression);
    } catch (JexlException e) {
      throw new ExpressionException(expression, e);
    }
  }

  public String evaluateStringExpression(String expression, PugModel model)
      throws ExpressionException {
    Object result = evaluateExpression(expression, model);
    return result == null ? "" : result.toString();
  }

  public void setCache(boolean cache) {
    if (cache) options.setCache(MAX_ENTRIES);
    else options.setCache(0);
    jexl = getJexlEngine(options);
  }

  /**
   * Sets the expression cache to a specific size. A cache size of 0 disables caching.
   *
   * @param cacheSize the cache size (0 to disable, positive value to enable with specific size)
   * @throws IllegalArgumentException if cacheSize is negative
   */
  public void setCacheSize(int cacheSize) {
    if (cacheSize < 0) {
      throw new IllegalArgumentException("cacheSize must be non-negative");
    }
    options.setCache(cacheSize);
    jexl = getJexlEngine(options);
  }

  /**
   * Gets the current expression cache size.
   *
   * @return the cache size
   */
  public int getCacheSize() {
    return options.getCache();
  }

  public void clearCache() {
    jexl.clearCache();
  }
}
