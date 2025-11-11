package de.neuland.pug4j;

import de.neuland.pug4j.expression.GraalJsExpressionHandler;
import de.neuland.pug4j.expression.JexlExpressionHandler;
import de.neuland.pug4j.template.FileTemplateLoader;
import org.junit.Test;

import static org.junit.Assert.*;

public class PugEngineCacheTest {

    @Test
    public void testDefaultCacheSize() {
        PugEngine engine = PugEngine.builder().build();

        // Engine should be created with caching enabled by default
        assertTrue(engine.isCaching());
    }

    @Test
    public void testCustomMaxCacheSize() {
        PugEngine engine = PugEngine.builder()
                .maxCacheSize(5000)
                .build();

        assertTrue(engine.isCaching());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMaxCacheSizeZero() {
        PugEngine.builder()
                .maxCacheSize(0)
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMaxCacheSizeNegative() {
        PugEngine.builder()
                .maxCacheSize(-1)
                .build();
    }

    @Test
    public void testCustomExpressionCacheSize() {
        PugEngine engine = PugEngine.builder()
                .expressionCacheSize(10000)
                .build();

        assertTrue(engine.isCaching());

        // Verify the expression handler cache size was set
        if (engine.getExpressionHandler() instanceof JexlExpressionHandler) {
            JexlExpressionHandler handler = (JexlExpressionHandler) engine.getExpressionHandler();
            assertEquals(10000, handler.getCacheSize());
        }
    }

    @Test
    public void testExpressionCacheSizeDisabled() {
        PugEngine engine = PugEngine.builder()
                .expressionCacheSize(0)
                .build();

        // Verify the expression cache is disabled
        if (engine.getExpressionHandler() instanceof JexlExpressionHandler) {
            JexlExpressionHandler handler = (JexlExpressionHandler) engine.getExpressionHandler();
            assertEquals(0, handler.getCacheSize());
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testExpressionCacheSizeNegative() {
        PugEngine.builder()
                .expressionCacheSize(-1)
                .build();
    }

    @Test
    public void testCachingDisabled() {
        PugEngine engine = PugEngine.builder()
                .caching(false)
                .build();

        assertFalse(engine.isCaching());
    }

    @Test
    public void testCombinedCacheConfiguration() {
        PugEngine engine = PugEngine.builder()
                .maxCacheSize(3000)
                .expressionCacheSize(8000)
                .caching(true)
                .build();

        assertTrue(engine.isCaching());

        if (engine.getExpressionHandler() instanceof JexlExpressionHandler) {
            JexlExpressionHandler handler = (JexlExpressionHandler) engine.getExpressionHandler();
            assertEquals(8000, handler.getCacheSize());
        }
    }

    @Test
    public void testExpressionCacheSizeWithNonJexlHandler() {
        // This should not throw an exception; the expressionCacheSize setting
        // is simply ignored for non-JexlExpressionHandler implementations
        PugEngine engine = PugEngine.builder()
                .expressionHandler(new GraalJsExpressionHandler())
                .expressionCacheSize(1000)
                .build();

        assertNotNull(engine);
    }

    @Test
    public void testBuilderChaining() {
        PugEngine engine = PugEngine.builder()
                .templateLoader(new FileTemplateLoader())
                .expressionHandler(new JexlExpressionHandler())
                .caching(true)
                .maxCacheSize(2000)
                .expressionCacheSize(4000)
                .build();

        assertTrue(engine.isCaching());
        assertNotNull(engine.getTemplateLoader());
        assertNotNull(engine.getExpressionHandler());
    }

    @Test
    public void testClearCacheDoesNotAffectConfiguration() {
        PugEngine engine = PugEngine.builder()
                .maxCacheSize(3000)
                .expressionCacheSize(2000)
                .build();

        // Clear cache
        engine.clearCache();

        // Engine configuration should remain unchanged
        assertTrue(engine.isCaching());
    }
}
