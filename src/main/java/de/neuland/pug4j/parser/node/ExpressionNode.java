package de.neuland.pug4j.parser.node;

import de.neuland.pug4j.compiler.IndentWriter;
import de.neuland.pug4j.compiler.NodeVisitor;
import de.neuland.pug4j.model.PugModel;
import java.util.UUID;

public class ExpressionNode extends Node {

  private boolean escape;
  private boolean buffer;
  private boolean inline;
  private String nodeId;

  public ExpressionNode() {
    super();
    nodeId = createNodeId();
  }

  public void setEscape(boolean escape) {
    this.escape = escape;
  }

  public boolean isEscape() {
    return escape;
  }

  public void setBuffer(boolean buffer) {
    this.buffer = buffer;
  }

  public boolean isBuffer() {
    return buffer;
  }

  public String getNodeId() {
    return nodeId;
  }

  public boolean isInline() {
    return inline;
  }

  public void setInline(boolean inline) {
    this.inline = inline;
  }

  private String createNodeId() {
    return UUID.randomUUID().toString().replace("-", "");
  }

  @Override
  public void setValue(String value) {
    super.setValue(value.trim());
  }

  @Override
  public void accept(NodeVisitor visitor, IndentWriter writer, PugModel model) {
    visitor.visit(this, writer, model);
  }
}
