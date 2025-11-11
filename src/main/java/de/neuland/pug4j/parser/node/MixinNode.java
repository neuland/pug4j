package de.neuland.pug4j.parser.node;

import de.neuland.pug4j.compiler.IndentWriter;
import de.neuland.pug4j.compiler.NodeVisitor;
import de.neuland.pug4j.model.PugModel;
import java.util.HashMap;

public class MixinNode extends CallNode {

  private String rest;
  private HashMap<String, String> defaultValues = new HashMap<>();

  public void setRest(String rest) {
    this.rest = rest;
  }

  public String getRest() {
    return rest;
  }

  public HashMap<String, String> getDefaultValues() {
    return defaultValues;
  }

  public void setDefaultValues(final HashMap<String, String> defaultValues) {
    this.defaultValues = defaultValues;
  }

  @Override
  public void accept(NodeVisitor visitor, IndentWriter writer, PugModel model) {
    visitor.visit(this, writer, model);
  }
}
