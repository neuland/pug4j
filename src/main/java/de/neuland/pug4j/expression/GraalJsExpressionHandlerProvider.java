package de.neuland.pug4j.expression;

/**
 * Provider for GraalJS expression handlers.
 */
public class GraalJsExpressionHandlerProvider implements ExpressionHandlerProvider {
    
    @Override
    public String getType() {
        return ExpressionHandlerFactory.TYPE_GRAALJS;
    }
    
    @Override
    public ExpressionHandler createHandler() {
        return new GraalJsExpressionHandler();
    }
}