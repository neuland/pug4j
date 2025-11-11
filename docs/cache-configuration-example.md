# Cache Configuration Example

This document demonstrates how to configure cache sizes in Pug4j.

## Using PugEngine (Recommended - New API since 3.0.0)

### Basic Cache Configuration

```java
import de.neuland.pug4j.PugEngine;
import de.neuland.pug4j.RenderContext;
import de.neuland.pug4j.template.FileTemplateLoader;

PugEngine engine = PugEngine.builder()
    .templateLoader(new FileTemplateLoader("templates/"))
    .maxCacheSize(5000)              // 5,000 template entries
    .expressionCacheSize(10000)      // 10,000 expression entries
    .caching(true)                   // Enable caching (default: true)
    .build();

// Load and render a template
PugTemplate template = engine.getTemplate("index.pug");
String html = engine.render(template, model, RenderContext.defaults());
```

### Disabling Caches

```java
// Disable all caching
PugEngine engine = PugEngine.builder()
    .caching(false)
    .build();

// Or disable only expression caching
PugEngine engine = PugEngine.builder()
    .expressionCacheSize(0)  // 0 disables expression cache
    .build();
```

### High-Traffic Application Example

```java
import de.neuland.pug4j.PugEngine;
import de.neuland.pug4j.RenderContext;
import de.neuland.pug4j.template.FileTemplateLoader;

public class HighTrafficWebApp {
    private final PugEngine engine;

    public HighTrafficWebApp() {
        this.engine = PugEngine.builder()
            .templateLoader(new FileTemplateLoader("templates/"))
            .maxCacheSize(10000)           // Large template cache
            .expressionCacheSize(20000)     // Large expression cache
            .caching(true)
            .build();
    }

    public String renderPage(String templateName, Map<String, Object> model) {
        try {
            PugTemplate template = engine.getTemplate(templateName);
            return engine.render(template, model);
        } catch (Exception e) {
            throw new RuntimeException("Failed to render template", e);
        }
    }
}
```

### Clearing Caches

```java
PugEngine engine = PugEngine.builder()
    .maxCacheSize(5000)
    .expressionCacheSize(10000)
    .build();

// Clear all caches (cache sizes remain configured)
engine.clearCache();
```

## Using PugConfiguration (Deprecated - Legacy API)

> **Note**: `PugConfiguration` is deprecated since 3.0.0 and will be removed in 4.0.0.
> Use `PugEngine` instead (see above).

### Template Cache Configuration

```java
PugConfiguration config = new PugConfiguration();

// Get the default template cache size (1000 entries)
long defaultSize = config.getMaxCacheSize();
System.out.println("Default template cache size: " + defaultSize);

// Set a custom template cache size
config.setMaxCacheSize(5000);
System.out.println("New template cache size: " + config.getMaxCacheSize());

// Clear the template cache (size remains unchanged)
config.clearCache();
```

### Expression Handler Cache Configuration

```java
PugConfiguration config = new PugConfiguration();

// Get the default expression cache size (5000 for JexlExpressionHandler)
int defaultExpressionCacheSize = config.getExpressionCacheSize();
System.out.println("Default expression cache size: " + defaultExpressionCacheSize);

// Set a custom expression cache size
config.setExpressionCacheSize(10000);
System.out.println("New expression cache size: " + config.getExpressionCacheSize());

// Disable expression caching
config.setExpressionCacheSize(0);

// Re-enable with a specific size
config.setExpressionCacheSize(3000);
```

## Important Notes

1. **Template Cache Size**: Must be a positive value (> 0). The cache is configured at engine build time in `PugEngine`.

2. **Expression Cache Size**: Can be 0 (to disable) or any positive value. This only works with `JexlExpressionHandler`. If you use a different expression handler (like `GraalJsExpressionHandler`), the setting is ignored.

3. **Cache Clearing**: The `clearCache()` method clears both template and expression caches but does NOT change the cache size settings.

4. **Immutability**: `PugEngine` instances are immutable. Cache sizes are set at build time and cannot be changed afterwards. To use different cache sizes, create a new `PugEngine` instance.

## Migration Guide

### From Previous Versions (Hardcoded Caches)

In previous versions, cache sizes were hardcoded:
- Template cache: 1000 entries (constant `MAX_ENTRIES`)
- Expression cache: 5000 entries (constant in `JexlExpressionHandler`)

Now you can configure these values:

```java
// Old: Cache size was fixed (no configuration available)
PugConfiguration config = new PugConfiguration();
// ...cache sizes were hardcoded...

// New (3.0+): Use PugEngine with configurable cache sizes
PugEngine engine = PugEngine.builder()
    .maxCacheSize(customTemplateSize)
    .expressionCacheSize(customExpressionSize)
    .build();
```

### From PugConfiguration to PugEngine

```java
// Old API (deprecated)
PugConfiguration config = new PugConfiguration();
config.setTemplateLoader(new FileTemplateLoader("templates/"));
config.setMaxCacheSize(5000);
config.setExpressionCacheSize(10000);
PugTemplate template = config.getTemplate("index.pug");
String html = config.renderTemplate(template, model);

// New API (recommended)
PugEngine engine = PugEngine.builder()
    .templateLoader(new FileTemplateLoader("templates/"))
    .maxCacheSize(5000)
    .expressionCacheSize(10000)
    .build();

PugTemplate template = engine.getTemplate("index.pug");
String html = engine.render(template, model);
```

## Default Values

| Cache Type | Default Size | Can be 0? |
|-----------|--------------|-----------|
| Template Cache | 1000 | No (must be > 0) |
| Expression Cache | 5000 | Yes |
