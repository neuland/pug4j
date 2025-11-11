package de.neuland.pug4j.template;

import de.neuland.pug4j.exceptions.PugTemplateLoaderException;

import java.io.IOException;
import java.io.Reader;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Paths;

/**
 * Loads a Pug template from Classpath
 * It is useful when Pug templates are in the same JAR or WAR
 *
 * @author emiguel
 */
public class ClasspathTemplateLoader implements TemplateLoader {

    private final FileTemplateLoader fileTemplateLoader;

    private String getResourcePath(String path) {
        try {
            return Paths.get(Thread.currentThread().getContextClassLoader().getResource(path).toURI()).toString();
        } catch (URISyntaxException e) {
            throw new PugTemplateLoaderException("Path '" + path + "' does not exist.");
        }
    }

    public ClasspathTemplateLoader() {
        String path = getResourcePath("");
        fileTemplateLoader = new FileTemplateLoader(path);
    }

    public ClasspathTemplateLoader(Charset encoding) {
        String path = getResourcePath("");
        fileTemplateLoader = new FileTemplateLoader(path, encoding);
    }

    public ClasspathTemplateLoader(Charset encoding, String extension) {
        String path = getResourcePath("");
        fileTemplateLoader = new FileTemplateLoader(path, encoding, extension);
    }

    public ClasspathTemplateLoader(String templateLoaderPath) {
        String path = getResourcePath(templateLoaderPath);
        fileTemplateLoader = new FileTemplateLoader(path);
    }

    public ClasspathTemplateLoader(String templateLoaderPath, Charset encoding) {
        String path = getResourcePath(templateLoaderPath);
        fileTemplateLoader = new FileTemplateLoader(path, encoding);
    }

    public ClasspathTemplateLoader(String templateLoaderPath, String extension) {
        String path = getResourcePath(templateLoaderPath);
        fileTemplateLoader = new FileTemplateLoader(path, extension);
    }

    public ClasspathTemplateLoader(String templateLoaderPath, Charset encoding, String extension) {
        String path = getResourcePath(templateLoaderPath);
        fileTemplateLoader = new FileTemplateLoader(path, encoding, extension);
    }

    public long getLastModified(String name) {
        return -1;
    }

    @Override
    public Reader getReader(String name) throws IOException {
        return fileTemplateLoader.getReader(name);
    }

    @Override
    public String getExtension() {
        return fileTemplateLoader.getExtension();
    }

    @Override
    public String getBase() {
        return fileTemplateLoader.getBase();
    }

    public void setBase(String basePath) {
        fileTemplateLoader.setBase(basePath);
    }

    /**
     * Creates a new builder for constructing a ClasspathTemplateLoader.
     *
     * @return a new Builder instance
     * @since 3.0.0
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for creating ClasspathTemplateLoader instances.
     * Replaces the multiple constructor overloads with a fluent API.
     *
     * @since 3.0.0
     */
    public static class Builder {
        private String templateLoaderPath = "";
        private Charset encoding;
        private String extension;
        private String basePath;

        /**
         * Sets the classpath resource path to use as the template root directory.
         *
         * @param templateLoaderPath the classpath resource path (e.g., "templates" or "templates/pug")
         * @return this builder for method chaining
         */
        public Builder templateLoaderPath(String templateLoaderPath) {
            if (templateLoaderPath == null) {
                throw new IllegalArgumentException("templateLoaderPath cannot be null");
            }
            this.templateLoaderPath = templateLoaderPath;
            return this;
        }

        /**
         * Sets the character encoding for reading template files.
         *
         * @param encoding the character encoding
         * @return this builder for method chaining
         */
        public Builder encoding(Charset encoding) {
            if (encoding == null) {
                throw new IllegalArgumentException("encoding cannot be null");
            }
            this.encoding = encoding;
            return this;
        }

        /**
         * Sets the default file extension for template files.
         *
         * @param extension the file extension (without leading dot)
         * @return this builder for method chaining
         */
        public Builder extension(String extension) {
            if (extension == null) {
                throw new IllegalArgumentException("extension cannot be null");
            }
            this.extension = extension;
            return this;
        }

        /**
         * Sets the base path for resolving absolute template references (those starting with /).
         * This path is relative to the templateLoaderPath.
         *
         * @param basePath the base path subdirectory
         * @return this builder for method chaining
         */
        public Builder basePath(String basePath) {
            if (basePath == null) {
                throw new IllegalArgumentException("basePath cannot be null");
            }
            this.basePath = basePath;
            return this;
        }

        /**
         * Builds a new ClasspathTemplateLoader instance with the configured settings.
         *
         * @return a new ClasspathTemplateLoader
         * @throws PugTemplateLoaderException if the templateLoaderPath resource does not exist
         */
        public ClasspathTemplateLoader build() {
            ClasspathTemplateLoader loader;

            // Use appropriate constructor based on what was set
            if (encoding != null && extension != null) {
                loader = new ClasspathTemplateLoader(templateLoaderPath, encoding, extension);
            } else if (encoding != null) {
                loader = new ClasspathTemplateLoader(templateLoaderPath, encoding);
            } else if (extension != null) {
                loader = new ClasspathTemplateLoader(templateLoaderPath, extension);
            } else if (!templateLoaderPath.isEmpty()) {
                loader = new ClasspathTemplateLoader(templateLoaderPath);
            } else {
                loader = new ClasspathTemplateLoader();
            }

            if (basePath != null && !basePath.isEmpty()) {
                loader.setBase(basePath);
            }

            return loader;
        }
    }
}
