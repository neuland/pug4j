package de.neuland.pug4j;

import static org.junit.Assert.*;

import de.neuland.pug4j.expression.JexlExpressionHandler;
import de.neuland.pug4j.template.FileTemplateLoader;
import org.junit.Test;

public class PugEngineCacheTest {

  @Test
  public void testDefaultCacheSize() {
    PugEngine engine = PugEngine.builder().build();

    // Engine should be created with caching enabled by default
    assertTrue(engine.isCaching());
  }

  @Test
  public void testCustomMaxCacheSize() {
    PugEngine engine = PugEngine.builder().maxCacheSize(5000).build();

    assertTrue(engine.isCaching());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testMaxCacheSizeZero() {
    PugEngine.builder().maxCacheSize(0).build();
  }

  @Test(expected = IllegalArgumentException.class)
  public void testMaxCacheSizeNegative() {
    PugEngine.builder().maxCacheSize(-1).build();
  }

  @Test
  public void testCustomExpressionCacheSizeOnHandler() {
    PugEngine engine =
        PugEngine.builder().expressionHandler(new JexlExpressionHandler(10000)).build();

    JexlExpressionHandler handler = (JexlExpressionHandler) engine.getExpressionHandler();
    assertEquals(10000, handler.getCacheSize());
  }

  @Test
  public void testExpressionCacheDisabledOnHandler() {
    PugEngine engine =
        PugEngine.builder().expressionHandler(new JexlExpressionHandler(0)).build();

    JexlExpressionHandler handler = (JexlExpressionHandler) engine.getExpressionHandler();
    assertEquals(0, handler.getCacheSize());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testExpressionCacheSizeNegative() {
    new JexlExpressionHandler(-1);
  }

  @Test
  public void testEngineDoesNotReconfigureHandlerCache() {
    JexlExpressionHandler handler = new JexlExpressionHandler(123);
    PugEngine.builder().expressionHandler(handler).caching(true).build();
    PugEngine.builder().expressionHandler(handler).caching(false).build();

    // The engine must use the handler as given, regardless of its own caching setting
    assertEquals(123, handler.getCacheSize());
  }

  @Test
  public void testCachingDisabled() {
    PugEngine engine = PugEngine.builder().caching(false).build();

    assertFalse(engine.isCaching());
  }

  @Test
  public void testCombinedCacheConfiguration() {
    PugEngine engine =
        PugEngine.builder()
            .maxCacheSize(3000)
            .expressionHandler(new JexlExpressionHandler(8000))
            .caching(true)
            .build();

    assertTrue(engine.isCaching());

    JexlExpressionHandler handler = (JexlExpressionHandler) engine.getExpressionHandler();
    assertEquals(8000, handler.getCacheSize());
  }

  @Test
  public void testBuilderChaining() {
    PugEngine engine =
        PugEngine.builder()
            .templateLoader(new FileTemplateLoader())
            .expressionHandler(new JexlExpressionHandler(4000))
            .caching(true)
            .maxCacheSize(2000)
            .build();

    assertTrue(engine.isCaching());
    assertNotNull(engine.getTemplateLoader());
    assertNotNull(engine.getExpressionHandler());
  }

  @Test
  public void testClearCacheDoesNotAffectConfiguration() {
    PugEngine engine =
        PugEngine.builder()
            .maxCacheSize(3000)
            .expressionHandler(new JexlExpressionHandler(2000))
            .build();

    // Clear cache
    engine.clearCache();

    // Engine configuration should remain unchanged
    assertTrue(engine.isCaching());
  }
}
