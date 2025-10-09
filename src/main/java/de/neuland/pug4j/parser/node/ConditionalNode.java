package de.neuland.pug4j.parser.node;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import de.neuland.pug4j.compiler.IndentWriter;
import de.neuland.pug4j.compiler.NodeVisitor;
import de.neuland.pug4j.exceptions.ExpressionException;
import de.neuland.pug4j.expression.ExpressionHandler;
import de.neuland.pug4j.model.PugModel;

public class ConditionalNode extends Node {

    private List<IfConditionNode> conditions = new LinkedList<IfConditionNode>();

    public boolean checkCondition(PugModel model, String condition, ExpressionHandler expressionHandler) throws ExpressionException {
        Boolean value = expressionHandler.evaluateBooleanExpression(condition, model);
        return value != null && value;
    }

    public List<IfConditionNode> getConditions() {
        return Collections.unmodifiableList(conditions);
    }

    public void addCondition(IfConditionNode condition) {
        this.conditions.add(condition);
    }

    @Override
    public ConditionalNode clone() throws CloneNotSupportedException {
        ConditionalNode clone = (ConditionalNode) super.clone();

        clone.conditions = new LinkedList<IfConditionNode>();
        for (IfConditionNode condition : conditions) {
            clone.conditions.add((IfConditionNode) condition.clone());
        }

        return clone;
    }

    @Override
    public void accept(NodeVisitor visitor, IndentWriter writer, PugModel model) {
        visitor.visit(this, writer, model);
    }
}
