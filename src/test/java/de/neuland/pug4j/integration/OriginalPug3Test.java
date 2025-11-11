package de.neuland.pug4j.integration;

import static org.junit.Assert.assertEquals;

import de.neuland.pug4j.IntegrationTestSetup;
import de.neuland.pug4j.TestFileHelper;
import de.neuland.pug4j.expression.GraalJsExpressionHandler;
import java.io.*;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.HashMap;
import org.apache.commons.lang3.ArrayUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class OriginalPug3Test {

  private static String[] ignoredCases =
      new String[] {

        // try to read files in ../
        // unsupported
        "filters.include", // unsupported filters
        "filters.stylus", // missing filter
        "filters.less", // missing filter
        "filters.nested", // missing filter
        "filter-in-include", // missing less filter
        "pipeless-filters", // maybe missing markdown-it or different markdown syntax as in js
        // markdown
        "code.iteration", // function block not working in buffered code. Maybe report to GraalVM
        // Bugtracker.
        "filters.coffeescript", // missing filter
        "blocks-in-if" // blocks in buffered code not recognozed. Should be fixable.
      };

  private static String[] casesWithoutLinebreak = new String[] {"filters.nested"};

  private static final IntegrationTestSetup testSetup;

  static {
    try {
      testSetup =
          new IntegrationTestSetup(
              TestFileHelper.getOriginalPug3ResourcePath(""),
              "cases",
              "pug",
              new GraalJsExpressionHandler());
    } catch (FileNotFoundException | URISyntaxException e) {
      throw new RuntimeException("Failed to initialize test setup", e);
    }
  }

  private String file;

  public OriginalPug3Test(String file) {
    this.file = file;
  }

  @Test
  public void shouldCompileJadeToHtml() throws Exception {
    HashMap<String, Object> model = new HashMap<String, Object>();
    model.put("title", "Pug");

    String actual = testSetup.getActualHtml(file, model);
    String expected = testSetup.getExpectedHtml(file);

    if (ArrayUtils.contains(casesWithoutLinebreak, file.replace(".pug", ""))) {
      actual = actual.replaceAll("\\n| ", "");
      expected = expected.replaceAll("\\n| ", "");
    }

    assertEquals(file, expected, actual);
  }

  @Parameterized.Parameters(name = "{0}")
  public static Collection<String[]> data() throws FileNotFoundException, URISyntaxException {
    String resourcePath = TestFileHelper.getOriginalPug3ResourcePath("/cases");
    String extension = "pug";
    return IntegrationTestSetup.createTestFileData(resourcePath, extension, ignoredCases);
  }
}
