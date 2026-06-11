package de.neuland.pug4j.expression;

import de.neuland.pug4j.exceptions.ExpressionException;
import de.neuland.pug4j.model.PugModel;

/** Created by christoph on 27.10.15. */
public interface ExpressionHandler {
  Boolean evaluateBooleanExpression(String expression, PugModel model) throws ExpressionException;

  Object evaluateExpression(String expression, PugModel model) throws ExpressionException;

  String evaluateStringExpression(String expression, PugModel model) throws ExpressionException;

  void assertExpression(String expression) throws ExpressionException;

  void setCache(boolean cache);

  void clearCache();

  /**
   * Wraps the Java-side block renderer into the object that is exposed to the expression language
   * when a buffered code block contains nested pug content, e.g. a {@code forEach} callback
   * followed by indented pug lines.
   *
   * <p>The default implementation exposes the {@link Runnable} directly; the generated code from
   * {@link #getBlockInvocation(String)} then calls {@code run()} on it. Handlers that can provide
   * access to the surrounding lexical scope (such as {@code GraalJsExpressionHandler}) may return a
   * richer callback instead.
   *
   * @param blockRenderer renders the nested pug block against the current model
   * @param model the model the block will be rendered with
   * @return the object stored in the model and invoked from the buffered expression
   */
  default Object createBlockCallback(Runnable blockRenderer, PugModel model) {
    return blockRenderer;
  }

  /**
   * Returns the statement injected into buffered code to invoke the block callback created by
   * {@link #createBlockCallback(Runnable, PugModel)}. {@code callbackKey} is the model key under
   * which the callback is stored.
   */
  default String getBlockInvocation(String callbackKey) {
    return callbackKey + ".run();";
  }
}
