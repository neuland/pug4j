# Cache Configuration Feature

## Summary

Cache configuration is now available in Pug4j, allowing you to customize both template cache size and expression handler cache size.

## Changes Made

### 1. PugEngine (New API - Recommended)

**File**: `src/main/java/de/neuland/pug4j/PugEngine.java`

Added builder methods for cache configuration:
- `maxCacheSize(long)` - Configure template cache size (default: 1000, must be > 0)
- `expressionCacheSize(int)` - Configure expression cache size (default: 5000, can be 0 or positive)

**Example**:
```java
PugEngine engine = PugEngine.builder()
    .templateLoader(new FileTemplateLoader("templates/"))
    .maxCacheSize(10000)           // 10,000 template entries
    .expressionCacheSize(20000)    // 20,000 expression entries
    .build();
```

### 2. JexlExpressionHandler

**File**: `src/main/java/de/neuland/pug4j/expression/JexlExpressionHandler.java`

Added methods:
- `setCacheSize(int)` - Set expression cache to specific size
- `getCacheSize()` - Get current expression cache size

### 3. PugConfiguration (Deprecated API - Backward Compatibility)

**File**: `src/main/java/de/neuland/pug4j/PugConfiguration.java`

Added methods for backward compatibility:
- `setMaxCacheSize(long)` / `getMaxCacheSize()` - Template cache configuration
- `setExpressionCacheSize(int)` / `getExpressionCacheSize()` - Expression cache configuration

**Important**: Restored the deprecation JavaDoc that was accidentally removed.

## Tests

### PugEngineCacheTest.java
Tests for the new PugEngine cache configuration:
- Default cache sizes
- Custom cache sizes
- Validation (negative values, zero for template cache)
- Expression cache with different handlers
- Builder chaining
- Cache clearing

### PugConfigurationCacheTest.java
Tests for the deprecated PugConfiguration cache configuration:
- All the same scenarios as PugEngine tests
- Backward compatibility validation

## Default Values

| Setting | Default | Range |
|---------|---------|-------|
| Template Cache Size | 1000 | Must be > 0 |
| Expression Cache Size | 5000 | 0 or positive |

## Key Design Decisions

1. **PugEngine is immutable**: Cache sizes are set at build time and cannot be changed. This is consistent with the builder pattern and thread-safety.

2. **Expression cache size only applies to JexlExpressionHandler**: Other expression handlers ignore this setting without error.

3. **Backward compatibility**: The deprecated `PugConfiguration` also supports cache configuration for users still on the old API.

4. **Validation**:
   - Template cache must be positive (> 0)
   - Expression cache can be 0 (disabled) or positive

## Migration Path

Users can choose:
1. **New API (recommended)**: Use `PugEngine.builder()` with cache configuration
2. **Legacy API**: Continue using `PugConfiguration` with cache configuration (will work until 4.0.0)

## Documentation

- `cache-configuration-example.md` - Comprehensive examples and migration guide
- Updated JavaDoc in all modified classes
