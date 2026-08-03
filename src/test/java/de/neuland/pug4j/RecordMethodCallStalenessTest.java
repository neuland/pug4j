package de.neuland.pug4j;

import static org.junit.Assert.assertEquals;

import de.neuland.pug4j.expression.ExpressionHandler;
import de.neuland.pug4j.expression.GraalJsExpressionHandler;
import de.neuland.pug4j.expression.JexlExpressionHandler;
import de.neuland.pug4j.template.PugTemplate;
import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;

/**
 * Regression tests for record accessor method calls being memoised per compiled template: JEXL
 * caches resolved JexlMethods on the parsed script's AST nodes, so the method wrappers created by
 * RecordWrapperUberspect must invoke against the receiver passed per evaluation, never against a
 * receiver captured at resolution time. Before the fix, the first receiver's value was reused for
 * every later iteration of an each loop and for every later render of the same engine.
 */
public class RecordMethodCallStalenessTest {

  record NavItem(String key, String label) {}

  record Flagged(boolean value) {}

  public static class PlainItem {
    private final String key;

    public PlainItem(String key) {
      this.key = key;
    }

    public String key() {
      return key;
    }
  }

  private PugEngine newJexlEngine(String pug) {
    return UnitTestSetup.createEngineFromReader(new StringReader(pug), "inline");
  }

  private PugEngine newGraalEngine(String pug) {
    return UnitTestSetup.createEngineFromReader(
        new StringReader(pug), "inline", new GraalJsExpressionHandler());
  }

  private static List<NavItem> navItems() {
    return List.of(
        new NavItem("overview", "Overview"),
        new NavItem("profile", "My Profile"),
        new NavItem("orders", "My Orders"));
  }

  private static final String EACH_METHOD_AND_PROPERTY_PUG =
      """
      ul
        each item, i in items
          li #{i} #{item.key} #{item.key()}
      """;

  private static final String EACH_EXPECTED =
      "<ul>"
          + "<li>0 overview overview</li>"
          + "<li>1 profile profile</li>"
          + "<li>2 orders orders</li>"
          + "</ul>";

  private void assertEachRendersDistinctElements(PugEngine engine) throws Exception {
    PugTemplate template = engine.getTemplate("inline");
    Map<String, Object> model = new HashMap<>();
    model.put("items", navItems());
    assertEquals(EACH_EXPECTED, engine.render(template, model));
  }

  @Test
  public void jexlEachEvaluatesRecordMethodPerIteration() throws Exception {
    assertEachRendersDistinctElements(newJexlEngine(EACH_METHOD_AND_PROPERTY_PUG));
  }

  @Test
  public void graalEachEvaluatesRecordMethodPerIteration() throws Exception {
    assertEachRendersDistinctElements(newGraalEngine(EACH_METHOD_AND_PROPERTY_PUG));
  }

  private static final String TWO_LOOPS_PUG =
      """
      ul
        each item in items
          li= item.key()
      ol
        each item in reversedItems
          li= item.key()
      """;

  @Test
  public void jexlSecondEachOverReversedListSeesItsOwnElements() throws Exception {
    PugEngine engine = newJexlEngine(TWO_LOOPS_PUG);
    PugTemplate template = engine.getTemplate("inline");
    Map<String, Object> model = new HashMap<>();
    model.put("items", navItems());
    List<NavItem> reversed = new java.util.ArrayList<>(navItems());
    java.util.Collections.reverse(reversed);
    model.put("reversedItems", reversed);
    assertEquals(
        "<ul><li>overview</li><li>profile</li><li>orders</li></ul>"
            + "<ol><li>orders</li><li>profile</li><li>overview</li></ol>",
        engine.render(template, model));
  }

  private void assertSecondRenderSeesNewRecord(ExpressionHandler handler) throws Exception {
    PugEngine engine =
        UnitTestSetup.createEngineFromReader(new StringReader("p= flag.value()"), "inline", handler);
    PugTemplate template = engine.getTemplate("inline");

    Map<String, Object> model = new HashMap<>();
    model.put("flag", new Flagged(true));
    assertEquals("<p>true</p>", engine.render(template, model));

    model = new HashMap<>();
    model.put("flag", new Flagged(false));
    assertEquals("<p>false</p>", engine.render(template, model));
  }

  @Test
  public void jexlSecondRenderEvaluatesRecordMethodAgainstNewRecord() throws Exception {
    assertSecondRenderSeesNewRecord(new JexlExpressionHandler());
  }

  @Test
  public void graalSecondRenderEvaluatesRecordMethodAgainstNewRecord() throws Exception {
    assertSecondRenderSeesNewRecord(new GraalJsExpressionHandler());
  }

  private static final String PLAIN_CLASS_EACH_PUG =
      """
      ul
        each item, i in items
          li #{i} #{item.key()}
      """;

  @Test
  public void jexlResolvesComponentOnRecordInsideCollection() throws Exception {
    // records reached without passing through the model (e.g. list index access) must resolve
    // components too - with boundary wrapping this rendered empty
    PugEngine engine = newJexlEngine("p= items[0].key\np= items[1].key()");
    PugTemplate template = engine.getTemplate("inline");
    Map<String, Object> model = new HashMap<>();
    model.put("items", navItems());
    assertEquals("<p>overview</p><p>profile</p>", engine.render(template, model));
  }

  @Test
  public void jexlPlainClassMethodCallInEachStaysCorrect() throws Exception {
    PugEngine engine = newJexlEngine(PLAIN_CLASS_EACH_PUG);
    PugTemplate template = engine.getTemplate("inline");
    Map<String, Object> model = new HashMap<>();
    model.put(
        "items", List.of(new PlainItem("overview"), new PlainItem("profile"), new PlainItem("orders")));
    assertEquals(
        "<ul><li>0 overview</li><li>1 profile</li><li>2 orders</li></ul>",
        engine.render(template, model));
  }
}
