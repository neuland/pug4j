package de.neuland.pug4j.template;

import de.neuland.pug4j.exceptions.PugTemplateLoaderException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * Loads a Pug template from the classpath. It is useful when Pug templates are in the same JAR or
 * WAR. Templates are read through the ClassLoader, so they resolve both from exploded directories
 * and from inside JAR files (including uber JARs).
 *
 * @author emiguel
 */
public class ClasspathTemplateLoader implements TemplateLoader {

  private final String templateLoaderPath;
  private Charset encoding = StandardCharsets.UTF_8;
  private String extension = "pug";
  private String basePath = "";

  /**
   * Normalizes a classpath resource prefix: forward slashes, no leading or trailing slash.
   * ClassLoader.getResource expects paths without a leading slash ("templates", not "/templates")
   * and returns null otherwise.
   */
  private static String normalizeLoaderPath(String path) {
    String resourceName = FilenameUtils.separatorsToUnix(path);
    while (resourceName.startsWith("/")) {
      resourceName = resourceName.substring(1);
    }
    while (resourceName.endsWith("/")) {
      resourceName = resourceName.substring(0, resourceName.length() - 1);
    }
    return resourceName;
  }

  private static ClassLoader classLoader() {
    ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
    return contextClassLoader != null
        ? contextClassLoader
        : ClasspathTemplateLoader.class.getClassLoader();
  }

  private static String validateLoaderPath(String path) {
    String normalized = normalizeLoaderPath(path);
    // The classpath root ("") always exists but is not addressable via getResource inside JARs,
    // so only non-root prefixes can be validated eagerly. Directory lookups require a directory
    // entry (present on the filesystem and in JARs built by standard tooling); JARs with stripped
    // directory entries fail this check even though their templates exist - repackage with
    // directory entries or use the classpath root as loader path in that case.
    if (!normalized.isEmpty()
        && classLoader().getResource(normalized) == null
        && classLoader().getResource(normalized + "/") == null) {
      throw new PugTemplateLoaderException(
          "Classpath resource '" + path + "' does not exist. Expected a path relative to the "
              + "classpath root, e.g. \"templates\" or \"templates/pug\".");
    }
    return normalized;
  }

  public ClasspathTemplateLoader() {
    this("");
  }

  public ClasspathTemplateLoader(Charset encoding) {
    this("");
    this.encoding = encoding;
  }

  public ClasspathTemplateLoader(Charset encoding, String extension) {
    this(encoding);
    this.extension = extension;
  }

  public ClasspathTemplateLoader(String templateLoaderPath) {
    this.templateLoaderPath = validateLoaderPath(templateLoaderPath);
  }

  public ClasspathTemplateLoader(String templateLoaderPath, Charset encoding) {
    this(templateLoaderPath);
    this.encoding = encoding;
  }

  public ClasspathTemplateLoader(String templateLoaderPath, String extension) {
    this(templateLoaderPath);
    this.extension = extension;
  }

  public ClasspathTemplateLoader(String templateLoaderPath, Charset encoding, String extension) {
    this(templateLoaderPath, extension);
    this.encoding = encoding;
  }

  public long getLastModified(String name) {
    return -1;
  }

  @Override
  public Reader getReader(String name) throws IOException {
    if (name == null) {
      throw new IllegalArgumentException("Filename not provided!");
    }
    String resourceName = getResourceName(name);
    InputStream inputStream = classLoader().getResourceAsStream(resourceName);
    if (inputStream == null) {
      throw new FileNotFoundException(
          "Template '" + name + "' not found on classpath (resource '" + resourceName + "').");
    }
    return new InputStreamReader(inputStream, encoding);
  }

  /**
   * Resolves a template name to a classpath resource name, mirroring FileTemplateLoader's path
   * semantics: names starting with "/" are rooted at the loader path plus basePath, relative names
   * at the loader path directly.
   */
  private String getResourceName(String name) {
    name = FilenameUtils.separatorsToUnix(name);
    name = ensurePugExtension(name);
    String relative = name.startsWith("/") ? basePath + name.substring(1) : name;
    String full = templateLoaderPath.isEmpty() ? relative : templateLoaderPath + "/" + relative;
    String normalized = FilenameUtils.normalize(full, true);
    if (normalized == null) {
      throw new PugTemplateLoaderException(
          "Template path '" + name + "' escapes the template loader path.");
    }
    while (normalized.startsWith("/")) {
      normalized = normalized.substring(1);
    }
    return normalized;
  }

  private String ensurePugExtension(String templateName) {
    if (StringUtils.isBlank(FilenameUtils.getExtension(templateName))) {
      return templateName + "." + getExtension();
    }
    return templateName;
  }

  @Override
  public String getExtension() {
    return extension;
  }

  @Override
  public String getBase() {
    return basePath;
  }

  public void setBase(String basePath) {
    if (basePath.endsWith("/") || "".equals(basePath)) this.basePath = basePath;
    else this.basePath = basePath + "/";
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
   * Builder for creating ClasspathTemplateLoader instances. Replaces the multiple constructor
   * overloads with a fluent API.
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
     * Sets the base path for resolving absolute template references (those starting with /). This
     * path is relative to the templateLoaderPath.
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
