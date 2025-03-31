package de.neuland.pug4j.expression;

import de.neuland.pug4j.jexl3.PugJexlArithmetic;
import de.neuland.pug4j.jexl3.PugJexlBuilder;
import org.apache.commons.jexl3.*;

import de.neuland.pug4j.exceptions.ExpressionException;
import de.neuland.pug4j.model.PugModel;
import org.apache.commons.jexl3.internal.introspection.Uberspect;
import org.apache.commons.jexl3.introspection.JexlPermissions;
import org.apache.commons.jexl3.introspection.JexlUberspect;
import org.apache.commons.logging.LogFactory;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JexlExpressionHandler extends AbstractExpressionHandler {

	private static final int MAX_ENTRIES = 5000;
	public static Pattern plusplus = Pattern.compile("([a-zA-Z0-9-_]*[a-zA-Z0-9])\\+\\+\\s*;{0,1}\\s*$");
	public static Pattern isplusplus = Pattern.compile("\\+\\+\\s*;{0,1}\\s*$");
	public static Pattern minusminus = Pattern.compile("([a-zA-Z0-9-_]*[a-zA-Z0-9])--\\s*;{0,1}\\s*$");
	public static Pattern isminusminus = Pattern.compile("--\\s*;{0,1}\\s*$");
	private JexlEngine jexl;
	private final JexlExpressionHandlerOptions options = new JexlExpressionHandlerOptions();
	private final Uberspect pugUberspect = new Uberspect(LogFactory.getLog(JexlExpressionHandler.class),
		(op, obj) -> {
			if (obj instanceof Map) {
				return JexlUberspect.MAP;
			}
			if (op == JexlOperator.ARRAY_GET) {
				return JexlUberspect.MAP;
			} else if (op == JexlOperator.ARRAY_SET) {
				return JexlUberspect.MAP;
			} else {
				return JexlUberspect.POJO;
			}
		},
		JexlPermissions.UNRESTRICTED
	);

	private final PugJexlArithmetic pugJexlArithmetic = new PugJexlArithmetic(false);


	public JexlExpressionHandler() {
		jexl = getJexlEngine(options);
	}

	public JexlExpressionHandler(JexlExpressionHandlerOptions options) {
		jexl = getJexlEngine(options);
	}

	private JexlEngine getJexlEngine(JexlExpressionHandlerOptions options) {
		return getPugJexlBuilder(options).create();
	}

	private JexlBuilder getPugJexlBuilder(JexlExpressionHandlerOptions options) {
		return new PugJexlBuilder()
				.arithmetic(pugJexlArithmetic)
				.uberspect(pugUberspect)
				.safe(true)
				.silent(false)
				.strict(false)
				.cacheThreshold(options.getCacheThreshold())
				.cache(options.getCache())
				.debug(options.isDebug());
	}

	public Boolean evaluateBooleanExpression(String expression, PugModel model) throws ExpressionException {
		return BooleanUtil.convert(evaluateExpression(expression, model));
	}

	public Object evaluateExpression(String expression, PugModel model) throws ExpressionException {
		try {
			saveLocalVariableName(expression, model);
			expression = removeVar(expression);
			if (isplusplus.matcher(expression).find()) {
				expression = convertPlusPlusExpression(expression);
			}
			if (isminusminus.matcher(expression).find()) {
				expression = convertMinusMinusExpression(expression);
			}
			JexlScript e = jexl.createScript(expression);
            MapContext jexlContext = new MapContext(model);
            return e.execute(jexlContext);
		} catch (Exception e) {
			throw new ExpressionException(expression, e);
		}
	}

	private String convertMinusMinusExpression(String expression) {
		Matcher matcher = minusminus.matcher(expression);
		if (matcher.find(0) && matcher.groupCount() == 1) {
            String a = matcher.group(1);
            expression = a + " = " + a + " - 1";
        }
		return expression;
	}

	private String convertPlusPlusExpression(String expression) {
		Matcher matcher = plusplus.matcher(expression);
		if (matcher.find(0) && matcher.groupCount() == 1) {
            String a = matcher.group(1);
            expression = a + " = " + a + " + 1";
        }
		return expression;
	}

	private String removeVar(String expression) {
		expression = expression.replace("var ",";");
		expression = expression.replace("let ",";");
		expression = expression.replace("const ",";");
		return expression;
	}

	public void assertExpression(String expression) throws ExpressionException {
		try {
			jexl.createExpression(expression);
		} catch (Exception e) {
			throw new ExpressionException(expression, e);
		}
	}

	public String evaluateStringExpression(String expression, PugModel model) throws ExpressionException {
		Object result = evaluateExpression(expression, model);
		return result == null ? "" : result.toString();
	}
	
	public void setCache(boolean cache) {
		if(cache)
			options.setCache(MAX_ENTRIES);
		else
			options.setCache(0);
		jexl = getJexlEngine(options);
	}

    public void clearCache() {
        jexl.clearCache();
    }
}
