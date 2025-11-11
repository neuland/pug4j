package de.neuland.pug4j;

import de.neuland.pug4j.Pug4J.Mode;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Render-time configuration options for template rendering.
 * Instances can be reused across multiple render operations.
 *
 * @since 3.0.0
 */
public class RenderContext {

    private final boolean prettyPrint;
    private final Mode defaultMode;
    private final Map<String, Object> globalVariables;

    private RenderContext(Builder builder) {
        this.prettyPrint = builder.prettyPrint;
        this.defaultMode = builder.defaultMode;
        this.globalVariables = Collections.unmodifiableMap(new HashMap<>(builder.globalVariables));
    }

    /**
     * Creates a new builder for constructing a RenderContext.
     *
     * @return a new Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns a default RenderContext with:
     * - prettyPrint = false
     * - defaultMode = Mode.HTML
     * - no global variables
     *
     * @return a default RenderContext instance
     */
    public static RenderContext defaults() {
        return new Builder().build();
    }

    /**
     * Returns whether pretty printing is enabled.
     *
     * @return true if HTML output should be formatted with indentation and newlines
     */
    public boolean isPrettyPrint() {
        return prettyPrint;
    }

    /**
     * Returns the default mode used as a fallback when the template has no doctype.
     *
     * @return the default Mode (HTML, XML, or XHTML)
     */
    public Mode getDefaultMode() {
        return defaultMode;
    }

    /**
     * Returns an unmodifiable map of global variables available to all templates.
     *
     * @return the global variables map
     */
    public Map<String, Object> getGlobalVariables() {
        return globalVariables;
    }

    /**
     * Builder for creating RenderContext instances.
     */
    public static class Builder {
        private boolean prettyPrint = false;
        private Mode defaultMode = Mode.HTML;
        private Map<String, Object> globalVariables = new HashMap<>();

        /**
         * Sets whether to format HTML output with indentation and newlines.
         *
         * @param prettyPrint true to enable pretty printing
         * @return this builder for method chaining
         */
        public Builder prettyPrint(boolean prettyPrint) {
            this.prettyPrint = prettyPrint;
            return this;
        }

        /**
         * Sets the default mode to use as a fallback when the template has no doctype.
         *
         * @param defaultMode the default Mode (HTML, XML, or XHTML)
         * @return this builder for method chaining
         */
        public Builder defaultMode(Mode defaultMode) {
            if (defaultMode == null) {
                throw new IllegalArgumentException("defaultMode cannot be null");
            }
            this.defaultMode = defaultMode;
            return this;
        }

        /**
         * Adds a global variable that will be available to all templates.
         *
         * @param name the variable name
         * @param value the variable value
         * @return this builder for method chaining
         */
        public Builder globalVariable(String name, Object value) {
            if (name == null) {
                throw new IllegalArgumentException("Variable name cannot be null");
            }
            this.globalVariables.put(name, value);
            return this;
        }

        /**
         * Adds multiple global variables that will be available to all templates.
         *
         * @param variables a map of variable names to values
         * @return this builder for method chaining
         */
        public Builder globalVariables(Map<String, Object> variables) {
            if (variables == null) {
                throw new IllegalArgumentException("Variables map cannot be null");
            }
            this.globalVariables.putAll(variables);
            return this;
        }

        /**
         * Builds a new RenderContext instance.
         *
         * @return a new RenderContext
         */
        public RenderContext build() {
            return new RenderContext(this);
        }
    }
}
