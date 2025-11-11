package de.neuland.pug4j;

import de.neuland.pug4j.template.FileTemplateLoader;
import de.neuland.pug4j.template.PugTemplate;
import org.junit.Test;

import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Tests for the new PugEngine API (3.0.0)
 */
public class PugEngineTest {

    private static final String RESOURCES_PATH = "src/test/resources/";

    @Test
    public void testEngineWithBuilderAPI() throws Exception {
        // Create engine with builder
        PugEngine engine = PugEngine.builder()
                .templateLoader(
                        FileTemplateLoader.builder()
                                .templateLoaderPath(Paths.get(RESOURCES_PATH))
                                .build()
                )
                .caching(true)
                .build();

        assertNotNull("Engine should not be null", engine);
        assertTrue("Caching should be enabled", engine.isCaching());
    }

    @Test
    public void testRenderWithContext() throws Exception {
        // Setup
        PugEngine engine = PugEngine.forPath(RESOURCES_PATH + "compiler");

        RenderContext context = RenderContext.builder()
                .prettyPrint(false)
                .defaultMode(Pug4J.Mode.HTML)
                .globalVariable("title", "Test Title")
                .build();

        Map<String, Object> model = new HashMap<>();
        model.put("title", "Hello");

        // Get template
        PugTemplate template = engine.getTemplate("layout");

        // Render
        String html = engine.render(template, model, context);

        assertNotNull("Rendered HTML should not be null", html);
        assertFalse("HTML should not be empty", html.trim().isEmpty());
    }

    @Test
    public void testRenderWithDefaultContext() throws Exception {
        PugEngine engine = PugEngine.forPath(RESOURCES_PATH + "compiler");

        Map<String, Object> model = new HashMap<>();
        model.put("title", "Test");

        PugTemplate template = engine.getTemplate("layout");
        String html = engine.render(template, model);

        assertNotNull("Rendered HTML should not be null", html);
    }

    @Test
    public void testRenderContextBuilder() {
        RenderContext context = RenderContext.builder()
                .prettyPrint(true)
                .defaultMode(Pug4J.Mode.XML)
                .globalVariable("var1", "value1")
                .globalVariable("var2", 42)
                .build();

        assertTrue("Pretty print should be enabled", context.isPrettyPrint());
        assertEquals("Default mode should be XML", Pug4J.Mode.XML, context.getDefaultMode());
        assertEquals("Global variable should be set", "value1", context.getGlobalVariables().get("var1"));
        assertEquals("Global variable should be set", 42, context.getGlobalVariables().get("var2"));
    }

    @Test
    public void testRenderContextDefaults() {
        RenderContext context = RenderContext.defaults();

        assertFalse("Pretty print should be disabled by default", context.isPrettyPrint());
        assertEquals("Default mode should be HTML", Pug4J.Mode.HTML, context.getDefaultMode());
        assertTrue("Global variables should be empty", context.getGlobalVariables().isEmpty());
    }

    @Test
    public void testTemplateLoaderBuilder() {
        FileTemplateLoader loader = FileTemplateLoader.builder()
                .templateLoaderPath(RESOURCES_PATH)
                .basePath("pages")
                .extension("jade")
                .build();

        assertNotNull("Loader should not be null", loader);
        assertEquals("Base path should be set", "pages/", loader.getBase());
        assertEquals("Extension should be set", "jade", loader.getExtension());
    }

    @Test
    public void testEngineHasFilter() {
        PugEngine engine = PugEngine.builder().build();

        assertTrue("Engine should have default CDATA filter", engine.hasFilter("cdata"));
        assertTrue("Engine should have default CSS filter", engine.hasFilter("css"));
        assertTrue("Engine should have default JS filter", engine.hasFilter("js"));
        assertFalse("Engine should not have unknown filter", engine.hasFilter("unknown"));
    }

    @Test
    public void testTemplateExists() {
        PugEngine engine = PugEngine.forPath(RESOURCES_PATH + "compiler");

        assertTrue("Template 'layout' should exist", engine.templateExists("layout"));
        assertFalse("Template 'nonexistent' should not exist", engine.templateExists("nonexistent"));
    }
}
