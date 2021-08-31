package de.neuland.pug4j.parser.node;

import de.neuland.pug4j.compiler.IndentWriter;
import de.neuland.pug4j.exceptions.ExpressionException;
import de.neuland.pug4j.exceptions.PugCompilerException;
import de.neuland.pug4j.model.PugModel;
import de.neuland.pug4j.template.PugTemplate;
import java.util.LinkedList;
import org.apache.commons.lang3.ArrayUtils;

public class TagNode extends AttrsNode {
    private Node textNode;
    private static final String[] inlineTags = {"a", "abbr", "acronym", "b", "br", "code", "em", "font", "i", "img", "ins", "kbd", "map", "samp", "small", "span", "strong", "sub", "sup"};
    private static final String[] whitespaceSensitiveTags = {"pre","textarea"};
    private boolean interpolated = false;

    public TagNode() {
        this.block = new BlockNode();
    }

    public void setTextNode(Node textNode) {
        this.textNode = textNode;
    }

    public Node getTextNode() {
        return textNode;
    }

    public boolean hasTextNode() {
        return textNode != null;
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
        if (node instanceof FilterNode && node.hasBlock() && node.getBlock().getNodes().size()>0 ) {
            return everyIsInline(node.getBlock().getNodes());
        }
        boolean inline = false;
        if(node instanceof ExpressionNode){
            inline = ((ExpressionNode) node).isInline();
        }
        if(node instanceof TagNode){
            inline = ((TagNode) node).isInline();
        }
        return (isTextNode(node) && (node.getValue()==null || !node.getValue().contains("\n"))) || inline;
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

    @Override
    public void execute(IndentWriter writer, PugModel model, PugTemplate template) throws PugCompilerException {
        writer.increment();


        if (isWhitespaceSensitive()) {
            writer.setEscape(true);
        }

        if (!writer.isCompiledTag()) {
            if (!writer.isCompiledDoctype() && "html".equals(name)) {
// TODO:             template.setDoctype(null);
            }
            writer.setCompiledTag(true);
        }

        // pretty print
        if (writer.isPp() && !isInline()) {
            writer.prettyIndent(0, true);
        }

        if (isSelfClosing() || (!template.isXml() && isSelfClosingTag())) {
            openTag(writer, model, template, !(template.isTerse() && !isSelfClosing()));
            // TODO: if it is non-empty throw an error
//            if (tag.code ||
//                    tag.block &&
//                            !(tag.block.type === 'Block' && tag.block.nodes.length === 0) &&
//                            tag.block.nodes.some(function (tag) {
//                return tag.type !== 'Text' || !/^\s*$/.test(tag.val)
//            })) {
//                this.error(name + ' is a self closing element: <'+name+'/> but contains nested content.', 'SELF_CLOSING_CONTENT', tag);
//            }
        } else {
            openTag(writer, model, template, false);

            if (hasCodeNode()) {
                codeNode.execute(writer, model, template);
            }
            if (hasBlock()) {
                block.execute(writer, model, template);
            }
            if (writer.isPp() && !isInline() && !isWhitespaceSensitive() && !canInline()) {
                writer.prettyIndent(0, true);
            }
            writer.append("</");
            writer.append(bufferName(template, model));
            writer.append(">");
        }

        if (isWhitespaceSensitive()) {
            writer.setEscape(false);
        }
        writer.decrement();
    }

    private void openTag(IndentWriter writer, PugModel model, PugTemplate template, boolean selfClosing) {
        writer.append("<")
            .append(bufferName(template, model))
            .append(visitAttributes(model, template));

        if (selfClosing) {
            writer.append("/");
        }
        writer.append(">");
    }

    private void handleIgnoredBlock() {
        // TODO Fehlerbehandlung
    }


    private String bufferName(PugTemplate template, PugModel model) {
        if (isInterpolated()) {
            try {
                return template.getExpressionHandler().evaluateStringExpression(name, model);
            } catch (ExpressionException e) {
                e.printStackTrace();
                return null;
            }
        } else {
            return name;
        }
    }

    public boolean isInterpolated() {
        return interpolated;
    }

    public void setInterpolated(boolean interpolated) {
        this.interpolated = interpolated;
    }
}
