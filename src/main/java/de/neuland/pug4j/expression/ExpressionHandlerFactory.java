package de.neuland.pug4j.expression;

import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.function.Supplier;

/**
 * Factory for creating and managing expression handlers.
 * 
 * This factory provides a centralized way to create expression handlers and
 * allows for registration of custom handler implementations through the
 * {@link ExpressionHandlerProvider} interface.
 * 
 * <p>Built-in handlers:</p>
 * <ul>
 *   <li>JEXL - Apache JEXL expression language</li>
 *   <li>GraalJS - JavaScript expressions using GraalVM</li>
 * </ul>
 * 
 * <p>Custom handlers can be registered in two ways:</p>
 * <ol>
 *   <li>Programmatically using {@link #registerHandler(String, Supplier)}</li>
 *   <li>Using Java's ServiceLoader mechanism by implementing {@link ExpressionHandlerProvider}
 *       and registering it in META-INF/services/de.neuland.pug4j.expression.ExpressionHandlerProvider</li>
 * </ol>
 */
public class ExpressionHandlerFactory {
    
    private static final Map<String, Supplier<ExpressionHandler>> handlerSuppliers = new HashMap<>();
    
    /**
     * Type identifier for the JEXL expression handler.
     */
    public static final String TYPE_JEXL = "jexl";
    
    /**
     * Type identifier for the GraalJS expression handler.
     */
    public static final String TYPE_GRAALJS = "graaljs";
    
    static {
        // Register built-in handlers using their providers
        registerHandler(new JexlExpressionHandlerProvider());
        registerHandler(new GraalJsExpressionHandlerProvider());
        
        // Load custom handlers via ServiceLoader
        ServiceLoader<ExpressionHandlerProvider> serviceLoader = ServiceLoader.load(ExpressionHandlerProvider.class);
        for (ExpressionHandlerProvider provider : serviceLoader) {
            registerHandler(provider);
        }
    }
    
    /**
     * Creates a new expression handler of the specified type.
     * 
     * @param type the type of expression handler to create
     * @return a new expression handler instance
     * @throws IllegalArgumentException if the specified type is not registered
     */
    public static ExpressionHandler createHandler(String type) {
        Supplier<ExpressionHandler> supplier = handlerSuppliers.get(type);
        if (supplier == null) {
            throw new IllegalArgumentException("Unknown expression handler type: " + type);
        }
        return supplier.get();
    }
    
    /**
     * Creates a new JEXL expression handler.
     * 
     * @return a new JEXL expression handler
     */
    public static ExpressionHandler createJexlHandler() {
        return createHandler(TYPE_JEXL);
    }
    
    /**
     * Creates a new GraalJS expression handler.
     * 
     * @return a new GraalJS expression handler
     */
    public static ExpressionHandler createGraalJsHandler() {
        return createHandler(TYPE_GRAALJS);
    }
    
    /**
     * Registers a new expression handler provider.
     * 
     * @param provider the provider to register
     */
    public static void registerHandler(ExpressionHandlerProvider provider) {
        registerHandler(provider.getType(), provider::createHandler);
    }
    
    /**
     * Registers a new expression handler type with a supplier.
     * 
     * @param type the type identifier for the handler
     * @param supplier a supplier that creates new instances of the handler
     */
    public static void registerHandler(String type, Supplier<ExpressionHandler> supplier) {
        handlerSuppliers.put(type, supplier);
    }
    
    /**
     * Returns the default expression handler (JEXL).
     * 
     * @return a new instance of the default expression handler
     */
    public static ExpressionHandler getDefaultHandler() {
        return createJexlHandler();
    }
}