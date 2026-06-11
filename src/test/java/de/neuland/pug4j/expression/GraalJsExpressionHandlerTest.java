package de.neuland.pug4j.expression;

import static org.junit.Assert.*;

import de.neuland.pug4j.exceptions.ExpressionException;
import de.neuland.pug4j.model.PugModel;
import de.neuland.pug4j.parser.node.BlockNode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;

public class GraalJsExpressionHandlerTest {

  private GraalJsExpressionHandler graalJsExpressionHandler;
  private PugModel pugModel;

  @Before
  public void setUp() throws Exception {
    graalJsExpressionHandler = new GraalJsExpressionHandler();
    pugModel = new PugModel(new HashMap<>());
  }

  @Test
  public void testArrayList() throws ExpressionException {
    List<Object> addedHostList = Arrays.asList(1, 2, 3, 4);
    pugModel.put("hostList", addedHostList);
    graalJsExpressionHandler.evaluateExpression("var list = [1,2,3]", pugModel);
    List<Object> list = (List<Object>) pugModel.get("list");
    List<Object> hostList = (List) pugModel.get("hostList");
    Integer expected = 1;
    assertEquals(expected, list.get(0));
    assertEquals(expected, hostList.get(0));
  }

  @Test
  public void testNull() throws ExpressionException {
    graalJsExpressionHandler.evaluateExpression("var list;", pugModel);
    Object list = pugModel.get("list");
    assertNull(list);
  }

  @Test
  public void testMap() throws ExpressionException {
    pugModel.put("hostBlock", new BlockNode());
    graalJsExpressionHandler.evaluateExpression(
        "var map = {'foo':'bar',typenull:null,typeboolean:true,typebyte:10,typeshort:30000,typeint:64000,typelong:4000000000,typefloat:1.5,typedouble:1.1234567890123123123123123123,typehost:hostBlock,typelist:[1,2,3],typemap:{key:'value'},typedate:new Date()}",
        pugModel);
    Map map = (Map) pugModel.get("map");
    assertEquals("bar", map.get("foo"));
    assertTrue(map.get("foo") instanceof String);
    assertTrue(map.get("typenull") == null);
    assertTrue(map.get("typeboolean") instanceof Boolean);
    assertTrue(map.get("typebyte") instanceof Integer);
    assertTrue(map.get("typeshort") instanceof Integer);
    assertTrue(map.get("typeint") instanceof Integer);
    assertTrue(map.get("typelong") instanceof Long);
    assertTrue(map.get("typefloat") instanceof Float);
    assertTrue(map.get("typedouble") instanceof Double);
    assertTrue(map.get("typelist") instanceof List);
    assertTrue(map.get("typedate") instanceof Instant);
    assertTrue(map.get("typehost") instanceof BlockNode);
    assertTrue(map.get("typemap") instanceof Map);
  }

  @Test
  public void testReturnMap() throws ExpressionException {
    pugModel.put("hostBlock", new BlockNode());
    Map map =
        (Map)
            graalJsExpressionHandler.evaluateExpression(
                "{'foo':'bar',typenull:null,typeboolean:true,typebyte:10,typeshort:30000,typeint:64000,typelong:4000000000,typefloat:1.5,typedouble:1.1234567890123123123123123123,typehost:hostBlock,typelist:[1,2,3],typemap:{key:'value'},typedate:new Date()}",
                pugModel);
    assertEquals("bar", map.get("foo"));
    assertTrue(map.get("foo") instanceof String);
    assertTrue(map.get("typenull") == null);
    assertTrue(map.get("typeboolean") instanceof Boolean);
    assertTrue(map.get("typebyte") instanceof Integer);
    assertTrue(map.get("typeshort") instanceof Integer);
    assertTrue(map.get("typeint") instanceof Integer);
    assertTrue(map.get("typelong") instanceof Long);
    assertTrue(map.get("typefloat") instanceof Float);
    assertTrue(map.get("typedouble") instanceof Double);
    assertTrue(map.get("typelist") instanceof List);
    assertTrue(map.get("typedate") instanceof Instant);
    assertTrue(map.get("typehost") instanceof BlockNode);
    assertTrue(map.get("typemap") instanceof Map);
  }

  @Test
  public void testMapMulti() throws ExpressionException {
    graalJsExpressionHandler.evaluateExpression(
        "var map = {"
            + "  'text': 'text',"
            + "  'image': 'image.jpg',"
            + "  'button' : {"
            + "  'text': 'textbutton'"
            + "  },"
            + "  \"list\": [1,2,3,4]"
            + "}",
        pugModel);
    Map map = (Map) pugModel.get("map");
    assertEquals("textbutton", ((Map) map.get("button")).get("text"));
    Integer expected = 3;
    assertEquals(expected, ((List) map.get("list")).get(2));
  }

  @Test
  public void testReturn() throws ExpressionException {
    Object value =
        graalJsExpressionHandler.evaluateExpression(
            "{"
                + "  'text': 'text',"
                + "  'image': 'image.jpg',"
                + "  'button' : {"
                + "  'text': 'textbutton'"
                + "  },"
                + "  \"list\": [1,2,3,4]"
                + "}",
            pugModel);

    assertEquals("textbutton", ((Map) ((Map) value).get("button")).get("text"));
  }

  @Test
  public void testInt() throws ExpressionException {
    graalJsExpressionHandler.evaluateExpression("var count = 5", pugModel);
    Object count = pugModel.get("count");
    assertEquals("5", count.toString());
  }

  @Test
  public void testDoubleModel() throws ExpressionException {
    pugModel.put("one", Double.valueOf(1.0));
    graalJsExpressionHandler.evaluateExpression("var count = one", pugModel);
    Object count = pugModel.get("count");
    assertEquals("1", count.toString());
  }

  @Test
  public void testDouble() throws ExpressionException {
    graalJsExpressionHandler.evaluateExpression("var price = 5.50", pugModel);
    Object price = (Object) pugModel.get("price");
    // assertEquals(5.5,price,0.0001);
    assertEquals("5.5", price.toString());
  }

  @Test
  public void testDouble2() throws ExpressionException {
    graalJsExpressionHandler.evaluateExpression("var price = 5.00", pugModel);
    Object price = (Object) pugModel.get("price");
    assertEquals("5", price.toString());
  }

  @Test
  public void testString() throws ExpressionException {
    graalJsExpressionHandler.evaluateExpression("var moin = 'Hallo Welt!'", pugModel);
    String moin = (String) pugModel.get("moin");
    assertEquals("Hallo Welt!", moin);
  }

  @Test
  public void testBoolean() throws ExpressionException {
    graalJsExpressionHandler.evaluateExpression("var what = true", pugModel);
    boolean what = (boolean) pugModel.get("what");
    assertTrue(what);
  }

  @Test
  public void testArray() throws ExpressionException {
    List o = (List) graalJsExpressionHandler.evaluateExpression("([])", pugModel);
    assertTrue(o.isEmpty());
  }

  @Test
  public void testArraySubLists() throws ExpressionException {
    List o = (List) graalJsExpressionHandler.evaluateExpression("([[1,2],[3,4]])", pugModel);
    assertTrue(o.get(0) instanceof List);
  }

  @Test
  public void testLetTopLevel() throws ExpressionException {
    graalJsExpressionHandler.evaluateExpression("let x = 5", pugModel);
    assertEquals("5", pugModel.get("x").toString());
  }

  @Test
  public void testLetTopLevelEvaluatedTwice() throws ExpressionException {
    graalJsExpressionHandler.evaluateExpression("let x = 5", pugModel);
    graalJsExpressionHandler.evaluateExpression("let x = 5", pugModel);
    assertEquals("5", pugModel.get("x").toString());
  }

  @Test
  public void testConstTopLevel() throws ExpressionException {
    graalJsExpressionHandler.evaluateExpression("const y = [1,2]", pugModel);
    List y = (List) pugModel.get("y");
    assertEquals(2, y.size());
  }

  @Test
  public void testConstTopLevelEvaluatedTwice() throws ExpressionException {
    graalJsExpressionHandler.evaluateExpression("const y = [1,2]", pugModel);
    graalJsExpressionHandler.evaluateExpression("const y = [1,2]", pugModel);
    List y = (List) pugModel.get("y");
    assertEquals(2, y.size());
  }

  @Test
  public void testDeclarationWithTripleEqualsInitializer() throws ExpressionException {
    pugModel.put("a", true);
    graalJsExpressionHandler.evaluateExpression("let isTrue = a === true", pugModel);
    assertEquals(Boolean.TRUE, pugModel.get("isTrue"));
  }

  @Test
  public void testArrowFunctionInitializer() throws ExpressionException {
    graalJsExpressionHandler.evaluateExpression("var f = (n) => n + 1", pugModel);
    assertNotNull(pugModel.get("f"));
    Object result = graalJsExpressionHandler.evaluateExpression("f(2)", pugModel);
    assertEquals("3", result.toString());
  }

  @Test
  public void testMultipleDeclarators() throws ExpressionException {
    graalJsExpressionHandler.evaluateExpression("var a = 1, b = 2", pugModel);
    assertEquals("1", pugModel.get("a").toString());
    assertEquals("2", pugModel.get("b").toString());
  }

  @Test
  public void testVarNoInitializerThenRead() throws ExpressionException {
    graalJsExpressionHandler.evaluateExpression("var list;", pugModel);
    Object result = graalJsExpressionHandler.evaluateExpression("list", pugModel);
    assertNull(result);
  }

  @Test
  public void testObjectDestructuringDeclaration() throws ExpressionException {
    HashMap<String, Object> obj = new HashMap<>();
    obj.put("a", 1);
    obj.put("b", 2);
    pugModel.put("obj", obj);
    graalJsExpressionHandler.evaluateExpression("var {a, b} = obj", pugModel);
    Object a = graalJsExpressionHandler.evaluateExpression("a", pugModel);
    assertEquals("1", a.toString());
    assertEquals("1", pugModel.get("a").toString());
    assertEquals("2", pugModel.get("b").toString());
  }

  @Test
  public void testDollarIdentifier() throws ExpressionException {
    graalJsExpressionHandler.evaluateExpression("var $x = 1", pugModel);
    assertEquals("1", pugModel.get("$x").toString());
  }

  @Test
  public void testKeywordInsideStringLiteralUntouched() throws ExpressionException {
    graalJsExpressionHandler.evaluateExpression("var s = \"let it be\"", pugModel);
    assertEquals("let it be", pugModel.get("s"));
  }

  @Test
  public void testNestedBlockCallbackLetDeclaration() throws ExpressionException {
    List<Object> captured = new ArrayList<>();
    Runnable blockRenderer =
        () -> {
          try {
            graalJsExpressionHandler.evaluateExpression("let d = item * 2", pugModel);
            captured.add(graalJsExpressionHandler.evaluateExpression("d", pugModel));
          } catch (ExpressionException e) {
            throw new RuntimeException(e);
          }
        };
    pugModel.put(
        "pug4j__runnable_test",
        graalJsExpressionHandler.createBlockCallback(blockRenderer, pugModel));
    graalJsExpressionHandler.evaluateExpression(
        "[1,2,3].forEach(function(item){"
            + graalJsExpressionHandler.getBlockInvocation("pug4j__runnable_test")
            + "})",
        pugModel);
    assertEquals(3, captured.size());
    assertEquals("2", captured.get(0).toString());
    assertEquals("4", captured.get(1).toString());
    assertEquals("6", captured.get(2).toString());
  }

  @Test
  public void testNestedBlockCallbackResolvesFunctionLocals() throws ExpressionException {
    List<Object> captured = new ArrayList<>();
    Runnable blockRenderer =
        () -> {
          try {
            captured.add(graalJsExpressionHandler.evaluateExpression("item", pugModel));
          } catch (ExpressionException e) {
            throw new RuntimeException(e);
          }
        };
    pugModel.put(
        "pug4j__runnable_test",
        graalJsExpressionHandler.createBlockCallback(blockRenderer, pugModel));
    graalJsExpressionHandler.evaluateExpression(
        "[1,2,3].forEach(function(item){"
            + graalJsExpressionHandler.getBlockInvocation("pug4j__runnable_test")
            + "})",
        pugModel);
    assertEquals(Arrays.asList(1, 2, 3), captured);
  }

  @Test
  public void testNestedBlockCallbackUnknownIdentifierReturnsNull() throws ExpressionException {
    List<Object> captured = new ArrayList<>();
    Runnable blockRenderer =
        () -> {
          try {
            captured.add(graalJsExpressionHandler.evaluateExpression("doesNotExist", pugModel));
          } catch (ExpressionException e) {
            throw new RuntimeException(e);
          }
        };
    pugModel.put(
        "pug4j__runnable_test",
        graalJsExpressionHandler.createBlockCallback(blockRenderer, pugModel));
    graalJsExpressionHandler.evaluateExpression(
        "[1].forEach(function(item){"
            + graalJsExpressionHandler.getBlockInvocation("pug4j__runnable_test")
            + "})",
        pugModel);
    assertEquals(1, captured.size());
    assertNull(captured.get(0));
  }

  @Test
  public void testResolverStackEmptyAfterThrowingBlockRenderer() {
    Runnable blockRenderer =
        () -> {
          throw new IllegalStateException("boom");
        };
    pugModel.put(
        "pug4j__runnable_test",
        graalJsExpressionHandler.createBlockCallback(blockRenderer, pugModel));
    try {
      graalJsExpressionHandler.evaluateExpression(
          "[1].forEach(function(item){"
              + graalJsExpressionHandler.getBlockInvocation("pug4j__runnable_test")
              + "})",
          pugModel);
      fail("expected exception");
    } catch (IllegalStateException e) {
      assertEquals("boom", e.getMessage());
    } catch (ExpressionException e) {
      fail("host exception should not be wrapped as ExpressionException");
    }
    assertTrue(graalJsExpressionHandler.resolverStack.get().isEmpty());
  }

  @Test
  public void testArrayAccess() throws ExpressionException {
    HashMap<String, Object> product = new HashMap<>();
    List images = new ArrayList();
    images.add("Image 1");
    images.add("Image 2");
    product.put("images", images);
    pugModel.put("product", product);
    Object o = graalJsExpressionHandler.evaluateExpression("(product.images[0])", pugModel);
    assertEquals("Image 1", o);
    assertEquals("Image 1", ((List) ((Map) pugModel.get("product")).get("images")).get(0));
  }
}
