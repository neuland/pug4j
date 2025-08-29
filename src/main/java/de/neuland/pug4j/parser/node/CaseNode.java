package de.neuland.pug4j.parser.node;

import de.neuland.pug4j.exceptions.ExpressionException;
import de.neuland.pug4j.expression.ExpressionHandler;
import de.neuland.pug4j.model.PugModel;

public class CaseNode extends Node {

    public static class When extends Node {
    }

    public Boolean checkCondition(PugModel model, Node caseConditionNode, ExpressionHandler expressionHandler) throws ExpressionException {
        return expressionHandler.evaluateBooleanExpression(value + " == " + caseConditionNode.getValue(), model);
    }
}
