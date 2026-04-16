package de.neuland.pug4j;

import de.neuland.pug4j.Pug4J.Mode;
import de.neuland.pug4j.exceptions.PugCompilerException;
import de.neuland.pug4j.exceptions.PugException;
import de.neuland.pug4j.expression.ExpressionHandler;
import de.neuland.pug4j.expression.JexlExpressionHandler;
import de.neuland.pug4j.filter.CDATAFilter;
import de.neuland.pug4j.filter.CssFilter;
import de.neuland.pug4j.filter.Filter;
import de.neuland.pug4j.filter.JsFilter;
import de.neuland.pug4j.template.FileTemplateLoader;
import de.neuland.pug4j.template.PugTemplate;
import de.neuland.pug4j.template.TemplateLoader;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

/**
 * @deprecated As of 3.0.0, replaced by {@link PugEngine} and {@link RenderContext}.
 *     <p>This class mixed three responsibilities: template loading, caching, and rendering
 *     configuration. The new API separates these concerns:
 *     <ul>
 *       <li>{@link PugEngine} - Template factory and cache manager
 *       <li>{@link RenderContext} - Render-time options (prettyPrint, defaultMode, globalVariables)
 *     </ul>
 *     <p><strong>Migration example:</strong>
 *     <pre>{@code
 * // Old API (2.x)
 * PugConfiguration config = new PugConfiguration();
 * config.setTemplateLoader(loader);
 * config.setPrettyPrint(true);
 * config.setMode(Mode.HTML);
 * PugTemplate template = config.getTemplate("index");
 * String html = config.renderTemplate(template, model);
 *
 * // New API (3.0+)
 * PugEngine engine = PugEngine.builder()
 *     .templateLoader(loader)
 *     .build();
 *
 * RenderContext context = RenderContext.builder()
 *     .prettyPrint(true)
 *     .defaultMode(Mode.HTML)
 *     .build();
 *
 * PugTemplate template = engine.getTemplate("index");
 * String html = engine.render(template, model, context);
 * }</pre>
 *     <p>This class will be removed in version 4.0.0.
 * @see PugEngine
 * @see RenderContext
 */
@Deprecated(since = "3.0.0", forRemoval = true)
public class PugConfiguration {

  private static final String FILTER_CDATA = "cdata";
  private static final String FILTER_STYLE = "css";
  private static final String FILTER_SCRIPT = "js";

  private boolean prettyPrint = false;
  private boolean caching = true;
  private Mode mode = Pug4J.Mode.HTML;

  private Map<String, Filter> filters = new HashMap<>();
  private Map<String, Object> sharedVariables = new HashMap<>();
  private TemplateLoader templateLoader = new FileTemplateLoader();
  private ExpressionHandler expressionHandler = new JexlExpressionHandler();
  private PugEngine engine = null; // Cached engine instance
  protected static final long MAX_ENTRIES = 1000L;
  private long maxCacheSize = MAX_ENTRIES;
  private int expressionCacheSize = 5000;

  public PugConfiguration() {
    setFilter(FILTER_CDATA, new CDATAFilter());
    setFilter(FILTER_SCRIPT, new JsFilter());
    setFilter(FILTER_STYLE, new CssFilter());
  }

  /**
   * Gets or creates the PugEngine instance. This method lazily initializes the engine
   * on first use and returns the cached instance on subsequent calls.
   *
   * @return the PugEngine instance
   */
  public PugEngine getOrCreateEngine() {
    if (engine == null) {
      engine = PugEngine.builder()
          .templateLoader(templateLoader)
          .expressionHandler(expressionHandler)
          .caching(caching)
          .maxCacheSize(maxCacheSize)
          .expressionCacheSize(expressionCacheSize)
          .filters(filters)
          .build();
    }
    return engine;
  }

  public PugTemplate getTemplate(String name) throws IOException, PugException {
    return getOrCreateEngine().getTemplate(name);
  }

  public void renderTemplate(PugTemplate template, Map<String, Object> model, Writer writer)
      throws PugCompilerException {
    // Convert deprecated PugConfiguration to new API
    PugEngine engine = getOrCreateEngine();

    RenderContext context =
        RenderContext.builder()
            .prettyPrint(prettyPrint)
            .defaultMode(mode)
            .globalVariables(sharedVariables)
            .build();

    engine.render(template, model, context, writer);
  }

  public String renderTemplate(PugTemplate template, Map<String, Object> model)
      throws PugCompilerException {
    StringWriter writer = new StringWriter();
    renderTemplate(template, model, writer);
    return writer.toString();
  }


  public boolean isPrettyPrint() {
    return prettyPrint;
  }

  public void setPrettyPrint(boolean prettyPrint) {
    this.prettyPrint = prettyPrint;
  }

  public void setFilter(String name, Filter filter) {
    filters.put(name, filter);
    this.engine = null; // Invalidate cached engine
  }

  public void removeFilter(String name) {
    filters.remove(name);
    this.engine = null; // Invalidate cached engine
  }

  public Map<String, Filter> getFilters() {
    return filters;
  }

  public void setFilters(Map<String, Filter> filters) {
    this.filters = filters;
    this.engine = null; // Invalidate cached engine
  }

  public Map<String, Object> getSharedVariables() {
    return sharedVariables;
  }

  public void setSharedVariables(Map<String, Object> sharedVariables) {
    this.sharedVariables = sharedVariables;
  }

  public TemplateLoader getTemplateLoader() {
    return templateLoader;
  }

  public void setTemplateLoader(TemplateLoader templateLoader) {
    this.templateLoader = templateLoader;
    this.engine = null; // Invalidate cached engine
  }

  public void setExpressionHandler(ExpressionHandler expressionHandler) {
    this.expressionHandler = expressionHandler;
    this.engine = null; // Invalidate cached engine
  }

  public ExpressionHandler getExpressionHandler() {
    return expressionHandler;
  }

  public Mode getMode() {
    return mode;
  }

  public void setMode(Mode mode) {
    this.mode = mode;
  }

  public boolean templateExists(String url) {
    try (var reader = templateLoader.getReader(url)) {
      return reader != null;
    } catch (IOException e) {
      return false;
    }
  }

  public boolean isCaching() {
    return caching;
  }

  public void setCaching(boolean cache) {
    if (cache != this.caching) {
      expressionHandler.setCache(cache);
      this.caching = cache;
      this.engine = null; // Invalidate cached engine
    }
  }

  public void clearCache() {
    if (engine != null) {
      engine.clearCache();
    } else {
      expressionHandler.clearCache();
    }
  }

  /**
   * Gets the maximum number of entries in the template cache.
   *
   * @return the maximum cache size
   */
  public long getMaxCacheSize() {
    return maxCacheSize;
  }

  /**
   * Sets the maximum number of entries in the template cache. The cache will be rebuilt with the
   * new size, and existing entries will be cleared.
   *
   * @param maxCacheSize the maximum cache size (must be positive)
   * @throws IllegalArgumentException if maxCacheSize is not positive
   */
  public void setMaxCacheSize(long maxCacheSize) {
    if (maxCacheSize <= 0) {
      throw new IllegalArgumentException("maxCacheSize must be positive");
    }
    if (this.maxCacheSize != maxCacheSize) {
      this.maxCacheSize = maxCacheSize;
      this.engine = null; // Invalidate cached engine
    }
  }

  /**
   * Sets the expression handler cache size.
   *
   * @param cacheSize the cache size (0 to disable, positive value to enable with specific size)
   */
  public void setExpressionCacheSize(int cacheSize) {
    this.expressionCacheSize = cacheSize;
    this.engine = null; // Invalidate cached engine
  }

  /**
   * Gets the expression handler cache size.
   *
   * @return the cache size
   */
  public int getExpressionCacheSize() {
    return expressionCacheSize;
  }
}
