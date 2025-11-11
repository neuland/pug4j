package de.neuland.pug4j.integration;

import static org.junit.Assert.assertEquals;

import de.neuland.pug4j.IntegrationTestSetup;
import de.neuland.pug4j.TestFileHelper;
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

  private static final IntegrationTestSetup testSetup;

  static {
    try {
      testSetup =
          new IntegrationTestSetup(
              TestFileHelper.getOriginalResourcePath(""), "", "jade", new JexlExpressionHandler());
    } catch (FileNotFoundException | URISyntaxException e) {
      throw new RuntimeException("Failed to initialize test setup", e);
    }
  }

  private String file;

  public OriginalJadeTest(String file) {
    this.file = file;
  }

  @Test
  public void shouldCompileJadeToHtml() throws Exception {
    HashMap<String, Object> model = new HashMap<String, Object>();
    model.put("title", "Pug");

    String actual = testSetup.getActualHtml(file, model, false);
    String expected = testSetup.getExpectedHtml(file);

    assertEquals(file, expected, actual);
  }

  @Parameterized.Parameters(name = "{0}")
  public static Collection<String[]> data() throws FileNotFoundException, URISyntaxException {
    String resourcePath = TestFileHelper.getOriginalResourcePath("");
    String extension = "jade";
    return IntegrationTestSetup.createTestFileData(resourcePath, extension, ignoredCases);
  }
}
