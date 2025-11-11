package de.neuland.pug4j;

import static org.junit.Assert.assertEquals;

import de.neuland.pug4j.exceptions.PugLexerException;
import de.neuland.pug4j.exceptions.PugParserException;
import de.neuland.pug4j.template.FileTemplateLoader;
import de.neuland.pug4j.template.PugTemplate;
import java.io.StringWriter;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.io.FilenameUtils;
import org.junit.Test;

public class Pug4JTest {

  @Test
  public void testRenderDefault() throws Exception {
    final Path path = Paths.get("src/test/resources/compiler/extends.pug");
    final String html =
        Pug4J.render(path.toAbsolutePath().toString(), new HashMap<String, Object>());
    assertEquals(
        "<h1>hello world</h1><p>default foo</p><p>special bar</p><div class=\"prepend\">hello world</div><div class=\"append\">hello world</div><ul><li>1</li><li>2</li><li>3</li><li>4</li></ul><ul><li>a</li><li>b</li><li>c</li><li>d</li></ul>",
        html);
  }

  @Test(expected = NoSuchFileException.class)
  public void testRenderUnknownFile() throws Exception {
    final Path path = Paths.get("src/test/resources/compiler/extendsDoesNotExist.pug");
    final String html =
        Pug4J.render(path.toAbsolutePath().toString(), new HashMap<String, Object>());
    assertEquals(
        "<h1>hello world</h1><p>default foo</p><p>special bar</p><div class=\"prepend\">hello world</div><div class=\"append\">hello world</div><ul><li>1</li><li>2</li><li>3</li><li>4</li></ul><ul><li>a</li><li>b</li><li>c</li><li>d</li></ul>",
        html);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testRenderNull() throws Exception {
    String fullPath = null;
    final String html = Pug4J.render(fullPath, new HashMap<String, Object>());
    assertEquals(
        "<h1>hello world</h1><p>default foo</p><p>special bar</p><div class=\"prepend\">hello world</div><div class=\"append\">hello world</div><ul><li>1</li><li>2</li><li>3</li><li>4</li></ul><ul><li>a</li><li>b</li><li>c</li><li>d</li></ul>",
        html);
  }

  @Test(expected = PugParserException.class) // layout does not exist
  public void testRenderUnknownInclude() throws Exception {
    final Path path = Paths.get("src/test/resources/compiler/extendsUnknownInclude.pug");
    final String html =
        Pug4J.render(path.toAbsolutePath().toString(), new HashMap<String, Object>());
    assertEquals(
        "<h1>hello world</h1><p>default foo</p><p>special bar</p><div class=\"prepend\">hello world</div><div class=\"append\">hello world</div><ul><li>1</li><li>2</li><li>3</li><li>4</li></ul><ul><li>a</li><li>b</li><li>c</li><li>d</li></ul>",
        html);
  }

  @Test
  public void testRenderDefaultRelativePath() throws Exception {
    final String html =
        Pug4J.render("./src/test/resources/compiler/extends.pug", new HashMap<String, Object>());
    assertEquals(
        "<h1>hello world</h1><p>default foo</p><p>special bar</p><div class=\"prepend\">hello world</div><div class=\"append\">hello world</div><ul><li>1</li><li>2</li><li>3</li><li>4</li></ul><ul><li>a</li><li>b</li><li>c</li><li>d</li></ul>",
        html);
  }

  @Test
  public void testTemplateDefault() throws Exception {
    final Path path = Paths.get("src/test/resources/compiler/extends.pug");
    PugTemplate template = Pug4J.getTemplate(path.toAbsolutePath().toString());
    final String html = Pug4J.render(template, new HashMap<String, Object>());
    assertEquals(
        "<h1>hello world</h1><p>default foo</p><p>special bar</p><div class=\"prepend\">hello world</div><div class=\"append\">hello world</div><ul><li>1</li><li>2</li><li>3</li><li>4</li></ul><ul><li>a</li><li>b</li><li>c</li><li>d</li></ul>",
        html);
  }

  @Test
  public void testDefaultWithWriter() throws Exception {
    final Path path = Paths.get("src/test/resources/compiler/extends.pug");
    final StringWriter writer = new StringWriter();
    Pug4J.render(path.toAbsolutePath().toString(), new HashMap<String, Object>(), writer);
    final String html = writer.toString();
    assertEquals(
        "<h1>hello world</h1><p>default foo</p><p>special bar</p><div class=\"prepend\">hello world</div><div class=\"append\">hello world</div><ul><li>1</li><li>2</li><li>3</li><li>4</li></ul><ul><li>a</li><li>b</li><li>c</li><li>d</li></ul>",
        html);
  }

  @Test
  public void testTemplateDefaultWithWriter() throws Exception {
    final Path path = Paths.get("src/test/resources/compiler/extends.pug");
    PugTemplate template = Pug4J.getTemplate(path.toAbsolutePath().toString());
    final StringWriter writer = new StringWriter();
    Pug4J.render(template, new HashMap<String, Object>(), writer);
    String html = writer.toString();
    assertEquals(
        "<h1>hello world</h1><p>default foo</p><p>special bar</p><div class=\"prepend\">hello world</div><div class=\"append\">hello world</div><ul><li>1</li><li>2</li><li>3</li><li>4</li></ul><ul><li>a</li><li>b</li><li>c</li><li>d</li></ul>",
        html);
  }

  @Test
  public void testTemplateDefaultWithWriterPretty() throws Exception {
    final Path path = Paths.get("src/test/resources/compiler/extends.pug");
    PugTemplate template = Pug4J.getTemplate(path.toAbsolutePath().toString());
    final StringWriter writer = new StringWriter();
    Pug4J.render(template, new HashMap<String, Object>(), writer, true);
    String html = writer.toString();
    assertEquals(
        "\n"
            + "<h1>hello world</h1>\n"
            + "<p>default foo</p>\n"
            + "<p>special bar</p>\n"
            + "<div class=\"prepend\">\n"
            + "  hello world\n"
            + "</div>\n"
            + "<div class=\"append\">\n"
            + "  hello world\n"
            + "</div>\n"
            + "<ul>\n"
            + "  <li>1</li>\n"
            + "  <li>2</li>\n"
            + "  <li>3</li>\n"
            + "  <li>4</li>\n"
            + "</ul>\n"
            + "<ul>\n"
            + "  <li>a</li>\n"
            + "  <li>b</li>\n"
            + "  <li>c</li>\n"
            + "  <li>d</li>\n"
            + "</ul>",
        html);
  }

  @Test
  public void testConfigurationDefault() throws Exception {
    final Path path = Paths.get("src/test/resources/compiler/extends.pug");
    String fileName = path.toAbsolutePath().toString();
    PugEngine engine =
        PugEngine.builder()
            .templateLoader(new FileTemplateLoader(FilenameUtils.getFullPath(fileName)))
            .build();
    PugTemplate template = engine.getTemplate(FilenameUtils.getName(fileName));
    Map<String, Object> model = new HashMap<String, Object>();
    final String html = engine.render(template, model);
    assertEquals(
        "<h1>hello world</h1><p>default foo</p><p>special bar</p><div class=\"prepend\">hello world</div><div class=\"append\">hello world</div><ul><li>1</li><li>2</li><li>3</li><li>4</li></ul><ul><li>a</li><li>b</li><li>c</li><li>d</li></ul>",
        html);
  }

  @Test
  public void testConfigurationDefaultWithRelativePath() throws Exception {
    PugEngine engine = PugEngine.builder().build();
    PugTemplate template = engine.getTemplate("src/test/resources/compiler/extends.pug");
    Map<String, Object> model = new HashMap<String, Object>();
    final String html = engine.render(template, model);
    assertEquals(
        "<h1>hello world</h1><p>default foo</p><p>special bar</p><div class=\"prepend\">hello world</div><div class=\"append\">hello world</div><ul><li>1</li><li>2</li><li>3</li><li>4</li></ul><ul><li>a</li><li>b</li><li>c</li><li>d</li></ul>",
        html);
  }

  @Test(expected = PugParserException.class)
  public void testRenderDefaultWithTemplateOutsideTemplateLoaderPath() throws Exception {
    final Path path = Paths.get("src/test/resources/compiler/subdir/extends.pug");
    final String html =
        Pug4J.render(path.toAbsolutePath().toString(), new HashMap<String, Object>());
    assertEquals(
        "<h1>hello world</h1><p>default foo</p><p>special bar</p><div class=\"prepend\">hello world</div><div class=\"append\">hello world</div><ul><li>1</li><li>2</li><li>3</li><li>4</li></ul><ul><li>a</li><li>b</li><li>c</li><li>d</li></ul>",
        html);
  }

  @Test(expected = PugParserException.class) // Jade is not supported anymore
  public void testRenderJade() throws Exception {
    final Path path = Paths.get("src/test/resources/compiler/extends.jade");
    final String html =
        Pug4J.render(path.toAbsolutePath().toString(), new HashMap<String, Object>());
    assertEquals(
        "<h1>hello world</h1><p>default foo</p><p>special bar</p><div class=\"prepend\">hello world</div><div class=\"append\">hello world</div><ul><li>1</li><li>2</li><li>3</li><li>4</li></ul><ul><li>a</li><li>b</li><li>c</li><li>d</li></ul>",
        html);
  }

  @Test(expected = PugParserException.class) // Jade is not supported anymore
  public void testRenderJadeWithTemplateOutsideTemplateLoaderPath() throws Exception {
    final Path path = Paths.get("src/test/resources/compiler/subdir/extends.jade");
    Pug4J.render(path.toAbsolutePath().toString(), new HashMap<String, Object>());
  }

  @Test(expected = PugLexerException.class)
  public void testpug006() throws Exception {
    try {
      final String html =
          Pug4J.render("src/test/resources/errors/pug006.pug", new HashMap<String, Object>());
    } catch (PugLexerException exception) {
      assertEquals(
          "End of line was reached with no closing bracket for interpolation.",
          exception.getMessage());
      assertEquals(
          "class de.neuland.pug4j.exceptions.PugLexerException: pug006.pug:1:16\n"
              + "  > 1| h1 #{variable'}\n"
              + "----------------------^\n"
              + "\n"
              + "End of line was reached with no closing bracket for interpolation.",
          exception.toString());
      throw exception;
    }
  }
}
