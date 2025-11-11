package de.neuland.pug4j.integration;

import static org.junit.Assert.assertEquals;

import de.neuland.pug4j.ParameterizedTestCaseHelper;
import de.neuland.pug4j.PugConfiguration;
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
public class Pug4JIntegrationTest {

  private static String[] ignoredCases = new String[] {"include-with-filter"};

  private String file;

  public Pug4JIntegrationTest(String file) {
    this.file = file;
  }

  @Test
  public void shouldCompilePugToHtml() throws Exception {
    String fileTemplateLoaderPath = TestFileHelper.getPug4JTestsResourcePath("");
    String basePath = "cases";
    String extension = "pug";
    JexlExpressionHandler expressionHandler = new JexlExpressionHandler();
    PugConfiguration pug = new PugConfiguration();
    pug.setExpressionHandler(expressionHandler);
    ParameterizedTestCaseHelper testHelper =
        new ParameterizedTestCaseHelper(
            fileTemplateLoaderPath, basePath, extension, expressionHandler);
    PugConfiguration pugConfiguration = testHelper.getPugConfiguration();

    HashMap<String, Object> model = new HashMap<String, Object>();
    model.put("title", "Pug");

    String actual = testHelper.getActualHtml(file, pugConfiguration, model);
    String expected = testHelper.getExpectedHtml(file);

    assertEquals(file, expected, actual);
  }

  @Parameterized.Parameters(name = "{0}")
  public static Collection<String[]> data() throws FileNotFoundException, URISyntaxException {
    String resourcePath = TestFileHelper.getPug4JTestsResourcePath("/cases");
    String extension = "pug";
    return ParameterizedTestCaseHelper.createTestFileData(resourcePath, extension, ignoredCases);
  }
}
