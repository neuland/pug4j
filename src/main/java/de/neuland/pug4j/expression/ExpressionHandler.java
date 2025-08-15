package de.neuland.pug4j.expression;

import de.neuland.pug4j.exceptions.ExpressionException;
import de.neuland.pug4j.model.PugModel;

/**
 * Interface for expression handlers that evaluate expressions in Pug templates.
 * 
 * Expression handlers are responsible for parsing and evaluating expressions
 * written in a specific language or syntax (e.g., JEXL, JavaScript).
 * 
 * Implementations of this interface should be thread-safe.
 */
public interface ExpressionHandler {
    /**
     * Evaluates an expression and converts the result to a Boolean value.
     * 
     * @param expression the expression to evaluate
     * @param model the model containing variables that can be used in the expression
     * @return the result of the expression as a Boolean
     * @throws ExpressionException if the expression cannot be evaluated
     */
    Boolean evaluateBooleanExpression(String expression, PugModel model) throws ExpressionException;

    /**
     * Evaluates an expression and returns the result as an Object.
     * 
     * @param expression the expression to evaluate
     * @param model the model containing variables that can be used in the expression
     * @return the result of the expression
     * @throws ExpressionException if the expression cannot be evaluated
     */
    Object evaluateExpression(String expression, PugModel model) throws ExpressionException;

    /**
     * Evaluates an expression and converts the result to a String.
     * 
     * @param expression the expression to evaluate
     * @param model the model containing variables that can be used in the expression
     * @return the result of the expression as a String
     * @throws ExpressionException if the expression cannot be evaluated
     */
    String evaluateStringExpression(String expression, PugModel model) throws ExpressionException;

    /**
     * Validates that an expression is syntactically correct without evaluating it.
     * 
     * @param expression the expression to validate
     * @throws ExpressionException if the expression is not valid
     */
    void assertExpression(String expression) throws ExpressionException;

    /**
     * Enables or disables caching of compiled expressions.
     * 
     * @param cache true to enable caching, false to disable
     */
    void setCache(boolean cache);

    /**
     * Clears any cached compiled expressions.
     */
    void clearCache();
}

