package de.neuland.pug4j.parser.node;

import de.neuland.pug4j.compiler.IndentWriter;
import de.neuland.pug4j.compiler.NodeVisitor;
import de.neuland.pug4j.model.PugModel;

public class BlockNode extends Node {

  private boolean yield = false;
  private String mode;
  private boolean namedBlock;

  public void setYield(boolean yield) {
    this.yield = yield;
  }

  public boolean isYield() {
    return yield;
  }

  public BlockNode getYieldBlock() {
    BlockNode ret = this;
    for (Node node : getNodes()) {
      if (node instanceof BlockNode && ((BlockNode) node).isYield()) {
        return (BlockNode) node;
      } else if (node instanceof TagNode && ((TagNode) node).isTextOnly()) {
        continue;
      } else if (node instanceof BlockNode && ((BlockNode) node).getYieldBlock() != null) {
        ret = ((BlockNode) node).getYieldBlock();
      } else if (node.hasBlock()) {
        ret = ((BlockNode) node.getBlock()).getYieldBlock();
      }
      if (ret instanceof BlockNode && ((BlockNode) ret).isYield()) {
        return ret;
      }
    }
    return ret;
  }

  public String getMode() {
    return mode;
  }

  public void setMode(String mode) {
    this.mode = mode;
  }

  public boolean isNamedBlock() {
    return namedBlock;
  }

  public void setNamedBlock(boolean namedBlock) {
    this.namedBlock = namedBlock;
  }

  @Override
  public void accept(NodeVisitor visitor, IndentWriter writer, PugModel model) {
    visitor.visit(this, writer, model);
  }
}
