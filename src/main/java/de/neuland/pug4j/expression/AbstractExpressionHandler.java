package de.neuland.pug4j.expression;

import de.neuland.pug4j.model.PugModel;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class AbstractExpressionHandler implements ExpressionHandler {

    public static Pattern isLocalAssignment = Pattern.compile("^(var|let|const)[\\s]+([a-zA-Z0-9-_]+)[\\s]?={1}[\\s]?[^=]+$");

    protected void saveLocalVariableName(String expression, PugModel model) {
        Matcher matcher = isLocalAssignment.matcher(expression);
        if (matcher.matches()) {
            String var = matcher.group(2);
            model.putLocalVariableName(var);
        }
    }

    @Override
    public void setCache(boolean cache) {

    }

    @Override
    public void clearCache() {

    }
}
