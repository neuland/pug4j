package de.neuland.pug4j.parser.node;

import de.neuland.pug4j.compiler.IndentWriter;
import de.neuland.pug4j.compiler.NodeVisitor;
import de.neuland.pug4j.exceptions.ExpressionException;
import de.neuland.pug4j.expression.ExpressionHandler;
import de.neuland.pug4j.model.PugModel;

public class CaseNode extends Node {

    public static class When extends Node {
        @Override
        public void accept(NodeVisitor visitor, IndentWriter writer, PugModel model) {
            visitor.visit(this, writer, model);
        }
    }

    public Boolean checkCondition(PugModel model, Node caseConditionNode, ExpressionHandler expressionHandler) throws ExpressionException {
        return expressionHandler.evaluateBooleanExpression(value + " == " + caseConditionNode.getValue(), model);
    }

    @Override
    public void accept(NodeVisitor visitor, IndentWriter writer, PugModel model) {
        visitor.visit(this, writer, model);
    }
}
