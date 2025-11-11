package de.neuland.pug4j;

import de.neuland.pug4j.expression.ExpressionHandler;
import de.neuland.pug4j.template.FileTemplateLoader;
import de.neuland.pug4j.template.ReaderTemplateLoader;
import java.io.Reader;
import java.nio.file.Path;

/**
 * Reusable test setup for unit tests. Provides simple PugEngine instances with minimal
 * configuration.
 */
public class UnitTestSetup {

  /**
   * Creates a PugEngine with default settings (default template loader, JEXL expression handler,
   * caching enabled).
   *
   * @return configured PugEngine with default settings
   */
  public static PugEngine createEngine() {
    return PugEngine.builder().build();
  }

  /**
   * Creates a PugEngine with default settings (JEXL expression handler, caching enabled).
   *
   * @param templatePath the path to load templates from
   * @return configured PugEngine
   */
  public static PugEngine createEngine(String templatePath) {
    return PugEngine.builder()
        .templateLoader(new FileTemplateLoader(Path.of(templatePath)))
        .build();
  }

  /**
   * Creates a PugEngine with the specified template path and extension.
   *
   * @param templatePath the path to load templates from
   * @param extension the template file extension
   * @return configured PugEngine
   */
  public static PugEngine createEngine(String templatePath, String extension) {
    return PugEngine.builder()
        .templateLoader(new FileTemplateLoader(Path.of(templatePath), extension))
        .build();
  }

  /**
   * Creates a PugEngine with the specified template path and expression handler.
   *
   * @param templatePath the path to load templates from
   * @param expressionHandler the expression handler to use
   * @return configured PugEngine
   */
  public static PugEngine createEngine(String templatePath, ExpressionHandler expressionHandler) {
    return PugEngine.builder()
        .templateLoader(new FileTemplateLoader(Path.of(templatePath)))
        .expressionHandler(expressionHandler)
        .build();
  }

  /**
   * Creates a PugEngine with caching disabled for tests that need to reload templates.
   *
   * @param templatePath the path to load templates from
   * @return configured PugEngine with caching disabled
   */
  public static PugEngine createEngineNoCaching(String templatePath) {
    return PugEngine.builder()
        .templateLoader(new FileTemplateLoader(Path.of(templatePath)))
        .caching(false)
        .build();
  }

  /**
   * Creates a PugEngine from a Reader (for inline template strings).
   *
   * @param reader the reader containing the template
   * @param templateName the name to give the template
   * @return configured PugEngine
   */
  public static PugEngine createEngineFromReader(Reader reader, String templateName) {
    return PugEngine.builder()
        .templateLoader(new ReaderTemplateLoader(reader, templateName))
        .build();
  }

  /**
   * Creates a PugEngine from a Reader with a specific expression handler.
   *
   * @param reader the reader containing the template
   * @param templateName the name to give the template
   * @param expressionHandler the expression handler to use
   * @return configured PugEngine
   */
  public static PugEngine createEngineFromReader(
      Reader reader, String templateName, ExpressionHandler expressionHandler) {
    return PugEngine.builder()
        .templateLoader(new ReaderTemplateLoader(reader, templateName))
        .expressionHandler(expressionHandler)
        .build();
  }

  /**
   * Creates a default RenderContext for tests.
   *
   * @return RenderContext with default settings
   */
  public static RenderContext defaultRenderContext() {
    return RenderContext.defaults();
  }

  /**
   * Creates a RenderContext with pretty printing enabled.
   *
   * @return RenderContext with pretty printing
   */
  public static RenderContext prettyPrintContext() {
    return RenderContext.builder().prettyPrint(true).build();
  }

  /**
   * Creates a RenderContext with the specified mode.
   *
   * @param mode the rendering mode
   * @return RenderContext with the specified mode
   */
  public static RenderContext contextWithMode(Pug4J.Mode mode) {
    return RenderContext.builder().defaultMode(mode).build();
  }

  /**
   * Creates a RenderContext with pretty printing and the specified mode.
   *
   * @param mode the rendering mode
   * @return RenderContext with pretty printing and the specified mode
   */
  public static RenderContext prettyPrintContextWithMode(Pug4J.Mode mode) {
    return RenderContext.builder().prettyPrint(true).defaultMode(mode).build();
  }
}
