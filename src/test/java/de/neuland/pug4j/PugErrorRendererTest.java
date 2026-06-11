package de.neuland.pug4j;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import de.neuland.pug4j.exceptions.PugException;
import de.neuland.pug4j.exceptions.PugLexerException;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import org.apache.commons.io.FileUtils;
import org.junit.Test;

public class PugErrorRendererTest {

  @Test
  public void rendersSameErrorPageAsDeprecatedToHtmlString() throws Exception {
    String errorJade = TestFileHelper.getCompilerResourcePath("exceptions/error.jade");
    String exceptionHtml = TestFileHelper.getCompilerResourcePath("exceptions/error.html");
    try {
      Pug4J.render(errorJade, new HashMap<String, Object>());
      fail();
    } catch (PugException e) {
      String expectedHtml = readFile(exceptionHtml);
      String html = PugErrorRenderer.renderHtml(e, "<html><head><title>broken");
      assertEquals(
          removeAbsolutePath(expectedHtml.replaceAll("\r", "")),
          removeAbsolutePath(html.replaceAll("\r", "")));
    }
  }

  @Test
  public void rendersWithoutGeneratedHtml() throws Exception {
    String errorJade = TestFileHelper.getCompilerResourcePath("exceptions/error.jade");
    try {
      Pug4J.render(errorJade, new HashMap<String, Object>());
      fail();
    } catch (PugException e) {
      String html = PugErrorRenderer.renderHtml(e);
      assertNotNull(html);
      assertTrue(html.contains("Pug Compiler Exception"));
      assertTrue(html.contains("error.jade"));
      assertTrue("source context should be shown", html.contains("non.existing.query()"));
    }
  }

  @Test
  public void rendersLeanExceptionWithoutTemplateLines() {
    PugException e =
        new PugLexerException("something broke", "index.pug", 3, 1, Arrays.<String>asList());

    String html = PugErrorRenderer.renderHtml(e);

    assertNotNull(html);
    assertTrue(html.contains("Pug Lexer Exception"));
    assertTrue(html.contains("something broke"));
  }

  private String removeAbsolutePath(String html) {
    html =
        html.replaceAll("evaluateExpression@\\d+:\\d+", "evaluateExpression@0:0");
    html =
        html.replaceAll(
            "(<h2>In ).*(compiler/exceptions/error\\.jade at line 9, column 0\\.</h2>)",
            "$1\\.\\./compiler/exceptions/error\\.jade at line 9, column 0\\.</h2>");
    html =
        html.replaceAll(
            "(\\s)[^\\s]*(compiler/exceptions/error\\.jade:9)",
            "$1\\.\\./compiler/exceptions/error\\.jade:9");
    html =
        html.replaceAll(
            "(<h2>In ).*(compiler\\\\exceptions\\\\error\\.jade at line 9, column 0\\.</h2>)",
            "$1\\.\\./compiler/exceptions/error\\.jade at line 9, column 0\\.</h2>");
    html =
        html.replaceAll(
            "(\\s)[^\\s]*(compiler\\\\exceptions\\\\error\\.jade:9)",
            "$1\\.\\./compiler/exceptions/error\\.jade:9");
    return html;
  }

  private String readFile(String fileName) {
    try {
      return FileUtils.readFileToString(new File(fileName), "UTF-8");
    } catch (IOException e) {
      e.printStackTrace();
    }
    return "";
  }
}
