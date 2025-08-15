package de.neuland.pug4j.expression;

/**
 * Service provider interface for expression handlers.
 * 
 * This interface allows for the registration of custom expression handlers
 * through Java's ServiceLoader mechanism. Implementations should be registered
 * in META-INF/services/de.neuland.pug4j.expression.ExpressionHandlerProvider.
 */
public interface ExpressionHandlerProvider {
    
    /**
     * Returns the type identifier for this expression handler.
     * 
     * The type identifier is used to select the handler when creating
     * a new instance through the ExpressionHandlerFactory.
     * 
     * @return a unique type identifier for this handler
     */
    String getType();
    
    /**
     * Creates a new instance of the expression handler.
     * 
     * @return a new expression handler instance
     */
    ExpressionHandler createHandler();
}