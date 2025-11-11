package de.neuland.pug4j.parser.node;

import de.neuland.pug4j.compiler.IndentWriter;
import de.neuland.pug4j.compiler.NodeVisitor;
import de.neuland.pug4j.model.PugModel;

public class IfConditionNode extends Node {

  private boolean defaultNode = false;
  private boolean isInverse = false;

  public IfConditionNode(String condition, int lineNumber) {
    this.value = condition;
    this.lineNumber = lineNumber;
  }

  public void setDefault(boolean defaultNode) {
    this.defaultNode = defaultNode;
  }

  public boolean isDefault() {
    return defaultNode;
  }

  public boolean isInverse() {
    return isInverse;
  }

  public void setInverse(boolean isInverse) {
    this.isInverse = isInverse;
  }

  @Override
  public void accept(NodeVisitor visitor, IndentWriter writer, PugModel model) {
    visitor.visit(this, writer, model);
  }
}
