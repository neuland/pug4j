package de.neuland.pug4j.parser;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import de.neuland.pug4j.parser.node.BlockNode;
import de.neuland.pug4j.parser.node.Node;
import java.net.URISyntaxException;
import org.junit.Test;

public class DoctypeParserTest extends ParserTest {

  private BlockNode blockNode;

  @Test
  public void shouldReturnDoctype() throws URISyntaxException {
    loadInParser("doctype.jade");
    blockNode = (BlockNode) root;
    assertThat(blockNode.getNodes(), notNullValue());

    Node node = blockNode.pollNode();
    assertThat(node.getValue(), equalTo("strict"));
    assertThat(blockNode.hasNodes(), equalTo(false));
  }
}
