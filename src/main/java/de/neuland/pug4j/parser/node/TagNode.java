package de.neuland.pug4j.parser.node;

import de.neuland.pug4j.PugConfiguration;
import de.neuland.pug4j.compiler.IndentWriter;
import de.neuland.pug4j.compiler.NodeVisitor;
import de.neuland.pug4j.exceptions.ExpressionException;
import de.neuland.pug4j.exceptions.PugCompilerException;
import de.neuland.pug4j.exceptions.TemplateSource;
import de.neuland.pug4j.expression.ExpressionHandler;
import de.neuland.pug4j.model.PugModel;
import de.neuland.pug4j.template.TemplateLoader;
import java.util.LinkedList;
import java.util.Set;

public class TagNode extends AttrsNode {
  private static final Set<String> selfClosingTags =
      Set.of(
          "area",
          "base",
          "br",
          "col",
          "embed",
          "hr",
          "img",
          "input",
          "keygen",
          "link",
          "menuitem",
          "meta",
          "param",
          "source",
          "track",
          "wbr");
  private static final Set<String> inlineTags =
      Set.of(
          "a", "abbr", "acronym", "b", "br", "code", "em", "font", "i", "img", "ins", "kbd", "map",
          "samp", "small", "span", "strong", "sub", "sup");
  private static final Set<String> whitespaceSensitiveTags = Set.of("pre", "textarea");
  private boolean interpolated = false;

  public TagNode() {
    this.block = new BlockNode();
  }

  public boolean isInline() {
    return name != null && inlineTags.contains(name);
  }

  public boolean isWhitespaceSensitive() {
    return name != null && whitespaceSensitiveTags.contains(name);
  }

  private boolean isInline(Node node) {
    // Recurse if the node is a block
    if (node instanceof BlockNode
        && !((BlockNode) node).isYield()
        && !((BlockNode) node).isNamedBlock()) {
      return everyIsInline(node.getNodes());
    }
    if (node instanceof BlockNode && ((BlockNode) node).isYield()) {
      return true;
    }
    if (node instanceof BlockNode && ((BlockNode) node).isNamedBlock()) {
      return false;
    }
    if (node instanceof FilterNode && node.hasBlock() && !node.getBlock().getNodes().isEmpty()) {
      return everyIsInline(node.getBlock().getNodes());
    }
    boolean inline = false;
    if (node instanceof ExpressionNode) {
      inline = ((ExpressionNode) node).isInline();
    }
    if (node instanceof TagNode) {
      inline = ((TagNode) node).isInline();
    }
    return (isTextNode(node) && (node.getValue() == null || !node.getValue().contains("\n")))
        || inline;
  }

  private boolean everyIsInline(LinkedList<Node> nodes) {
    boolean multilineInlineOnlyTag = true;
    for (Node node : nodes) {
      if (!isInline(node)) {
        multilineInlineOnlyTag = false;
      }
    }
    return multilineInlineOnlyTag;
  }

  public boolean canInline() {
    Node block = this.getBlock();
    if (block == null) {
      return true;
    }
    LinkedList<Node> nodes = block.getNodes();
    return everyIsInline(nodes);
  }

  public String bufferName(
      ExpressionHandler expressionHandler, TemplateLoader templateLoader, PugModel model) {
    if (isInterpolated()) {
      try {
        return expressionHandler.evaluateStringExpression(name, model);
      } catch (ExpressionException e) {
        throw new PugCompilerException(this, TemplateSource.readLines(templateLoader, getFileName()), e);
      }
    } else {
      return name;
    }
  }

  /**
   * @deprecated Use bufferName(ExpressionHandler, TemplateLoader, PugModel) instead
   */
  @Deprecated
  public String bufferName(PugConfiguration configuration, PugModel model) {
    return bufferName(
        configuration.getExpressionHandler(), configuration.getTemplateLoader(), model);
  }

  public boolean isSelfClosingTag() {
    return name != null && selfClosingTags.contains(name);
  }

  public boolean isInterpolated() {
    return interpolated;
  }

  public void setInterpolated(boolean interpolated) {
    this.interpolated = interpolated;
  }

  @Override
  public void accept(NodeVisitor visitor, IndentWriter writer, PugModel model) {
    visitor.visit(this, writer, model);
  }
}
