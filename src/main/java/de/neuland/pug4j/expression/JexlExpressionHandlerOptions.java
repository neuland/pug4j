package de.neuland.pug4j.expression;

public class JexlExpressionHandlerOptions {
  private int cache = 5000;
  private int cacheThreshold = 64;
  private boolean debug = false;

  public int getCache() {
    return cache;
  }

  public void setCache(final int cache) {
    this.cache = cache;
  }

  public int getCacheThreshold() {
    return cacheThreshold;
  }

  public void setCacheThreshold(final int cacheThreshold) {
    this.cacheThreshold = cacheThreshold;
  }

  public boolean isDebug() {
    return debug;
  }

  public void setDebug(final boolean debug) {
    this.debug = debug;
  }
}
