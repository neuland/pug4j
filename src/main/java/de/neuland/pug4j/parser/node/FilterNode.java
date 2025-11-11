package de.neuland.pug4j.parser.node;

import java.util.*;

import de.neuland.pug4j.PugConfiguration;
import de.neuland.pug4j.compiler.IndentWriter;
import de.neuland.pug4j.compiler.NodeVisitor;
import de.neuland.pug4j.exceptions.ExpressionException;
import de.neuland.pug4j.exceptions.PugCompilerException;
import de.neuland.pug4j.model.PugModel;

public class FilterNode extends AttrsNode {

    private LinkedList<IncludeFilterNode> filters = new LinkedList<>();

    public Map<String, Object> convertToFilterAttributes(
            de.neuland.pug4j.expression.ExpressionHandler expressionHandler,
            de.neuland.pug4j.template.TemplateLoader templateLoader,
            PugModel model, LinkedList<Attr> attributes) {
        Map<String, Object> evaluatedAttributes = new HashMap<>();
        for (Attr attribute : attributes) {
            if (attribute.getValue() instanceof ExpressionString) {
                try {
                    evaluatedAttributes.put(attribute.getName(), expressionHandler.evaluateExpression(((ExpressionString) attribute.getValue()).getValue(), model));
                } catch (ExpressionException e) {
                    throw new PugCompilerException(this, templateLoader, e);
                }
            } else
                evaluatedAttributes.put(attribute.getName(), attribute.getValue());
        }
        return evaluatedAttributes;
    }

    /**
     * @deprecated Use convertToFilterAttributes(ExpressionHandler, TemplateLoader, PugModel, LinkedList) instead
     */
    @Deprecated
    public Map<String, Object> convertToFilterAttributes(PugConfiguration configuration, PugModel model, LinkedList<Attr> attributes) {
        return convertToFilterAttributes(configuration.getExpressionHandler(), configuration.getTemplateLoader(), model, attributes);
    }

    public void setFilter(LinkedList<IncludeFilterNode> filters) {
        this.filters = filters;
    }

    public LinkedList<IncludeFilterNode> getFilters() {
        return filters;
    }

    @Override
    public void accept(NodeVisitor visitor, IndentWriter writer, PugModel model) {
        visitor.visit(this, writer, model);
    }
}
