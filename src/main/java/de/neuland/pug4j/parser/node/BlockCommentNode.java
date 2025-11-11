package de.neuland.pug4j.parser.node;

import de.neuland.pug4j.compiler.IndentWriter;
import de.neuland.pug4j.compiler.NodeVisitor;
import de.neuland.pug4j.model.PugModel;

public class BlockCommentNode extends Node {
  private boolean buffered;

  public boolean isBuffered() {
    return buffered;
  }

  public void setBuffered(boolean buffered) {
    this.buffered = buffered;
  }

  @Override
  public void accept(NodeVisitor visitor, IndentWriter writer, PugModel model) {
    visitor.visit(this, writer, model);
  }
}
