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
  private final JexlExpressionHandlerOptions options = new JexlExpressionHandlerOptions();
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
    jexl = getJexlEngine(options);
  }

  public JexlExpressionHandler(JexlExpressionHandlerOptions options) {
    jexl = getJexlEngine(options);
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

  private String removeVar(String expression) {
    expression = expression.replace("var ", ";");
    expression = expression.replace("let ", ";");
    expression = expression.replace("const ", ";");
    return expression;
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
