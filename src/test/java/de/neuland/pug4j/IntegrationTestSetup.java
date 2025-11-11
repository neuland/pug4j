package de.neuland.pug4j;

import static org.junit.Assert.assertEquals;

import de.neuland.pug4j.expression.ExpressionHandler;
import de.neuland.pug4j.expression.JexlExpressionHandler;
import de.neuland.pug4j.filter.CDATAFilter;
import de.neuland.pug4j.filter.MarkdownFilter;
import de.neuland.pug4j.filter.PlainFilter;
import de.neuland.pug4j.template.FileTemplateLoader;
import de.neuland.pug4j.template.PugTemplate;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NotNull;

/**
 * Reusable test setup for integration tests. Provides a configured PugEngine with standard filters
 * and settings for testing Pug template compilation and rendering.
 */
public class IntegrationTestSetup {

  private final String fileTemplateLoaderPath;
  private final String basePath;
  private final String extension;
  private final PugEngine pugEngine;

  /**
   * Creates a new integration test setup with the specified configuration.
   *
   * @param fileTemplateLoaderPath the base path for template loading
   * @param basePath the subdirectory within the base path
   * @param extension the file extension for templates
   * @param expressionHandler the expression handler to use
   */
  public IntegrationTestSetup(
      String fileTemplateLoaderPath,
      String basePath,
      String extension,
      ExpressionHandler expressionHandler) {
    this(fileTemplateLoaderPath, basePath, extension, expressionHandler, null);
  }

  /**
   * Creates a new integration test setup with the specified configuration and custom engine
   * builder.
   *
   * @param fileTemplateLoaderPath the base path for template loading
   * @param basePath the subdirectory within the base path
   * @param extension the file extension for templates
   * @param expressionHandler the expression handler to use
   * @param engineCustomizer optional customizer to further configure the engine builder (can be
   *     null)
   */
  public IntegrationTestSetup(
      String fileTemplateLoaderPath,
      String basePath,
      String extension,
      ExpressionHandler expressionHandler,
      EngineCustomizer engineCustomizer) {
    this.fileTemplateLoaderPath = fileTemplateLoaderPath;
    this.basePath = basePath;
    this.extension = extension;

    FileTemplateLoader fileTemplateLoader =
        new FileTemplateLoader(Path.of(fileTemplateLoaderPath), extension);
    fileTemplateLoader.setBase(basePath);

    PugEngine.Builder builder =
        PugEngine.builder()
            .templateLoader(fileTemplateLoader)
            .expressionHandler(expressionHandler)
            .filter("plain", new PlainFilter())
            .filter("cdata", new CDATAFilter())
            .filter("markdown", new MarkdownFilter())
            .filter("markdown-it", new MarkdownFilter())
            .filter(
                "custom",
                (source, attributes, model) -> {
                  Object opt = attributes.get("opt");
                  Object num = attributes.get("num");
                  assertEquals("val", opt);
                  assertEquals(2, num);
                  return "BEGIN" + source + "END";
                })
            .filter("verbatim", (source, attributes, model) -> "\n" + source + "\n");

    if (engineCustomizer != null) {
      engineCustomizer.customize(builder);
    }

    this.pugEngine = builder.build();
  }

  /** Functional interface for customizing the PugEngine builder. */
  @FunctionalInterface
  public interface EngineCustomizer {
    void customize(PugEngine.Builder builder);
  }

  /**
   * Creates a default integration test setup for Pug4J tests with JEXL expression handler.
   *
   * @param basePath the subdirectory within the test resources
   * @return configured IntegrationTestSetup
   * @throws FileNotFoundException if the resource path cannot be found
   * @throws URISyntaxException if the URI syntax is invalid
   */
  public static IntegrationTestSetup forPug4JTests(String basePath)
      throws FileNotFoundException, URISyntaxException {
    return new IntegrationTestSetup(
        TestFileHelper.getPug4JTestsResourcePath(""), basePath, "pug", new JexlExpressionHandler());
  }

  /**
   * Creates a default integration test setup for cases tests.
   *
   * @return configured IntegrationTestSetup for cases
   * @throws FileNotFoundException if the resource path cannot be found
   * @throws URISyntaxException if the URI syntax is invalid
   */
  public static IntegrationTestSetup forCases() throws FileNotFoundException, URISyntaxException {
    return forPug4JTests("cases");
  }

  /**
   * Gets the configured PugEngine instance.
   *
   * @return the PugEngine
   */
  public PugEngine getPugEngine() {
    return pugEngine;
  }

  /**
   * Renders a template and returns the HTML output, trimmed and with carriage returns removed.
   *
   * @param filename the template filename
   * @param model the model data
   * @return the rendered HTML
   * @throws IOException if the template cannot be loaded or rendered
   */
  @NotNull
  public String getActualHtml(String filename, HashMap<String, Object> model) throws IOException {
    return getActualHtml(filename, model, true);
  }

  /**
   * Renders a template and returns the HTML output, trimmed and with carriage returns removed.
   *
   * @param filename the template filename
   * @param model the model data
   * @param prettyPrint whether to enable pretty printing
   * @return the rendered HTML
   * @throws IOException if the template cannot be loaded or rendered
   */
  @NotNull
  public String getActualHtml(String filename, HashMap<String, Object> model, boolean prettyPrint)
      throws IOException {
    PugTemplate template = pugEngine.getTemplate(File.separator + filename);
    RenderContext context =
        RenderContext.builder().prettyPrint(prettyPrint).defaultMode(Pug4J.Mode.XHTML).build();
    Writer writer = new StringWriter();
    pugEngine.render(template, model, context, writer);
    String html = writer.toString();
    return html.trim().replaceAll("\r", "");
  }

  /**
   * Reads the expected HTML from the corresponding .html file.
   *
   * @param filename the template filename
   * @return the expected HTML
   * @throws IOException if the file cannot be read
   */
  @NotNull
  public String getExpectedHtml(String filename) throws IOException {
    String filePath =
        fileTemplateLoaderPath
            + File.separator
            + basePath
            + File.separator
            + filename.replace("." + extension, ".html");
    String content = FileUtils.readFileToString(new File(filePath), "UTF-8");
    return content.trim().replaceAll("\r", "");
  }

  /**
   * Creates test data for parameterized tests by scanning for template files.
   *
   * @param resourcePath the path to scan for templates
   * @param extension the file extension to look for
   * @param ignoredCases array of case names to ignore
   * @return collection of test data
   */
  @NotNull
  public static Collection<String[]> createTestFileData(
      String resourcePath, String extension, String[] ignoredCases) {
    File folder = new File(resourcePath);
    Collection<File> files = FileUtils.listFiles(folder, new String[] {extension}, false);

    Collection<String[]> data = new ArrayList<String[]>();
    for (File file : files) {
      if (!ArrayUtils.contains(ignoredCases, file.getName().replace("." + extension, ""))) {
        data.add(new String[] {file.getName()});
      }
    }
    return data;
  }
}
