package de.neuland.pug4j.template;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import de.neuland.pug4j.PugEngine;
import de.neuland.pug4j.RenderContext;
import de.neuland.pug4j.TestFileHelper;
import de.neuland.pug4j.UnitTestSetup;
import de.neuland.pug4j.exceptions.PugCompilerException;
import de.neuland.pug4j.helper.beans.IterableMap;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;

public class JadeRunFullTemplateTest {

  private PugEngine engine;

  @Before
  public void setUp() throws Exception {
    engine = UnitTestSetup.createEngine(getResourcePath(""), "jade");
  }

  @Test
  public void testFullRun() throws IOException {

    Map<String, Object> root = new HashMap<String, Object>();
    root.put("hello", "world");
    root.put("hallo", null);

    PugTemplate temp = engine.getTemplate("fullrun");

    StringWriter out = new StringWriter();
    try {
      engine.render(temp, root, RenderContext.defaults(), out);
    } catch (PugCompilerException e) {
      e.printStackTrace();
      fail();
    }
    out.flush();
    assertEquals("<div><div>Hi everybody</div></div>", out.toString());
  }

  @Test
  public void testEachLoopWithIterableMap() throws Exception {

    IterableMap users = new IterableMap();
    users.put("bob", "Robert Smith");
    users.put("alex", "Alex Supertramp");

    Map<String, Object> root = new HashMap<String, Object>();
    root.put("users", users);

    PugTemplate temp = engine.getTemplate("each_loop");

    StringWriter out = new StringWriter();
    try {
      engine.render(temp, root, RenderContext.defaults(), out);
    } catch (PugCompilerException e) {
      e.printStackTrace();
      fail();
    }
    out.flush();
    assertEquals("<ul><li>Robert Smith</li><li>Alex Supertramp</li></ul>", out.toString());
  }

  public String getResourcePath(String fileName) throws URISyntaxException {
    try {
      return TestFileHelper.getRootResourcePath() + "/template/" + fileName;
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
    return null;
  }
}
