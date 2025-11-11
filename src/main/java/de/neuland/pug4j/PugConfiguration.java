package de.neuland.pug4j;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import de.neuland.pug4j.Pug4J.Mode;
import de.neuland.pug4j.exceptions.PugCompilerException;
import de.neuland.pug4j.exceptions.PugException;
import de.neuland.pug4j.expression.ExpressionHandler;
import de.neuland.pug4j.expression.JexlExpressionHandler;
import de.neuland.pug4j.filter.CDATAFilter;
import de.neuland.pug4j.filter.CssFilter;
import de.neuland.pug4j.filter.Filter;
import de.neuland.pug4j.filter.JsFilter;
import de.neuland.pug4j.model.PugModel;
import de.neuland.pug4j.parser.Parser;
import de.neuland.pug4j.parser.node.Node;
import de.neuland.pug4j.template.FileTemplateLoader;
import de.neuland.pug4j.template.PugTemplate;
import de.neuland.pug4j.template.TemplateLoader;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

/**
 * @deprecated As of 3.0.0, replaced by {@link PugEngine} and {@link RenderContext}.
 * <p>
 * This class mixed three responsibilities: template loading, caching, and rendering configuration.
 * The new API separates these concerns:
 * </p>
 * <ul>
 *   <li>{@link PugEngine} - Template factory and cache manager</li>
 *   <li>{@link RenderContext} - Render-time options (prettyPrint, defaultMode, globalVariables)</li>
 * </ul>
 *
 * <p><strong>Migration example:</strong></p>
 * <pre>{@code
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
 *
 * <p>This class will be removed in version 4.0.0.</p>
 *
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
    protected static final long MAX_ENTRIES = 1000L;
    private long maxCacheSize = MAX_ENTRIES;
    private Cache<String, PugTemplate> cache = Caffeine.newBuilder().maximumSize(maxCacheSize).build();

    public PugConfiguration() {
        setFilter(FILTER_CDATA, new CDATAFilter());
        setFilter(FILTER_SCRIPT, new JsFilter());
        setFilter(FILTER_STYLE, new CssFilter());
    }

    public PugTemplate getTemplate(String name) throws IOException, PugException {

        if (caching) {

            long lastModified = templateLoader.getLastModified(name);
            return cache.get(getKeyValue(name, lastModified), value -> {

                try {
                    return createTemplate(name);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }

            });

        }

        return createTemplate(name);
    }

    private String getKeyValue(String name, long lastModified) {
        return name + "-" + lastModified;
    }

    public void renderTemplate(PugTemplate template, Map<String, Object> model, Writer writer) throws PugCompilerException {
        PugModel pugModel = new PugModel(sharedVariables);
        for (String filterName : filters.keySet()) {
            pugModel.addFilter(filterName, filters.get(filterName));
        }
        pugModel.putAll(model);
        template.process(pugModel, writer, this);
    }

    public String renderTemplate(PugTemplate template, Map<String, Object> model) throws PugCompilerException {
        StringWriter writer = new StringWriter();
        renderTemplate(template, model, writer);
        return writer.toString();
    }

    private PugTemplate createTemplate(String name) throws PugException, IOException {

        Parser parser = new Parser(name, templateLoader, expressionHandler);
        Node root = parser.parse();
        PugTemplate template = new PugTemplate(root, getMode());
        return template;
    }

    public boolean isPrettyPrint() {
        return prettyPrint;
    }

    public void setPrettyPrint(boolean prettyPrint) {
        this.prettyPrint = prettyPrint;
    }

    public void setFilter(String name, Filter filter) {
        filters.put(name, filter);
    }

    public void removeFilter(String name) {
        filters.remove(name);
    }

    public Map<String, Filter> getFilters() {
        return filters;
    }

    public void setFilters(Map<String, Filter> filters) {
        this.filters = filters;
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
    }

    public void setExpressionHandler(ExpressionHandler expressionHandler) {
        this.expressionHandler = expressionHandler;
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
        try {
            return templateLoader.getReader(url) != null;
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
        }
    }

    public void clearCache() {
        expressionHandler.clearCache();
        cache.invalidateAll();
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
     * Sets the maximum number of entries in the template cache.
     * The cache will be rebuilt with the new size, and existing entries will be cleared.
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
            rebuildCache();
        }
    }

    /**
     * Rebuilds the template cache with the current maxCacheSize.
     * This will clear all existing cached templates.
     */
    private void rebuildCache() {
        cache = Caffeine.newBuilder().maximumSize(maxCacheSize).build();
    }

    /**
     * Sets the expression handler cache size.
     * This method only works if the expression handler is a {@link JexlExpressionHandler}.
     * If a different expression handler is used, this method will throw an {@link IllegalStateException}.
     *
     * @param cacheSize the cache size (0 to disable, positive value to enable with specific size)
     * @throws IllegalStateException if the expression handler is not a JexlExpressionHandler
     * @throws IllegalArgumentException if cacheSize is negative
     */
    public void setExpressionCacheSize(int cacheSize) {
        if (expressionHandler instanceof JexlExpressionHandler) {
            ((JexlExpressionHandler) expressionHandler).setCacheSize(cacheSize);
        } else {
            throw new IllegalStateException(
                    "setExpressionCacheSize() only works with JexlExpressionHandler, current handler is: "
                            + expressionHandler.getClass().getName());
        }
    }

    /**
     * Gets the expression handler cache size.
     * This method only works if the expression handler is a {@link JexlExpressionHandler}.
     * If a different expression handler is used, this method will throw an {@link IllegalStateException}.
     *
     * @return the cache size
     * @throws IllegalStateException if the expression handler is not a JexlExpressionHandler
     */
    public int getExpressionCacheSize() {
        if (expressionHandler instanceof JexlExpressionHandler) {
            return ((JexlExpressionHandler) expressionHandler).getCacheSize();
        } else {
            throw new IllegalStateException(
                    "getExpressionCacheSize() only works with JexlExpressionHandler, current handler is: "
                            + expressionHandler.getClass().getName());
        }
    }
}
