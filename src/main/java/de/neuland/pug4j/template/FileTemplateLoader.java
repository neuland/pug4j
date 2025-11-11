package de.neuland.pug4j.template;

import de.neuland.pug4j.exceptions.PugTemplateLoaderException;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.concurrent.TimeUnit;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileTemplateLoader implements TemplateLoader {

  private final String separator = FileSystems.getDefault().getSeparator();
  private Charset encoding = StandardCharsets.UTF_8;
  private Path templateLoaderPath = null;
  private String extension = "pug";
  private String basePath = "";
  private Logger logger = LoggerFactory.getLogger(this.getClass());

  public FileTemplateLoader() {}

  public FileTemplateLoader(Charset encoding) {
    this.encoding = encoding;
  }

  public FileTemplateLoader(Charset encoding, String extension) {
    this(encoding);
    this.extension = extension;
  }

  public FileTemplateLoader(String templateLoaderPath) {
    this(Paths.get(FilenameUtils.separatorsToSystem(templateLoaderPath)));
  }

  public FileTemplateLoader(String templateLoaderPath, Charset encoding) {
    this(templateLoaderPath);
    this.encoding = encoding;
  }

  public FileTemplateLoader(String templateLoaderPath, String extension) {
    this(templateLoaderPath);
    this.extension = extension;
  }

  public FileTemplateLoader(String templateLoaderPath, Charset encoding, String extension) {
    this(templateLoaderPath, extension);
    this.encoding = encoding;
  }

  public FileTemplateLoader(Path templateLoaderPath) {
    if (templateLoaderPath == null || !Files.isDirectory(templateLoaderPath)) {
      throw new PugTemplateLoaderException(
          "Directory '" + templateLoaderPath + "' does not exist.");
    }
    this.templateLoaderPath = templateLoaderPath.toAbsolutePath();
  }

  public FileTemplateLoader(Path templateLoaderPath, Charset encoding) {
    this(templateLoaderPath);
    this.encoding = encoding;
  }

  public FileTemplateLoader(Path templateLoaderPath, String extension) {
    this(templateLoaderPath);
    this.extension = extension;
  }

  public FileTemplateLoader(Path templateLoaderPath, Charset encoding, String extension) {
    this(templateLoaderPath, extension);
    this.encoding = encoding;
  }

  public long getLastModified(String name) throws IOException {
    Path filepath = getFilepath(name);
    return Files.getLastModifiedTime(filepath).to(TimeUnit.MILLISECONDS);
  }

  @Override
  public Reader getReader(String name) throws IOException {
    if (name == null) {
      throw new IllegalArgumentException("Filename not provided!");
    }
    Path filepath = getFilepath(name);
    logger.debug("Template: " + name + " resolved filepath is " + filepath.toAbsolutePath());
    final InputStream inputStream = Files.newInputStream(filepath);
    return new InputStreamReader(inputStream, encoding);
  }

  private String ensurePugExtension(String templateName) {
    if (StringUtils.isBlank(FilenameUtils.getExtension(templateName))) {
      return templateName + "." + getExtension();
    }
    return templateName;
  }

  private Path getFilepath(String name) {
    name = FilenameUtils.separatorsToSystem(name);
    name = ensurePugExtension(name);
    if (templateLoaderPath != null) {
      if (name.startsWith(separator)) {
        // absolute-rooted within loader: apply basePath and strip leading separator
        String rel = name.substring(1);
        Path p = templateLoaderPath.resolve(basePath).resolve(rel).normalize();
        return p;
      } else {
        return templateLoaderPath.resolve(name).normalize();
      }
    } else {
      return Paths.get(name);
    }
  }

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
   * Creates a new builder for constructing a FileTemplateLoader.
   *
   * @return a new Builder instance
   * @since 3.0.0
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Builder for creating FileTemplateLoader instances. Replaces the multiple constructor overloads
   * with a fluent API.
   *
   * @since 3.0.0
   */
  public static class Builder {
    private Path templateLoaderPath;
    private Charset encoding = StandardCharsets.UTF_8;
    private String extension = "pug";
    private String basePath = "";

    /**
     * Sets the template loader path as a String. The path will be converted to an absolute Path.
     *
     * @param templateLoaderPath the path to the template directory
     * @return this builder for method chaining
     */
    public Builder templateLoaderPath(String templateLoaderPath) {
      if (templateLoaderPath == null) {
        throw new IllegalArgumentException("templateLoaderPath cannot be null");
      }
      this.templateLoaderPath = Paths.get(FilenameUtils.separatorsToSystem(templateLoaderPath));
      return this;
    }

    /**
     * Sets the template loader path as a Path.
     *
     * @param templateLoaderPath the path to the template directory
     * @return this builder for method chaining
     */
    public Builder templateLoaderPath(Path templateLoaderPath) {
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
     * Builds a new FileTemplateLoader instance with the configured settings.
     *
     * @return a new FileTemplateLoader
     * @throws PugTemplateLoaderException if templateLoaderPath is not set or is not a directory
     */
    public FileTemplateLoader build() {
      FileTemplateLoader loader;

      if (templateLoaderPath != null) {
        loader = new FileTemplateLoader(templateLoaderPath, encoding, extension);
      } else {
        loader = new FileTemplateLoader(encoding, extension);
      }

      if (!basePath.isEmpty()) {
        loader.setBase(basePath);
      }

      return loader;
    }
  }
}
