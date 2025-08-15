package de.neuland.pug4j.expression;

/**
 * Example provider for the custom expression handler.
 * 
 * This class demonstrates how to implement the ExpressionHandlerProvider interface
 * to register a custom expression handler with the factory.
 */
public class CustomExpressionHandlerProvider implements ExpressionHandlerProvider {
    
    /**
     * The type identifier for this expression handler.
     */
    public static final String TYPE = "custom";
    
    @Override
    public String getType() {
        return TYPE;
    }
    
    @Override
    public ExpressionHandler createHandler() {
        return new CustomExpressionHandlerExample();
    }
}