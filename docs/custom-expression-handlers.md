# Creating Custom Expression Handlers for Pug4j

This guide explains how to create and register custom expression handlers for Pug4j.

## Overview

Pug4j supports multiple expression handlers for evaluating expressions in templates. By default, it uses the JEXL expression handler, but it also includes a GraalJS handler for JavaScript expressions. You can also create your own custom expression handlers to support different expression languages or add specialized functionality.

## Creating a Custom Expression Handler

To create a custom expression handler, you need to:

1. Implement the `ExpressionHandler` interface or extend the `AbstractExpressionHandler` class
2. Create a provider for your handler by implementing the `ExpressionHandlerProvider` interface
3. Register your provider with the `ExpressionHandlerFactory`

### Step 1: Implement the ExpressionHandler Interface

The `ExpressionHandler` interface defines the methods that your handler must implement:

```java
public interface ExpressionHandler {
    Boolean evaluateBooleanExpression(String expression, PugModel model) throws ExpressionException;
    Object evaluateExpression(String expression, PugModel model) throws ExpressionException;
    String evaluateStringExpression(String expression, PugModel model) throws ExpressionException;
    void assertExpression(String expression) throws ExpressionException;
    void setCache(boolean cache);
    void clearCache();
}
```

For convenience, you can extend the `AbstractExpressionHandler` class, which provides some common functionality:

```java
public class MyExpressionHandler extends AbstractExpressionHandler {
    @Override
    public Boolean evaluateBooleanExpression(String expression, PugModel model) throws ExpressionException {
        // Implement boolean expression evaluation
    }

    @Override
    public Object evaluateExpression(String expression, PugModel model) throws ExpressionException {
        // Save local variable names for variable declarations
        saveLocalVariableName(expression, model);
        
        // Implement expression evaluation
    }

    @Override
    public String evaluateStringExpression(String expression, PugModel model) throws ExpressionException {
        // Implement string expression evaluation
    }

    @Override
    public void assertExpression(String expression) throws ExpressionException {
        // Implement expression validation
    }
}
```

### Step 2: Create a Provider for Your Handler

Create a provider class that implements the `ExpressionHandlerProvider` interface:

```java
public class MyExpressionHandlerProvider implements ExpressionHandlerProvider {
    public static final String TYPE = "myhandler";
    
    @Override
    public String getType() {
        return TYPE;
    }
    
    @Override
    public ExpressionHandler createHandler() {
        return new MyExpressionHandler();
    }
}
```

### Step 3: Register Your Provider

There are two ways to register your provider:

#### Option 1: Programmatic Registration

You can register your provider programmatically using the `ExpressionHandlerFactory`:

```java
// Register the provider
ExpressionHandlerFactory.registerHandler(new MyExpressionHandlerProvider());

// Or register with a supplier
ExpressionHandlerFactory.registerHandler("myhandler", () -> new MyExpressionHandler());

// Use your handler
PugConfiguration config = new PugConfiguration();
config.setExpressionHandler(ExpressionHandlerFactory.createHandler("myhandler"));
```

#### Option 2: ServiceLoader Registration

For automatic discovery, you can use Java's ServiceLoader mechanism:

1. Create a file at `META-INF/services/de.neuland.pug4j.expression.ExpressionHandlerProvider`
2. Add the fully qualified name of your provider class to this file:

```
de.neuland.pug4j.expression.MyExpressionHandlerProvider
```

With this setup, your provider will be automatically discovered and registered when the `ExpressionHandlerFactory` is initialized.

## Example Implementation

See the following example classes in the Pug4j codebase:

- `CustomExpressionHandlerExample`: A simple example expression handler
- `CustomExpressionHandlerProvider`: The provider for the example handler

## Best Practices

1. Make your expression handler thread-safe
2. Implement caching for better performance
3. Provide clear error messages in exceptions
4. Document the syntax and features of your expression language
5. Include examples of how to use your handler

## Testing Your Handler

Create unit tests for your expression handler to ensure it works correctly:

```java
@Test
public void testBooleanExpression() {
    MyExpressionHandler handler = new MyExpressionHandler();
    PugModel model = new PugModel();
    
    Boolean result = handler.evaluateBooleanExpression("true", model);
    assertTrue(result);
    
    result = handler.evaluateBooleanExpression("false", model);
    assertFalse(result);
}
```

## Conclusion

Custom expression handlers allow you to extend Pug4j with support for different expression languages or specialized functionality. By following this guide, you can create and register your own expression handlers to meet your specific needs.