package de.neuland.pug4j.integration;

import static org.junit.Assert.assertEquals;

import de.neuland.pug4j.PugEngine;
import de.neuland.pug4j.RenderContext;
import de.neuland.pug4j.TestFileHelper;
import de.neuland.pug4j.expression.ExpressionHandler;
import de.neuland.pug4j.expression.GraalJsExpressionHandler;
import de.neuland.pug4j.expression.JexlExpressionHandler;
import de.neuland.pug4j.filter.CDATAFilter;
import de.neuland.pug4j.filter.PlainFilter;
import de.neuland.pug4j.template.FileTemplateLoader;
import de.neuland.pug4j.template.PugTemplate;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.junit.Assume;
import org.junit.Test;

/**
 * Renders a single kitchen-sink template that exercises the full pug feature set (inheritance,
 * mixins, iteration, conditionals, filters, attribute styles, interpolation, includes) with
 * model-driven expressions limited to the JEXL/GraalJS common subset.
 *
 * <p>Serves two purposes: a regression safety net asserting both expression handlers produce the
 * exact expected HTML, and a realistic workload for performance comparisons (opt-in via {@code
 * -Dpug4j.kitchensink.perf=true}).
 */
public class KitchenSinkIntegrationTest {

  private static final String TEMPLATE = "kitchen-sink";

  private PugEngine engine(ExpressionHandler expressionHandler) throws Exception {
    FileTemplateLoader loader =
        new FileTemplateLoader(Path.of(TestFileHelper.getResourcePath("/kitchensink")), "pug");
    return PugEngine.builder()
        .templateLoader(loader)
        .expressionHandler(expressionHandler)
        .filter("plain", new PlainFilter())
        .filter("cdata", new CDATAFilter())
        .build();
  }

  /** Model deliberately uses nested maps and lists, the structures both handlers must agree on. */
  private Map<String, Object> model() {
    Map<String, Object> user = new LinkedHashMap<>();
    user.put("id", 42);
    user.put("name", "Alice");
    user.put("age", 35);
    user.put("admin", true);
    user.put("tags", Arrays.asList("vip", "beta-tester"));

    List<Map<String, Object>> products = new ArrayList<>();
    products.add(product("Keyboard", 49, true, Arrays.asList("mechanical", "rgb")));
    products.add(product("Mouse", 25, false, Arrays.asList("wireless")));
    products.add(product("Monitor", 199, true, Arrays.asList("27 inch", "4k", "hdr")));

    Map<String, Object> userProperties = new LinkedHashMap<>();
    userProperties.put("plan", "premium");
    userProperties.put("since", "2020");

    Map<String, Object> model = new HashMap<>();
    model.put("pageTitle", "Kitchen Sink");
    model.put("user", user);
    model.put("products", products);
    model.put("productCount", products.size());
    model.put("userProperties", userProperties);
    model.put("emptyList", new ArrayList<>());
    model.put("htmlSnippet", "<em>raw emphasis</em>");
    return model;
  }

  private static Map<String, Object> product(
      String name, int price, boolean inStock, List<String> features) {
    Map<String, Object> product = new LinkedHashMap<>();
    product.put("name", name);
    product.put("price", price);
    product.put("inStock", inStock);
    product.put("features", features);
    return product;
  }

  private String render(ExpressionHandler expressionHandler) throws Exception {
    PugEngine engine = engine(expressionHandler);
    PugTemplate template = engine.getTemplate(TEMPLATE);
    return engine.render(template, model(), RenderContext.defaults());
  }

  private String expected() throws Exception {
    File file = new File(TestFileHelper.getResourcePath("/kitchensink/kitchen-sink.html"));
    // Windows checkouts may carry CRLF; rendered output is always LF (the scanner normalizes)
    return FileUtils.readFileToString(file, StandardCharsets.UTF_8).replace("\r", "");
  }

  @Test
  public void jexlRendersExpectedHtml() throws Exception {
    assertEquals(expected(), render(new JexlExpressionHandler()));
  }

  @Test
  public void graalJsRendersExpectedHtml() throws Exception {
    assertEquals(expected(), render(new GraalJsExpressionHandler()));
  }

  @Test
  public void bothHandlersProduceIdenticalOutput() throws Exception {
    assertEquals(render(new JexlExpressionHandler()), render(new GraalJsExpressionHandler()));
  }

  /**
   * Opt-in snapshot regeneration after intentional output changes: {@code mvn test
   * -Dtest=KitchenSinkIntegrationTest#regenerateExpectedHtml
   * -Dpug4j.kitchensink.regenerate=true}. Only writes if both handlers agree; review the diff
   * before committing.
   */
  @Test
  public void regenerateExpectedHtml() throws Exception {
    Assume.assumeTrue(Boolean.getBoolean("pug4j.kitchensink.regenerate"));
    String jexl = render(new JexlExpressionHandler());
    assertEquals("handlers must agree before snapshotting", jexl, render(new GraalJsExpressionHandler()));
    File file =
        new File("src/test/resources/kitchensink/kitchen-sink.html").getAbsoluteFile();
    FileUtils.writeStringToFile(file, jexl, StandardCharsets.UTF_8);
    System.out.println("KITCHENSINK snapshot written: " + file);
  }

  /**
   * Opt-in performance smoke run: {@code mvn test -Dtest=KitchenSinkIntegrationTest
   * -Dpug4j.kitchensink.perf=true}. Prints per-handler timings; not asserted, JMH-grade rigor is
   * not the goal here.
   */
  @Test
  public void perfSmoke() throws Exception {
    Assume.assumeTrue(Boolean.getBoolean("pug4j.kitchensink.perf"));
    int warmup = 200;
    int iterations = 1000;
    for (ExpressionHandler handler :
        new ExpressionHandler[] {new JexlExpressionHandler(), new GraalJsExpressionHandler()}) {
      PugEngine engine = engine(handler);
      PugTemplate template = engine.getTemplate(TEMPLATE);
      Map<String, Object> model = model();
      RenderContext context = RenderContext.defaults();
      for (int i = 0; i < warmup; i++) {
        engine.render(template, model, context);
      }
      long start = System.nanoTime();
      for (int i = 0; i < iterations; i++) {
        engine.render(template, model, context);
      }
      long ms = (System.nanoTime() - start) / 1_000_000;
      System.out.println(
          "KITCHENSINK-PERF " + handler.getClass().getSimpleName() + " " + iterations
              + " renders: " + ms + " ms");
    }
  }
}
