package de.neuland.pug4j.integration;

import static org.junit.Assert.assertEquals;

import de.neuland.pug4j.ParameterizedTestCaseHelper;
import de.neuland.pug4j.PugConfiguration;
import de.neuland.pug4j.TestFileHelper;
import de.neuland.pug4j.expression.ExpressionHandler;
import de.neuland.pug4j.expression.JexlExpressionHandler;
import java.io.*;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.HashMap;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class OriginalJadeTest {
  private static String[] ignoredCases =
      new String[] {
        "attrs",
        "attrs.js",
        "code.conditionals",
        "code.iteration",
        "comments",
        "escape-chars",
        "filters.coffeescript",
        "filters.less",
        "filters.markdown",
        "filters.stylus",
        "html",
        "include-only-text-body",
        "include-only-text",
        "include-with-text-head",
        "include-with-text",
        "mixin.blocks",
        "mixin.merge",
        "quotes",
        "script.whitespace",
        "scripts",
        "scripts.non-js",
        "source",
        "styles",
        "template",
        "text-block",
        "text",
        "vars",
        "yield-title",
        "doctype.default",
        "comments.conditional",
        "html5"
      };

  private String file;

  public OriginalJadeTest(String file) {
    this.file = file;
  }

  @Test
  public void shouldCompileJadeToHtml() throws Exception {
    String basePath = "";
    String fileTemplateLoaderPath = TestFileHelper.getOriginalResourcePath("");
    String extension = "jade";
    ExpressionHandler expressionHandler = new JexlExpressionHandler();
    ParameterizedTestCaseHelper testHelper =
        new ParameterizedTestCaseHelper(
            fileTemplateLoaderPath, basePath, extension, expressionHandler);
    PugConfiguration pugConfiguration = testHelper.getPugConfiguration();
    pugConfiguration.setPrettyPrint(false);
    HashMap<String, Object> model = new HashMap<String, Object>();
    model.put("title", "Pug");

    String actual = testHelper.getActualHtml(file, pugConfiguration, model);
    String expected = testHelper.getExpectedHtml(file);

    assertEquals(file, expected, actual);
  }

  @Parameterized.Parameters(name = "{0}")
  public static Collection<String[]> data() throws FileNotFoundException, URISyntaxException {
    String resourcePath = TestFileHelper.getOriginalResourcePath("");
    String extension = "jade";
    return ParameterizedTestCaseHelper.createTestFileData(resourcePath, extension, ignoredCases);
  }
}
