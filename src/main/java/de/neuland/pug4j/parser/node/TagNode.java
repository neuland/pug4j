package de.neuland.pug4j.parser.node;

import de.neuland.pug4j.PugConfiguration;
import de.neuland.pug4j.compiler.IndentWriter;
import de.neuland.pug4j.compiler.NodeVisitor;
import de.neuland.pug4j.exceptions.ExpressionException;
import de.neuland.pug4j.exceptions.PugCompilerException;
import de.neuland.pug4j.model.PugModel;

import java.util.LinkedList;

import org.apache.commons.lang3.ArrayUtils;

public class TagNode extends AttrsNode {
    private static final String[] selfClosingTags = {"area", "base", "br", "col", "embed", "hr", "img", "input", "keygen", "link", "menuitem", "meta", "param", "source", "track", "wbr"};
    private static final String[] inlineTags = {"a", "abbr", "acronym", "b", "br", "code", "em", "font", "i", "img", "ins", "kbd", "map", "samp", "small", "span", "strong", "sub", "sup"};
    private static final String[] whitespaceSensitiveTags = {"pre", "textarea"};
    private boolean interpolated = false;

    public TagNode() {
        this.block = new BlockNode();
    }

    public boolean isInline() {
        return ArrayUtils.indexOf(inlineTags, this.name) > -1;
    }

    public boolean isWhitespaceSensitive() {
        return ArrayUtils.indexOf(whitespaceSensitiveTags, this.name) > -1;
    }

    private boolean isInline(Node node) {
        // Recurse if the node is a block
        if (node instanceof BlockNode && !((BlockNode) node).isYield() && !((BlockNode) node).isNamedBlock()) {
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
        return (isTextNode(node) && (node.getValue() == null || !node.getValue().contains("\n"))) || inline;
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

    public String bufferName(PugConfiguration configuration, PugModel model) {
        if (isInterpolated()) {
            try {
                return configuration.getExpressionHandler().evaluateStringExpression(name, model);
            } catch (ExpressionException e) {
                throw new PugCompilerException(this, configuration.getTemplateLoader(), e);
            }
        } else {
            return name;
        }
    }

    public boolean isSelfClosingTag() {
        return ArrayUtils.contains(selfClosingTags, name);
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
