package de.neuland.pug4j;

import static org.junit.Assert.*;

import de.neuland.pug4j.expression.JexlExpressionHandler;
import org.junit.Test;

public class PugConfigurationCacheTest {

  @Test
  public void testSetMaxCacheSize() {
    PugConfiguration config = new PugConfiguration();

    // Test default value
    assertEquals(1000L, config.getMaxCacheSize());

    // Test setting a new value
    config.setMaxCacheSize(5000L);
    assertEquals(5000L, config.getMaxCacheSize());

    // Test setting another value
    config.setMaxCacheSize(100L);
    assertEquals(100L, config.getMaxCacheSize());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testSetMaxCacheSizeZero() {
    PugConfiguration config = new PugConfiguration();
    config.setMaxCacheSize(0L);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testSetMaxCacheSizeNegative() {
    PugConfiguration config = new PugConfiguration();
    config.setMaxCacheSize(-1L);
  }

  @Test
  public void testSetExpressionCacheSize() {
    PugConfiguration config = new PugConfiguration();

    // Test default value (5000 for JexlExpressionHandler)
    assertEquals(5000, config.getExpressionCacheSize());

    // Test setting a new value
    config.setExpressionCacheSize(1000);
    assertEquals(1000, config.getExpressionCacheSize());

    // Test disabling cache
    config.setExpressionCacheSize(0);
    assertEquals(0, config.getExpressionCacheSize());

    // Test re-enabling with custom size
    config.setExpressionCacheSize(2000);
    assertEquals(2000, config.getExpressionCacheSize());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testSetExpressionCacheSizeNegative() {
    PugConfiguration config = new PugConfiguration();
    config.setExpressionCacheSize(-1);
  }

  @Test
  public void testExpressionCacheSizeWithJexlHandler() {
    PugConfiguration config = new PugConfiguration();
    config.setExpressionHandler(new JexlExpressionHandler());

    // Should work fine with JexlExpressionHandler
    config.setExpressionCacheSize(3000);
    assertEquals(3000, config.getExpressionCacheSize());
  }

  @Test(expected = IllegalStateException.class)
  public void testSetExpressionCacheSizeWithNonJexlHandler() {
    PugConfiguration config = new PugConfiguration();

    // Set a custom expression handler that is not JexlExpressionHandler
    config.setExpressionHandler(new de.neuland.pug4j.expression.GraalJsExpressionHandler());

    // This should throw IllegalStateException
    config.setExpressionCacheSize(1000);
  }

  @Test(expected = IllegalStateException.class)
  public void testGetExpressionCacheSizeWithNonJexlHandler() {
    PugConfiguration config = new PugConfiguration();

    // Set a custom expression handler that is not JexlExpressionHandler
    config.setExpressionHandler(new de.neuland.pug4j.expression.GraalJsExpressionHandler());

    // This should throw IllegalStateException
    config.getExpressionCacheSize();
  }

  @Test
  public void testClearCacheDoesNotResetSize() {
    PugConfiguration config = new PugConfiguration();

    // Set custom cache sizes
    config.setMaxCacheSize(3000L);
    config.setExpressionCacheSize(2000);

    // Clear caches
    config.clearCache();

    // Sizes should remain the same
    assertEquals(3000L, config.getMaxCacheSize());
    assertEquals(2000, config.getExpressionCacheSize());
  }
}
