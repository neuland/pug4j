package de.neuland.pug4j;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import de.neuland.pug4j.template.FileTemplateLoader;
import de.neuland.pug4j.template.PugTemplate;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Verifies that the deprecated {@link PugConfiguration} API and the new {@link PugEngine}/{@link
 * RenderContext} API produce identical output for the same template and model — including
 * templates without a doctype, where the configured mode decides terse/xml rendering.
 */
@SuppressWarnings({"deprecation", "removal"})
public class ApiEquivalenceTest {

  @Rule public TemporaryFolder folder = new TemporaryFolder();

  private Path templateDir;

  @Before
  public void setUp() throws IOException {
    templateDir = folder.getRoot().toPath();
    Files.writeString(
        templateDir.resolve("no-doctype.pug"),
        "div\n  input(type=\"checkbox\" checked=true)\n  p= name\n");
  }

  private Map<String, Object> model() {
    Map<String, Object> model = new HashMap<>();
    model.put("name", "pug4j");
    return model;
  }

  @Test
  public void oldAndNewApiRenderIdenticallyInHtmlMode() throws Exception {
    // Old API defaults to Mode.HTML; the new API defaults to Mode.XHTML (pug.js behavior),
    // so HTML mode is set explicitly on the RenderContext here.
    PugConfiguration config = new PugConfiguration();
    config.setTemplateLoader(new FileTemplateLoader(templateDir));
    PugTemplate oldTemplate = config.getTemplate("no-doctype.pug");
    String oldOutput = config.renderTemplate(oldTemplate, model());

    PugEngine engine = PugEngine.forPath(templateDir);
    RenderContext context = RenderContext.builder().defaultMode(Pug4J.Mode.HTML).build();
    PugTemplate newTemplate = engine.getTemplate("no-doctype.pug");
    String newOutput = engine.render(newTemplate, model(), context);

    assertEquals(oldOutput, newOutput);
    assertTrue(
        "HTML mode should render terse boolean attributes: " + newOutput,
        newOutput.contains("checked>"));
  }

  @Test
  public void newApiDefaultsToPugJsBehaviorWithoutDoctype() throws Exception {
    PugEngine engine = PugEngine.forPath(templateDir);
    PugTemplate template = engine.getTemplate("no-doctype.pug");
    String output = engine.render(template, model());

    assertTrue(
        "without a doctype, void tags should self-close like pug.js: " + output,
        output.contains("/>"));
  }

  @Test
  public void oldAndNewApiRenderIdenticallyInXhtmlMode() throws Exception {
    PugConfiguration config = new PugConfiguration();
    config.setTemplateLoader(new FileTemplateLoader(templateDir));
    config.setMode(Pug4J.Mode.XHTML);
    PugTemplate oldTemplate = config.getTemplate("no-doctype.pug");
    String oldOutput = config.renderTemplate(oldTemplate, model());

    PugEngine engine = PugEngine.forPath(templateDir);
    RenderContext context = RenderContext.builder().defaultMode(Pug4J.Mode.XHTML).build();
    PugTemplate newTemplate = engine.getTemplate("no-doctype.pug");
    String newOutput = engine.render(newTemplate, model(), context);

    assertEquals(oldOutput, newOutput);
    assertTrue(
        "XHTML mode should self-close void tags: " + newOutput, newOutput.contains("/>"));
  }

  @Test
  public void expressionCacheSizeSurvivesEngineCreation() throws Exception {
    PugConfiguration config = new PugConfiguration();
    config.setTemplateLoader(new FileTemplateLoader(templateDir));
    config.setExpressionCacheSize(123);
    PugTemplate template = config.getTemplate("no-doctype.pug");
    config.renderTemplate(template, model()); // triggers getOrCreateEngine()
    assertEquals(123, config.getExpressionCacheSize());
  }
}
