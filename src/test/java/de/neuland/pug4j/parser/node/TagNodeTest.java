package de.neuland.pug4j.parser.node;

import static org.junit.Assert.assertEquals;

import de.neuland.pug4j.Pug4J;
import de.neuland.pug4j.PugEngine;
import de.neuland.pug4j.UnitTestSetup;
import de.neuland.pug4j.template.PugTemplate;
import java.util.Collections;
import java.util.LinkedList;
import org.junit.Before;
import org.junit.Test;

public class TagNodeTest {

  private static final String TEXT = "dummytext";

  private String[] bodylessTags = {
    "meta", "img", "link", "input", "area", "base", "col", "br", "hr", "source"
  };
  private String[] normalTags = {"div", "table", "span"};
  private PugEngine engine;

  @Before
  public void init() {
    engine = UnitTestSetup.createEngine();
  }

  @Test
  public void shouldCloseBodylessTagsWithSlashAndIgnoreBlockWhenCompilingToXhtml() {

    for (String tagName : bodylessTags) {
      TagNode tagNode = new TagNode();
      tagNode.setName(tagName);
      withTextBlock(tagNode);
      PugTemplate template = new PugTemplate(tagNode, Pug4J.Mode.XHTML);

      String result = engine.render(template, Collections.<String, Object>emptyMap());

      assertEquals("<" + tagName + "/>", result);
    }
  }

  @Test
  public void shouldCloseBodylessTagsWithoutSlashAndIgnoreBlockWhenCompilingToHtml() {

    for (String tagName : bodylessTags) {
      TagNode tagNode = new TagNode();
      tagNode.setName(tagName);
      withTextBlock(tagNode);
      PugTemplate template = new PugTemplate(tagNode, Pug4J.Mode.HTML);

      String result = engine.render(template, Collections.<String, Object>emptyMap());

      assertEquals("<" + tagName + ">", result);
    }
  }

  @Test
  public void shouldCloseBodylessTagsWithEndTagWhenCompilingToXml() {
    for (String tagName : bodylessTags) {
      TagNode tagNode = new TagNode();
      tagNode.setName(tagName);
      withTextBlock(tagNode);
      PugTemplate template = new PugTemplate(tagNode, Pug4J.Mode.XML);

      String result = engine.render(template, Collections.<String, Object>emptyMap());

      assertEquals("<" + tagName + ">" + TEXT + "</" + tagName + ">", result);
    }
  }

  @Test
  public void shouldCloseSelfClosingBodylessTagsWithSlashAndIgnoreBlockWhenCompilingToXhtml() {

    for (String tagName : bodylessTags) {
      TagNode tagNode = new TagNode();
      tagNode.setName(tagName);
      tagNode.setSelfClosing(true);
      withTextBlock(tagNode);
      PugTemplate template = new PugTemplate(tagNode, Pug4J.Mode.XHTML);

      String result = engine.render(template, Collections.<String, Object>emptyMap());

      assertEquals("<" + tagName + "/>", result);
    }
  }

  @Test
  public void shouldCloseSelfClosingBodylessTagsWithSlashAndIgnoreBlockWhenCompilingToHtml() {
    for (String tagName : bodylessTags) {
      TagNode tagNode = new TagNode();
      tagNode.setName(tagName);
      tagNode.setSelfClosing(true);
      withTextBlock(tagNode);
      PugTemplate template = new PugTemplate(tagNode, Pug4J.Mode.HTML);

      String result = engine.render(template, Collections.<String, Object>emptyMap());

      assertEquals("<" + tagName + "/>", result);
    }
  }

  @Test
  public void shouldCloseSelfClosingBodylessTagsWithSlashAndIgnoreBlockWhenCompilingToXml() {
    for (String tagName : bodylessTags) {
      TagNode tagNode = new TagNode();
      tagNode.setName(tagName);
      tagNode.setSelfClosing(true);
      withTextBlock(tagNode);
      PugTemplate template = new PugTemplate(tagNode, Pug4J.Mode.XML);

      String result = engine.render(template, Collections.<String, Object>emptyMap());

      assertEquals("<" + tagName + "/>", result);
    }
  }

  @Test
  public void shouldCloseNormalTagsWithEndTagWhenCompilingToXhtml() {
    for (String tagName : normalTags) {
      TagNode tagNode = new TagNode();
      tagNode.setName(tagName);
      withTextBlock(tagNode);
      PugTemplate template = new PugTemplate(tagNode, Pug4J.Mode.XHTML);

      String result = engine.render(template, Collections.<String, Object>emptyMap());

      assertEquals("<" + tagName + ">" + TEXT + "</" + tagName + ">", result);
    }
  }

  @Test
  public void shouldCloseNormalTagsWithEndTagWhenCompilingToHtml() {
    for (String tagName : normalTags) {
      TagNode tagNode = new TagNode();
      tagNode.setName(tagName);
      withTextBlock(tagNode);
      PugTemplate template = new PugTemplate(tagNode, Pug4J.Mode.HTML);

      String result = engine.render(template, Collections.<String, Object>emptyMap());

      assertEquals("<" + tagName + ">" + TEXT + "</" + tagName + ">", result);
    }
  }

  @Test
  public void shouldCloseNormalTagsWithEndTagWhenCompilingToXml() {
    for (String tagName : normalTags) {
      TagNode tagNode = new TagNode();
      tagNode.setName(tagName);
      withTextBlock(tagNode);
      PugTemplate template = new PugTemplate(tagNode, Pug4J.Mode.XML);

      String result = engine.render(template, Collections.<String, Object>emptyMap());

      assertEquals("<" + tagName + ">" + TEXT + "</" + tagName + ">", result);
    }
  }

  @Test
  public void shouldCloseSelfClosingNormalTagsWithSlashAndIgnoreBlockWhenCompilingToXhtml() {
    for (String tagName : normalTags) {
      TagNode tagNode = new TagNode();
      tagNode.setName(tagName);
      tagNode.setSelfClosing(true);
      withTextBlock(tagNode);
      PugTemplate template = new PugTemplate(tagNode, Pug4J.Mode.XHTML);

      String result = engine.render(template, Collections.<String, Object>emptyMap());

      assertEquals("<" + tagName + "/>", result);
    }
  }

  @Test
  public void shouldCloseSelfClosingNormalTagsWithSlashAndIgnoreBlockWhenCompilingToHtml() {
    for (String tagName : normalTags) {
      TagNode tagNode = new TagNode();
      tagNode.setName(tagName);
      tagNode.setSelfClosing(true);
      withTextBlock(tagNode);
      PugTemplate template = new PugTemplate(tagNode, Pug4J.Mode.HTML);

      String result = engine.render(template, Collections.<String, Object>emptyMap());

      assertEquals("<" + tagName + "/>", result);
    }
  }

  @Test
  public void shouldCloseSelfClosingNormalTagsWithSlashAndIgnoreBlockWhenCompilingToXml() {
    for (String tagName : normalTags) {
      TagNode tagNode = new TagNode();
      tagNode.setName(tagName);
      tagNode.setSelfClosing(true);
      withTextBlock(tagNode);
      PugTemplate template = new PugTemplate(tagNode, Pug4J.Mode.XML);

      String result = engine.render(template, Collections.<String, Object>emptyMap());

      assertEquals("<" + tagName + "/>", result);
    }
  }

  private void withTextBlock(TagNode tagNode) {
    TextNode textNode = new TextNode();
    textNode.setValue(TEXT);

    BlockNode blockNode = new BlockNode();

    LinkedList<Node> list = new LinkedList<Node>();
    list.add(textNode);
    blockNode.setNodes(list);

    tagNode.setBlock(blockNode);
  }
}
