package de.neuland.pug4j;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import de.neuland.pug4j.exceptions.PugCompilerException;
import de.neuland.pug4j.exceptions.PugException;
import de.neuland.pug4j.expression.ExpressionHandler;
import de.neuland.pug4j.expression.JexlExpressionHandler;
import de.neuland.pug4j.filter.CDATAFilter;
import de.neuland.pug4j.filter.CssFilter;
import de.neuland.pug4j.filter.Filter;
import de.neuland.pug4j.filter.JsFilter;
import de.neuland.pug4j.parser.Parser;
import de.neuland.pug4j.parser.node.Node;
import de.neuland.pug4j.template.FileTemplateLoader;
import de.neuland.pug4j.template.PugTemplate;
import de.neuland.pug4j.template.TemplateLoader;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Template engine for loading, parsing, and caching Pug templates.
 * <p>
 * This is the main entry point for the Pug4J API. Create a PugEngine instance
 * with the desired configuration, then use it to load templates that can be
 * rendered with different models and contexts.
 * </p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * PugEngine engine = PugEngine.builder()
 *     .templateLoader(FileTemplateLoader.builder()
 *         .templateLoaderPath("/templates")
 *         .build())
 *     .filter("markdown", new MarkdownFilter())
 *     .caching(true)
 *     .build();
 *
 * PugTemplate template = engine.getTemplate("index");
 * String html = template.render(model, RenderContext.defaults());
 * }</pre>
 *
 * @since 3.0.0
 */
public class PugEngine {

    private static final String FILTER_CDATA = "cdata";
    private static final String FILTER_CSS = "css";
    private static final String FILTER_JS = "js";
    private static final long MAX_CACHE_ENTRIES = 1000L;

    private final TemplateLoader templateLoader;
    private final ExpressionHandler expressionHandler;
    private final boolean caching;
    private final Map<String, Filter> filters;
    private final Cache<String, PugTemplate> cache;

    private PugEngine(Builder builder) {
        this.templateLoader = builder.templateLoader;
        this.expressionHandler = builder.expressionHandler;
        this.caching = builder.caching;
        this.filters = Collections.unmodifiableMap(new HashMap<>(builder.filters));
        this.cache = Caffeine.newBuilder().maximumSize(MAX_CACHE_ENTRIES).build();

        // Configure expression handler caching
        this.expressionHandler.setCache(this.caching);
    }

    /**
     * Creates a new builder for constructing a PugEngine.
     *
     * @return a new Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a PugEngine configured to load templates from the specified filesystem path.
     * Uses default settings (JEXL expression handler, caching enabled, standard filters).
     *
     * @param templatePath the absolute path to the template directory
     * @return a configured PugEngine instance
     */
    public static PugEngine forPath(String templatePath) {
        return builder()
                .templateLoader(new FileTemplateLoader(Path.of(templatePath)))
                .build();
    }

    /**
     * Creates a PugEngine configured to load templates from the specified filesystem path.
     * Uses default settings (JEXL expression handler, caching enabled, standard filters).
     *
     * @param templatePath the path to the template directory
     * @return a configured PugEngine instance
     */
    public static PugEngine forPath(Path templatePath) {
        return builder()
                .templateLoader(new FileTemplateLoader(templatePath))
                .build();
    }

    /**
     * Loads and parses a template by name. If caching is enabled, returns a cached
     * template if available and the template file has not been modified.
     *
     * @param name the template name/path
     * @return the parsed template
     * @throws IOException if the template cannot be loaded
     * @throws PugException if the template cannot be parsed
     */
    public PugTemplate getTemplate(String name) throws IOException, PugException {
        if (caching) {
            long lastModified = templateLoader.getLastModified(name);
            String cacheKey = getCacheKey(name, lastModified);

            return cache.get(cacheKey, key -> {
                try {
                    return createTemplate(name);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        }

        return createTemplate(name);
    }

    /**
     * Checks whether a template with the given name exists and can be loaded.
     *
     * @param name the template name/path
     * @return true if the template exists, false otherwise
     */
    public boolean templateExists(String name) {
        try {
            return templateLoader.getReader(name) != null;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Clears all cached templates and expression handler caches.
     * This forces templates to be reloaded and reparsed on next access.
     */
    public void clearCache() {
        expressionHandler.clearCache();
        cache.invalidateAll();
    }

    /**
     * Returns the template loader used by this engine.
     *
     * @return the template loader
     */
    public TemplateLoader getTemplateLoader() {
        return templateLoader;
    }

    /**
     * Returns the expression handler used by this engine.
     *
     * @return the expression handler
     */
    public ExpressionHandler getExpressionHandler() {
        return expressionHandler;
    }

    /**
     * Returns whether template caching is enabled.
     *
     * @return true if caching is enabled
     */
    public boolean isCaching() {
        return caching;
    }

    /**
     * Returns an unmodifiable map of all registered filters.
     *
     * @return the filters map
     */
    public Map<String, Filter> getFilters() {
        return filters;
    }

    /**
     * Returns the filter with the specified name, or null if no such filter exists.
     *
     * @param name the filter name
     * @return the filter, or null
     */
    public Filter getFilter(String name) {
        return filters.get(name);
    }

    /**
     * Checks whether a filter with the specified name is registered.
     *
     * @param name the filter name
     * @return true if the filter exists
     */
    public boolean hasFilter(String name) {
        return filters.containsKey(name);
    }

    /**
     * Renders a template with the given model and render context, writing output to a Writer.
     * This is a convenience method that delegates to {@link PugTemplate#render(Map, RenderContext, PugEngine, Writer)}.
     *
     * @param template the template to render
     * @param model the model data
     * @param context the render context with settings like prettyPrint and defaultMode
     * @param writer the writer to output the rendered HTML
     * @throws PugCompilerException if rendering fails
     * @since 3.0.0
     */
    public void render(PugTemplate template, Map<String, Object> model, RenderContext context, Writer writer)
            throws PugCompilerException {
        template.render(model, context, this, writer);
    }

    /**
     * Renders a template with the given model and render context, returning the result as a String.
     * This is a convenience method that delegates to {@link PugTemplate#render(Map, RenderContext, PugEngine)}.
     *
     * @param template the template to render
     * @param model the model data
     * @param context the render context with settings like prettyPrint and defaultMode
     * @return the rendered HTML as a String
     * @throws PugCompilerException if rendering fails
     * @since 3.0.0
     */
    public String render(PugTemplate template, Map<String, Object> model, RenderContext context)
            throws PugCompilerException {
        return template.render(model, context, this);
    }

    /**
     * Renders a template with the given model using default render settings.
     * This is a convenience method that delegates to {@link PugTemplate#render(Map, PugEngine)}.
     *
     * @param template the template to render
     * @param model the model data
     * @return the rendered HTML as a String
     * @throws PugCompilerException if rendering fails
     * @since 3.0.0
     */
    public String render(PugTemplate template, Map<String, Object> model) throws PugCompilerException {
        return template.render(model, this);
    }

    private PugTemplate createTemplate(String name) throws PugException, IOException {
        Parser parser = new Parser(name, templateLoader, expressionHandler);
        Node root = parser.parse();
        return new PugTemplate(root);
    }

    private String getCacheKey(String name, long lastModified) {
        return name + "-" + lastModified;
    }

    /**
     * Builder for creating PugEngine instances.
     */
    public static class Builder {
        private TemplateLoader templateLoader = new FileTemplateLoader();
        private ExpressionHandler expressionHandler = new JexlExpressionHandler();
        private boolean caching = true;
        private Map<String, Filter> filters = new HashMap<>();

        private Builder() {
            // Add default filters
            filters.put(FILTER_CDATA, new CDATAFilter());
            filters.put(FILTER_JS, new JsFilter());
            filters.put(FILTER_CSS, new CssFilter());
        }

        /**
         * Sets the template loader to use for loading template files.
         *
         * @param templateLoader the template loader
         * @return this builder for method chaining
         */
        public Builder templateLoader(TemplateLoader templateLoader) {
            if (templateLoader == null) {
                throw new IllegalArgumentException("templateLoader cannot be null");
            }
            this.templateLoader = templateLoader;
            return this;
        }

        /**
         * Sets the expression handler to use for evaluating expressions in templates.
         *
         * @param expressionHandler the expression handler
         * @return this builder for method chaining
         */
        public Builder expressionHandler(ExpressionHandler expressionHandler) {
            if (expressionHandler == null) {
                throw new IllegalArgumentException("expressionHandler cannot be null");
            }
            this.expressionHandler = expressionHandler;
            return this;
        }

        /**
         * Sets whether to enable template caching.
         *
         * @param caching true to enable caching
         * @return this builder for method chaining
         */
        public Builder caching(boolean caching) {
            this.caching = caching;
            return this;
        }

        /**
         * Registers a filter with the specified name.
         *
         * @param name the filter name
         * @param filter the filter instance
         * @return this builder for method chaining
         */
        public Builder filter(String name, Filter filter) {
            if (name == null) {
                throw new IllegalArgumentException("Filter name cannot be null");
            }
            if (filter == null) {
                throw new IllegalArgumentException("Filter cannot be null");
            }
            this.filters.put(name, filter);
            return this;
        }

        /**
         * Registers multiple filters.
         *
         * @param filters a map of filter names to filter instances
         * @return this builder for method chaining
         */
        public Builder filters(Map<String, Filter> filters) {
            if (filters == null) {
                throw new IllegalArgumentException("Filters map cannot be null");
            }
            this.filters.putAll(filters);
            return this;
        }

        /**
         * Builds a new PugEngine instance with the configured settings.
         *
         * @return a new PugEngine
         */
        public PugEngine build() {
            return new PugEngine(this);
        }
    }
}
