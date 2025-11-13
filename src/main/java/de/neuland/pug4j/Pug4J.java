package de.neuland.pug4j;

import de.neuland.pug4j.exceptions.PugCompilerException;
import de.neuland.pug4j.template.FileTemplateLoader;
import de.neuland.pug4j.template.PugTemplate;
import de.neuland.pug4j.template.ReaderTemplateLoader;
import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.apache.commons.io.FilenameUtils;

public class Pug4J {

  public enum Mode {
    HTML,
    XML,
    XHTML
  }

  public static String render(String filename, Map<String, Object> model)
      throws IOException, PugCompilerException {
    return render(filename, model, false);
  }

  public static String render(String filename, Map<String, Object> model, boolean pretty)
      throws IOException, PugCompilerException {
    final StringWriter writer = new StringWriter();
    render(filename, model, writer, pretty);
    return writer.toString();
  }

  public static void render(String filename, Map<String, Object> model, Writer writer)
      throws IOException, PugCompilerException {
    render(filename, model, writer, false);
  }

  public static void render(
      String filename, Map<String, Object> model, Writer writer, boolean pretty)
      throws IOException, PugCompilerException {
    if (filename == null) {
      throw new IllegalArgumentException("Filename can not be null");
    }

    String prefix = FilenameUtils.getFullPath(filename);
    String filePath = FilenameUtils.getName(filename);

    PugEngine engine =
        PugEngine.builder()
            .templateLoader(new FileTemplateLoader(prefix, StandardCharsets.UTF_8))
            .build();

    RenderContext context =
        pretty ? RenderContext.builder().prettyPrint(true).build() : RenderContext.defaults();

    PugTemplate template = engine.getTemplate(filePath);
    engine.render(template, model, context, writer);
  }

  /**
   * @deprecated Use {@link PugEngine#render(PugTemplate, Map, RenderContext)} instead.
   */
  @Deprecated
  public static String render(PugTemplate template, Map<String, Object> model)
      throws PugCompilerException {
    return render(template, model, false);
  }

  /**
   * @deprecated Use {@link PugEngine#render(PugTemplate, Map, RenderContext)} instead.
   */
  @Deprecated
  public static String render(PugTemplate template, Map<String, Object> model, boolean pretty)
      throws PugCompilerException {
    PugEngine engine = PugEngine.builder().build();
    RenderContext context =
        pretty ? RenderContext.builder().prettyPrint(true).build() : RenderContext.defaults();
    return engine.render(template, model, context);
  }

  /**
   * @deprecated Use {@link PugEngine#render(PugTemplate, Map, RenderContext, Writer)} instead.
   */
  @Deprecated
  public static void render(PugTemplate template, Map<String, Object> model, Writer writer)
      throws PugCompilerException {
    render(template, model, writer, false);
  }

  /**
   * @deprecated Use {@link PugEngine#render(PugTemplate, Map, RenderContext, Writer)} instead.
   */
  @Deprecated
  public static void render(
      PugTemplate template, Map<String, Object> model, Writer writer, boolean pretty)
      throws PugCompilerException {
    PugEngine engine = PugEngine.builder().build();
    RenderContext context =
        pretty ? RenderContext.builder().prettyPrint(true).build() : RenderContext.defaults();
    engine.render(template, model, context, writer);
  }

  /**
   * @deprecated This method is rarely used. Create a {@link PugEngine} and use {@link
   *     PugEngine#render(PugTemplate, Map, RenderContext)} instead.
   */
  @Deprecated
  public static String render(URL url, Map<String, Object> model)
      throws IOException, PugCompilerException {
    return render(url, model, false);
  }

  /**
   * @deprecated This method is rarely used. Create a {@link PugEngine} and use {@link
   *     PugEngine#render(PugTemplate, Map, RenderContext)} instead.
   */
  @Deprecated
  public static String render(URL url, Map<String, Object> model, boolean pretty)
      throws IOException, PugCompilerException {
    try (BufferedReader reader =
        new BufferedReader(new InputStreamReader(url.openStream(), StandardCharsets.UTF_8))) {
      ReaderTemplateLoader loader = new ReaderTemplateLoader(reader, url.getPath());
      PugEngine engine = PugEngine.builder().templateLoader(loader).build();
      RenderContext context =
          pretty ? RenderContext.builder().prettyPrint(true).build() : RenderContext.defaults();
      PugTemplate template = engine.getTemplate(url.getPath());
      return engine.render(template, model, context);
    }
  }

  /**
   * @deprecated This method is rarely used. Create a {@link PugEngine} with {@link
   *     de.neuland.pug4j.template.ReaderTemplateLoader} and use {@link
   *     PugEngine#render(PugTemplate, Map, RenderContext)} instead.
   */
  @Deprecated
  public static String render(Reader reader, String filename, Map<String, Object> model)
      throws IOException, PugCompilerException {
    return render(reader, filename, model, false);
  }

  /**
   * @deprecated This method is rarely used. Create a {@link PugEngine} with {@link
   *     de.neuland.pug4j.template.ReaderTemplateLoader} and use {@link
   *     PugEngine#render(PugTemplate, Map, RenderContext)} instead.
   */
  @Deprecated
  public static String render(
      Reader reader, String filename, Map<String, Object> model, boolean pretty)
      throws IOException, PugCompilerException {
    ReaderTemplateLoader loader = new ReaderTemplateLoader(reader, filename);
    PugEngine engine = PugEngine.builder().templateLoader(loader).build();
    RenderContext context =
        pretty ? RenderContext.builder().prettyPrint(true).build() : RenderContext.defaults();
    PugTemplate template = engine.getTemplate(filename);
    return engine.render(template, model, context);
  }

  /**
   * @deprecated This method is not part of the simple API. Create a {@link PugEngine} and use
   *     {@link PugEngine#getTemplate(String)} instead.
   */
  @Deprecated
  public static PugTemplate getTemplate(String filename) throws IOException {
    if (filename == null) {
      throw new IllegalArgumentException("Filename can not be null");
    }

    String prefix = FilenameUtils.getFullPath(filename);
    String filePath = FilenameUtils.getName(filename);

    PugEngine engine =
        PugEngine.builder()
            .templateLoader(new FileTemplateLoader(prefix, StandardCharsets.UTF_8))
            .build();

    return engine.getTemplate(filePath);
  }

  /**
   * @deprecated This method is not part of the simple API. Create a {@link PugEngine} and use
   *     {@link PugEngine#getTemplate(String)} instead.
   */
  @Deprecated
  public static PugTemplate getTemplate(String filename, String extension) throws IOException {
    if (filename == null) {
      throw new IllegalArgumentException("Filename can not be null");
    }

    String prefix = FilenameUtils.getFullPath(filename);
    String filePath = FilenameUtils.getName(filename);

    PugEngine engine =
        PugEngine.builder()
            .templateLoader(new FileTemplateLoader(prefix, StandardCharsets.UTF_8, extension))
            .build();

    return engine.getTemplate(filePath);
  }
}
