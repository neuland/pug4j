package de.neuland.pug4j.expression;

/**
 * Provider for JEXL expression handlers.
 */
public class JexlExpressionHandlerProvider implements ExpressionHandlerProvider {
    
    @Override
    public String getType() {
        return ExpressionHandlerFactory.TYPE_JEXL;
    }
    
    @Override
    public ExpressionHandler createHandler() {
        return new JexlExpressionHandler();
    }
}