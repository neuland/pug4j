package de.neuland.pug4j.parser.node;

import java.util.*;

import de.neuland.pug4j.PugConfiguration;
import de.neuland.pug4j.compiler.IndentWriter;
import de.neuland.pug4j.compiler.NodeVisitor;
import de.neuland.pug4j.exceptions.PugCompilerException;
import de.neuland.pug4j.model.PugModel;
import de.neuland.pug4j.parser.ArgumentSplitter;

public class CallNode extends AttrsNode {

    protected List<String> arguments = new ArrayList<>();
    private boolean call = false;

    public List<MixinBlockNode> getInjectionPoints(Node block) {
        List<MixinBlockNode> result = new ArrayList<>();
        for (Node node : block.getNodes()) {
            if (node instanceof MixinBlockNode && !node.hasNodes()) {
                result.add((MixinBlockNode) node);
            } else if (node instanceof ConditionalNode) {
                for (IfConditionNode condition : ((ConditionalNode) node).getConditions()) {
                    result.addAll(getInjectionPoints(condition.getBlock()));
                }
            } else if (node.hasBlock()) {
                result.addAll(getInjectionPoints(node.getBlock()));
            }
        }
        return result;
    }

    public LinkedHashMap<String, Object> getMixinVariables(PugModel model, MixinNode mixin,
            de.neuland.pug4j.expression.ExpressionHandler expressionHandler,
            de.neuland.pug4j.template.TemplateLoader templateLoader) {
        LinkedHashMap<String, Object> mixinVariables = new LinkedHashMap<>();
        List<String> names = mixin.getArguments();
        List<String> values = arguments;
        if (names == null) {
            return mixinVariables;
        }

        for (int i = 0; i < names.size(); i++) {
            String key = names.get(i);
            String valueExpression = mixin.getDefaultValues().get(key);
            Object value = null;
            if (i < values.size()) {
                valueExpression = values.get(i);
            }
            if (valueExpression != null) {
                try {
                    value = expressionHandler.evaluateExpression(valueExpression, model);
                } catch (Throwable e) {
                    throw new PugCompilerException(this, templateLoader, e);
                }
            }
            if (key != null) {
                mixinVariables.put(key, value);
            }
        }
        if (mixin.getRest() != null) {
            ArrayList<Object> restArguments = new ArrayList<>();
            for (int i = names.size(); i < arguments.size(); i++) {
                Object value = null;
                if (i < values.size()) {
                    value = values.get(i);
                }
                if (value != null) {
                    try {
                        value = expressionHandler.evaluateExpression(values.get(i), model);
                    } catch (Throwable e) {
                        throw new PugCompilerException(this, templateLoader, e);
                    }
                }
                restArguments.add(value);
            }
            mixinVariables.put(mixin.getRest(), restArguments);
        }
        return mixinVariables;
    }

    /**
     * @deprecated Use getMixinVariables(PugModel, MixinNode, ExpressionHandler, TemplateLoader) instead
     */
    @Deprecated
    public LinkedHashMap<String, Object> getMixinVariables(PugModel model, MixinNode mixin, PugConfiguration configuration) {
        return getMixinVariables(model, mixin, configuration.getExpressionHandler(), configuration.getTemplateLoader());
    }

    public List<String> getArguments() {
        return arguments;
    }

    public void setArguments(List<String> arguments) {
        this.arguments = arguments;
    }

    public void setArguments(String arguments) {
        this.arguments.clear();
        this.arguments = ArgumentSplitter.split(arguments);
    }

    public boolean isCall() {
        return call;
    }

    public void setCall(boolean call) {
        this.call = call;
    }

    @Override
    public void accept(NodeVisitor visitor, IndentWriter writer, PugModel model) {
        visitor.visit(this, writer, model);
    }
}
