package de.neuland.pug4j.integration;

import static org.junit.Assert.assertEquals;

import de.neuland.pug4j.IntegrationTestSetup;
import de.neuland.pug4j.TestFileHelper;
import de.neuland.pug4j.expression.GraalJsExpressionHandler;
import java.io.FileNotFoundException;
import java.net.URISyntaxException;
import java.util.HashMap;
import org.junit.Test;

/**
 * Renders templates twice on the same thread. The GraalVM context is thread-local and persistent,
 * so state leaking from the first render (e.g. global lexical bindings from top-level let/const)
 * only shows up on the second render — the parameterized suites render each case exactly once and
 * never catch this.
 */
public class GraalJsRepeatedRenderTest {

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

  @Test
  public void shouldRenderSameTemplateTwiceOnSameThread() throws Exception {
    String[] files = {"equals.pug", "let-const.pug", "buffered-code-function-block.pug"};
    for (String file : files) {
      String expected = testSetup.getExpectedHtml(file);
      assertEquals(file + " (first render)", expected, testSetup.getActualHtml(file, new HashMap<>()));
      assertEquals(
          file + " (second render)", expected, testSetup.getActualHtml(file, new HashMap<>()));
    }
  }
}
