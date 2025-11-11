package de.neuland.pug4j.integration;

import static org.junit.Assert.assertEquals;

import de.neuland.pug4j.IntegrationTestSetup;
import de.neuland.pug4j.TestFileHelper;
import de.neuland.pug4j.expression.GraalJsExpressionHandler;
import java.io.*;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.HashMap;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class Pug4JGraalVMIntegrationTest {

  private static String[] ignoredCases = new String[] {"include-with-filter"};
  private static final IntegrationTestSetup testSetup;

  static {
    try {
      testSetup =
          new IntegrationTestSetup(
              TestFileHelper.getPug4JGraalVMTestsResourcePath(""),
              "cases",
              "pug",
              new GraalJsExpressionHandler());
    } catch (FileNotFoundException | URISyntaxException e) {
      throw new RuntimeException("Failed to initialize test setup", e);
    }
  }

  private String file;

  public Pug4JGraalVMIntegrationTest(String file) {
    this.file = file;
  }

  @Test
  public void shouldCompilePugToHtml() throws Exception {
    HashMap<String, Object> model = new HashMap<String, Object>();
    model.put("title", "Pug");

    String actual = testSetup.getActualHtml(file, model);
    String expected = testSetup.getExpectedHtml(file);

    assertEquals(file, expected, actual);
  }

  @Parameterized.Parameters(name = "{0}")
  public static Collection<String[]> data() throws FileNotFoundException, URISyntaxException {
    String resourcePath = TestFileHelper.getPug4JGraalVMTestsResourcePath("/cases");
    String extension = "pug";
    return IntegrationTestSetup.createTestFileData(resourcePath, extension, ignoredCases);
  }
}
