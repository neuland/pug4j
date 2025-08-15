package de.neuland.pug4j.expression;

import de.neuland.pug4j.exceptions.ExpressionException;
import de.neuland.pug4j.model.PugModel;

/**
 * Example of a custom expression handler implementation.
 * 
 * This is a simple example that demonstrates how to create a custom expression handler.
 * It doesn't do any real expression evaluation but shows the structure required.
 */
public class CustomExpressionHandlerExample extends AbstractExpressionHandler {
    
    /**
     * Evaluates a boolean expression.
     * 
     * @param expression the expression to evaluate
     * @param model the model containing variables
     * @return the result as a Boolean
     * @throws ExpressionException if the expression cannot be evaluated
     */
    @Override
    public Boolean evaluateBooleanExpression(String expression, PugModel model) throws ExpressionException {
        // Simple implementation that returns true for "true" and false for everything else
        return "true".equals(expression);
    }

    /**
     * Evaluates an expression and returns the result.
     * 
     * @param expression the expression to evaluate
     * @param model the model containing variables
     * @return the result of the expression
     * @throws ExpressionException if the expression cannot be evaluated
     */
    @Override
    public Object evaluateExpression(String expression, PugModel model) throws ExpressionException {
        // Save local variable names for variable declarations
        saveLocalVariableName(expression, model);
        
        // Simple implementation that returns the expression itself
        return expression;
    }

    /**
     * Evaluates an expression and returns the result as a string.
     * 
     * @param expression the expression to evaluate
     * @param model the model containing variables
     * @return the result as a String
     * @throws ExpressionException if the expression cannot be evaluated
     */
    @Override
    public String evaluateStringExpression(String expression, PugModel model) throws ExpressionException {
        Object result = evaluateExpression(expression, model);
        return result == null ? "" : result.toString();
    }

    /**
     * Validates that an expression is syntactically correct.
     * 
     * @param expression the expression to validate
     * @throws ExpressionException if the expression is not valid
     */
    @Override
    public void assertExpression(String expression) throws ExpressionException {
        // Simple implementation that accepts all expressions
        if (expression == null) {
            throw new ExpressionException("Expression cannot be null");
        }
    }
    
    /**
     * Enables or disables caching.
     * 
     * @param cache true to enable caching, false to disable
     */
    @Override
    public void setCache(boolean cache) {
        // No caching in this example
    }
    
    /**
     * Clears the cache.
     */
    @Override
    public void clearCache() {
        // No caching in this example
    }
}