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
  //    @Test
  //    @Ignore
  //    public void testBlockNodeAccess() throws ExpressionException  {
  //        IndentWriter writer = new IndentWriter(new StringWriter());
  //        BlockNode blockNode = new BlockNode();
  //        LinkedList<Node> nodes = new LinkedList();
  //        TextNode textNode = new TextNode();
  //        textNode.setValue("Hallo Welt");
  //        nodes.add(textNode);
  //        blockNode.setNodes(nodes);
  //        pugModel.put("pug4j__block", blockNode);
  //        pugModel.put("pug4j__writer", writer);
  //        pugModel.put("pug4j__template", new PugTemplate());
  //        pugModel.put("pug4j__model", new PugModel(new HashMap<>()));
  //        Object o =
  // graalJsExpressionHandler.evaluateExpression("pug4j__block.execute(pug4j__writer,pug4j__model,pug4j__template)", pugModel);
  //        assertEquals("Hallo Welt",writer.toString());
  //    }
  //    @Test
  //    @Ignore
  //    public void testContextInContextAccess() throws ExpressionException  {
  //        IndentWriter writer = new IndentWriter(new StringWriter());
  //        BlockNode blockNode = new BlockNode();
  //        LinkedList<Node> nodes = new LinkedList();
  //        ExpressionNode textNode = new ExpressionNode();
  //        textNode.setBuffer(true);
  //        textNode.setValue("item");
  //        nodes.add(textNode);
  //        blockNode.setNodes(nodes);
  //        pugModel.put("pug4j__block", blockNode);
  //        pugModel.put("pug4j__writer", writer);
  //        PugTemplate pugTemplate = new PugTemplate();
  //        pugModel.put("pug4j__template", pugTemplate);
  //        pugModel.put("pug4j__model", new PugModel(new HashMap<>()));
  //        pugModel.put("pug4j__context", graalJsExpressionHandler.getContext());
  //        //Object o = graalJsExpressionHandler.evaluateExpression("var items =
  // ['1','2','3'];items.forEach(function(item){pug4j__block.execute(pug4j__writer,pug4j__model,pug4j__template)})", pugModel);
  //        Object o = graalJsExpressionHandler.evaluateExpression("var items =
  // ['1','2','3'];for(i=0;i<3;i++){var item =
  // items[i];pug4j__block.execute(pug4j__writer,pug4j__model,pug4j__template)}", pugModel);
  //        assertEquals("123",writer.toString());
  //    }

}
