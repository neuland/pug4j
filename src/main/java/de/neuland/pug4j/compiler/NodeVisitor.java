package de.neuland.pug4j.compiler;

import de.neuland.pug4j.model.PugModel;
import de.neuland.pug4j.parser.node.*;

/**
 * Visitor interface for processing different node types during compilation. This follows the Gang
 * of Four Visitor pattern to avoid instanceof chains.
 */
public interface NodeVisitor {
  void visit(TagNode node, IndentWriter writer, PugModel model);

  void visit(BlockNode node, IndentWriter writer, PugModel model);

  void visit(BlockCommentNode node, IndentWriter writer, PugModel model);

  void visit(MixinNode node, IndentWriter writer, PugModel model);

  void visit(CallNode node, IndentWriter writer, PugModel model);

  void visit(CaseNode node, IndentWriter writer, PugModel model);

  void visit(CaseNode.When node, IndentWriter writer, PugModel model);

  void visit(CommentNode node, IndentWriter writer, PugModel model);

  void visit(ConditionalNode node, IndentWriter writer, PugModel model);

  void visit(DoctypeNode node, IndentWriter writer, PugModel model);

  void visit(EachNode node, IndentWriter writer, PugModel model);

  void visit(ExpressionNode node, IndentWriter writer, PugModel model);

  void visit(FilterNode node, IndentWriter writer, PugModel model);

  void visit(IfConditionNode node, IndentWriter writer, PugModel model);

  void visit(IncludeFilterNode node, IndentWriter writer, PugModel model);

  void visit(LiteralNode node, IndentWriter writer, PugModel model);

  void visit(MixinBlockNode node, IndentWriter writer, PugModel model);

  void visit(TextNode node, IndentWriter writer, PugModel model);

  void visit(WhileNode node, IndentWriter writer, PugModel model);
}
