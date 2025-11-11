package de.neuland.pug4j.compiler;

import java.io.StringWriter;
import java.io.Writer;
import java.util.*;
import java.util.function.Consumer;

import de.neuland.pug4j.RenderContext;
import de.neuland.pug4j.exceptions.ExpressionException;
import de.neuland.pug4j.exceptions.PugCompilerException;
import de.neuland.pug4j.expression.ExpressionHandler;
import de.neuland.pug4j.filter.Filter;
import de.neuland.pug4j.model.PugModel;
import de.neuland.pug4j.parser.node.*;
import de.neuland.pug4j.template.PugTemplate;
import de.neuland.pug4j.template.TemplateLoader;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;

import static de.neuland.pug4j.model.PugModel.PUG4J_MODEL_PREFIX;

public class Compiler implements NodeVisitor {

    private final PugTemplate template;
    private final RenderContext context;
    private final de.neuland.pug4j.PugEngine engine;
    private String bufferedExpressionString = "";
    private final AttributesCompiler attributesCompiler;

    public Compiler(PugTemplate pugTemplate, RenderContext context, de.neuland.pug4j.PugEngine engine) {
        this.template = pugTemplate;
        this.context = context;
        this.engine = engine;
        this.attributesCompiler = new AttributesCompiler(engine);
    }

    private ExpressionHandler getExpressionHandler() {
        return engine.getExpressionHandler();
    }

    private TemplateLoader getTemplateLoader() {
        return engine.getTemplateLoader();
    }

    public String compileToString(PugModel model) throws PugCompilerException {
        StringWriter writer = new StringWriter();
        compile(model, writer);
        return writer.toString();
    }

    public void compile(PugModel model, Writer w) throws PugCompilerException {
        IndentWriter writer = new IndentWriter(w);
        writer.setUseIndent(context.isPrettyPrint());
        visit(writer, model, template.getRootNode());

    }

    private void visit(final IndentWriter writer, final PugModel model, final Node node) {
        node.accept(this, writer, model);
    }

    @Override
    public void visit(CaseNode.When node, IndentWriter writer, PugModel model) {
        visit(writer, model, node.getBlock());
    }

    @Override
    public void visit(TagNode node, IndentWriter writer, PugModel model) {
        writer.increment();

        if (node.isWhitespaceSensitive()) {
            writer.setEscape(true);
        }

        // pretty print
        if (writer.isPp() && !node.isInline()) {
            writer.prettyIndent(0, true);
        }

        final String tagName = node.bufferName(getExpressionHandler(), getTemplateLoader(), model);
        final boolean terse = template.isTerse();
        final String tagAttributes = attributesCompiler.visitAttributes(model, node, terse);

        if (node.isSelfClosing() || (!template.isXml() && node.isSelfClosingTag())) {
            boolean selfClosing = !(template.isTerse() && !node.isSelfClosing());
            writer.append("<")
                    .append(tagName)
                    .append(tagAttributes);

            if (selfClosing) {
                writer.append("/");
            }
            writer.append(">");
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
            writer.append("<")
                    .append(tagName)
                    .append(tagAttributes)
                    .append(">");

            if (node.hasBlock()) {
                visit(writer, model, node.getBlock());
            }
            if (writer.isPp() && !node.isInline() && !node.isWhitespaceSensitive() && !node.canInline()) {
                writer.prettyIndent(0, true);
            }
            writer.append("</")
                    .append(tagName)
                    .append(">");
        }

        if (node.isWhitespaceSensitive()) {
            writer.setEscape(false);
        }
        writer.decrement();
    }

    @Override
    public void visit(BlockNode node, IndentWriter writer, PugModel model) {
        // Pretty print multi-line text
        if (writer.isPp() && node.getNodes().size() > 1 && !writer.isEscape() && node.isTextNode(node.getNodes().get(0)) && node.isTextNode(node.getNodes().get(1)))
            writer.prettyIndent(1, true);
        bufferedExpressionString = "";
        for (int i = 0; i < node.getNodes().size(); ++i) {
            // Pretty print text
            Node childNode = node.getNodes().get(i);
            if (writer.isPp() && i > 0 && !writer.isEscape() && node.isTextNode(childNode) && node.isTextNode(node.getNodes().get(i - 1)) && (node.getNodes().get(i - 1).getValue() != null && node.getNodes().get(i - 1).getValue().contains("\n")))
                writer.prettyIndent(1, false);

            visit(writer, model, childNode);

            Node nextChildNode = null;
            if (i + 1 < node.getNodes().size())
                nextChildNode = node.getNodes().get(i + 1);

            //If multiple expressions in a row evaluate buffered code
            if (bufferedExpressionString.isEmpty() || (nextChildNode != null && (nextChildNode instanceof ExpressionNode && (nextChildNode.hasBlock() || nextChildNode.getValue().trim().startsWith("}"))))) {
                continue;
            }

            try {
                getExpressionHandler().evaluateExpression(bufferedExpressionString, model);
            } catch (ExpressionException e) {
                throw new PugCompilerException(node, getTemplateLoader(), e);
            }
            bufferedExpressionString = "";
        }
    }

    @Override
    public void visit(BlockCommentNode node, IndentWriter writer, PugModel model) {
        if (!node.isBuffered()) {
            return;
        }
        if (writer.isPp()) {
            writer.prettyIndent(1, true);
        }
        writer.append("<!--" + node.getValue());
        visit(writer, model, node.getBlock());
        if (writer.isPp()) {
            writer.prettyIndent(1, true);
        }
        writer.append("-->");
    }

    @Override
    public void visit(CallNode node, IndentWriter writer, PugModel model) {
        boolean dynamic = node.getName().charAt(0) == '#';

        String newname = (dynamic ? node.getName().substring(2, node.getName().length() - 1) : '"' + node.getName() + '"');
        try {
            newname = (String) getExpressionHandler().evaluateExpression(newname, model);
        } catch (ExpressionException e) {
            throw new PugCompilerException(node, getTemplateLoader(), e);
        }

        MixinNode mixin;
        final String mixinname = dynamic ? newname : node.getName();
        mixin = model.getMixin(mixinname);

        if (mixin == null) {
            throw new PugCompilerException(node, getTemplateLoader(), "mixin " + node.getName() + " is not defined");
        }

        // Clone mixin
        try {
            mixin = (MixinNode) mixin.clone();
        } catch (CloneNotSupportedException e) {
            // Can't happen
            throw new IllegalStateException(e);
        }

        if (node.hasBlock()) {
            List<MixinBlockNode> injectionPoints = node.getInjectionPoints(mixin.getBlock());
            for (MixinBlockNode point : injectionPoints) {
                point.getNodes().add(node.getBlock());
            }
        }

        if (node.isCall()) {
            model.pushScope();
            model.putLocal("block", node.getBlock());
            final LinkedHashMap<String, Object> mixinVariables = node.getMixinVariables(model, mixin, getExpressionHandler(), getTemplateLoader());
            model.putAll(mixinVariables);
            Map<String, String> attrs = attributesCompiler.getAttributesMap(model, node, template.isTerse());
            model.putLocal("attributes", attrs);

            visit(writer, model, mixin.getBlock());
            model.putLocal("block", null);
            model.popScope();
        }
    }

    @Override
    public void visit(CaseNode node, IndentWriter writer, PugModel model) {
        try {
            boolean skip = false;
            for (Node when : node.getBlock().getNodes()) {
                if (skip || "default".equals(when.getValue()) || node.checkCondition(model, when, getExpressionHandler())) {
                    if (when.getBlock() != null) {
                        visit(writer, model, when);
                        break;
                    } else {
                        skip = true;
                    }
                }
            }
        } catch (ExpressionException e) {
            throw new PugCompilerException(node, getTemplateLoader(), e);
        }
    }

    @Override
    public void visit(CommentNode node, IndentWriter writer, PugModel model) {
        if (!node.isBuffered()) {
            return;
        }
        if (writer.isPp()) {
            writer.prettyIndent(1, true);
        }
        writer.append("<!--");
        writer.append(node.getValue());
        writer.append("-->");
    }

    @Override
    public void visit(ConditionalNode node, IndentWriter writer, PugModel model) {
        for (IfConditionNode conditionNode : node.getConditions()) {
            try {
                if (conditionNode.isDefault() || node.checkCondition(model, conditionNode.getValue(), getExpressionHandler()) ^ conditionNode.isInverse()) {
                    visit(writer, model, conditionNode.getBlock());
                    return;
                }
            } catch (ExpressionException e) {
                throw new PugCompilerException(conditionNode, getTemplateLoader(), e);
            }
        }
    }

    @Override
    public void visit(DoctypeNode node, IndentWriter writer, PugModel model) {
        writer.append(node.getDoctypeLine());
    }

    @Override
    public void visit(EachNode node, IndentWriter writer, PugModel model) {
        Object result;
        try {
            result = getExpressionHandler().evaluateExpression(node.getCode(), model);
        } catch (ExpressionException e) {
            throw new PugCompilerException(node, getTemplateLoader(), e);
        }
        if (result == null) {
            throw new PugCompilerException(node, getTemplateLoader(), "[" + node.getCode() + "] has to be iterable but was null");
        }
        model.pushScope();
        final Consumer<Node> nodeConsumer = (Node lambdaNode) -> visit(writer, model, lambdaNode);
        node.run(writer, model, result, getExpressionHandler(), getTemplateLoader(), nodeConsumer);
        model.popScope();
    }

    @Override
    public void visit(ExpressionNode node, IndentWriter writer, PugModel model) {

        String value = node.getValue();
        if (node.hasBlock() || value.trim().startsWith("}")) {
            String pug4j_buffer = bufferedExpressionString;
            if (pug4j_buffer.isEmpty()) {
                value = node.getValue();
            } else {
                if (node.getValue().trim().startsWith("}") && pug4j_buffer.trim().endsWith("}")) {
                    value = pug4j_buffer + " " + node.getValue().trim().substring(1);
                } else {
                    value = pug4j_buffer + " " + node.getValue();
                }
            }
            if (node.hasBlock()) {
                final Node block = node.getBlock();
                final Runnable runnable = () -> {
                    model.pushScope();
                    visit(writer, model, block);
                    model.popScope();
                };
                final String runnableKey = PUG4J_MODEL_PREFIX + "runnable_" + node.getNodeId();
                model.put(runnableKey, runnable);
                StringBuilder stringBuilder = new StringBuilder()
                        .append(value);
                if (!value.trim().endsWith("{")) {
                    stringBuilder.append("{");
                }

                bufferedExpressionString = stringBuilder
                        .append(runnableKey)
                        .append(".run();")
                        .append("}")
                        .toString();
            } else {
                bufferedExpressionString = value;
            }
        } else {
            Object result = null;
            try {
                result = getExpressionHandler().evaluateExpression(value, model);
            } catch (ExpressionException e) {
                throw new PugCompilerException(node, getTemplateLoader(), e);
            }
            if (result == null || !node.isBuffer()) {
                return;
            }
            String expressionValue;
            if (result.getClass().isArray()) {
                Object[] resultArray = (Object[]) result;
                expressionValue = StringUtils.joinWith(",", resultArray);
            } else if (result instanceof List) {
                List resultArray = (List) result;
                expressionValue = StringUtils.joinWith(",", resultArray.toArray());
            } else if (result instanceof Map) {
                expressionValue = new LinkedHashMap<>((Map) result).toString();
            } else {
                expressionValue = result.toString();
            }
            if (node.isEscape()) {
                expressionValue = StringEscapeUtils.escapeHtml4(expressionValue);
            }
            writer.append(expressionValue);
        }

    }

    @Override
    public void visit(FilterNode node, IndentWriter writer, PugModel model) {
        ArrayList<String> values = new ArrayList<>();
        LinkedList<Node> nodes = node.getBlock().getNodes();
        LinkedList<FilterNode> nestedFilterNodes = new LinkedList<>();

        //Find deepest FilterNode and get its nodes
        while (!nodes.isEmpty() && nodes.get(0) instanceof FilterNode) {
            FilterNode node1 = (FilterNode) nodes.get(0);
            nestedFilterNodes.push(node1);
            nodes = node1.getBlock().getNodes();
        }

        for (Node node1 : nodes) {
            values.add(node1.getValue());
        }

        String result = StringUtils.join(values, "");
        //For example:
        //:cdata:custom():custom1()
        for (FilterNode filterValue : nestedFilterNodes) {
            Filter filter = model.getFilter(filterValue.getValue());
            if (filter != null) {
                result = filter.convert(result, node.convertToFilterAttributes(getExpressionHandler(), getTemplateLoader(), model, filterValue.getAttributes()), model);
            }
        }

        //For example:
        //:cdata
        Filter filter = model.getFilter(node.getValue());
        if (filter != null) {
            result = filter.convert(result, node.convertToFilterAttributes(getExpressionHandler(), getTemplateLoader(), model, node.getAttributes()), model);
        }

        //For example:
        //include:filter1():filter2 file.ext
        for (IncludeFilterNode filterValue : node.getFilters()) {
            filter = model.getFilter(filterValue.getValue());
            if (filter != null) {
                result = filter.convert(result, node.convertToFilterAttributes(getExpressionHandler(), getTemplateLoader(), model, filterValue.getAttributes()), model);
            }
        }
        writer.append(result);
    }

    @Override
    public void visit(IfConditionNode node, IndentWriter writer, PugModel model) {
        visit(writer, model, node.getBlock());
    }

    @Override
    public void visit(IncludeFilterNode node, IndentWriter writer, PugModel model) {
        //nothing happens
    }

    @Override
    public void visit(LiteralNode node, IndentWriter writer, PugModel model) {
        writer.append(node.getValue());
    }

    @Override
    public void visit(MixinBlockNode node, IndentWriter writer, PugModel model) {
        LinkedList<Node> nodes = node.getNodes();
        if (nodes.size() == 1) {
            Node node1 = nodes.get(0);
            if (node1 != null)
                visit(writer, model, node1);
        }
    }

    @Override
    public void visit(MixinNode node, IndentWriter writer, PugModel model) {
        if (node.isCall()) {
            visit((CallNode) node, writer, model);
        } else {
            model.setMixin(node.getName(), node);
        }
    }

    @Override
    public void visit(TextNode node, IndentWriter writer, PugModel model) {
        writer.append(node.getValue());
    }

    @Override
    public void visit(WhileNode node, IndentWriter writer, PugModel model) {
        try {
            model.pushScope();
            while (getExpressionHandler().evaluateBooleanExpression(node.getValue(), model)) {
                visit(writer, model, node.getBlock());
            }
            model.popScope();
        } catch (ExpressionException e) {
            throw new PugCompilerException(node, getTemplateLoader(), e);
        }
    }


}