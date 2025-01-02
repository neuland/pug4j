package de.neuland.pug4j.compiler;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.neuland.pug4j.expression.ExpressionHandler;
import org.apache.commons.text.StringEscapeUtils;

import de.neuland.pug4j.exceptions.ExpressionException;
import de.neuland.pug4j.model.PugModel;

public class Utils {
	public static Pattern interpolationPattern = Pattern.compile("(\\\\)?([#!])\\{");

	public static List<Object> prepareInterpolate(String string, boolean xmlEscape) {
		List<Object> result = new LinkedList<Object>();

		Matcher matcher = interpolationPattern.matcher(string);
		int start = 0;
		while (matcher.find()) {
			String before = string.substring(start, matcher.start(0));
			if (xmlEscape) {
				before = escapeHTML(before);
			}
			result.add(before);

			boolean escape = matcher.group(1) != null;
			String flag = matcher.group(2);

			int openBrackets = 1;
			boolean closingBracketFound = false;
			int closingBracketIndex = matcher.end();
			while (!closingBracketFound && closingBracketIndex < string.length()) {
				char currentChar = string.charAt(closingBracketIndex);
				if (currentChar == '{') {
					openBrackets ++;
				}
				else if (currentChar == '}') {
					openBrackets --;
					if (openBrackets == 0) {
						closingBracketFound = true;
					}
				}
				closingBracketIndex++;
			}
			String code = string.substring(matcher.end(), closingBracketIndex -1);

			if (escape) {
				String escapedExpression = string.substring(matcher.start(0), closingBracketIndex).substring(1);
				if (xmlEscape) {
					escapedExpression = escapeHTML(escapedExpression);
				}
				result.add(escapedExpression);
			} else {
				InterpolatedString interpolatedString = new InterpolatedString(code);
				if (flag.equals("#")) {
					interpolatedString.setEscape(true);
				}
				result.add(interpolatedString);
			}
			start = closingBracketIndex;
		}
		String last = string.substring(start);
		if (xmlEscape) {
			last = escapeHTML(last);
		}
		result.add(last);

		return result;
	}

	public static String interpolate(List<Object> prepared, PugModel model, ExpressionHandler expressionHandler) throws ExpressionException {

		StringBuffer result = new StringBuffer();

		for (Object entry : prepared) {
			if (entry instanceof String) {
				result.append(entry);
			} else if (entry instanceof InterpolatedString) {
				InterpolatedString expression = (InterpolatedString) entry;
				String stringValue = "";
				String value = expressionHandler.evaluateStringExpression(expression.getValue(), model);
				if (value != null) {
					stringValue = value;
				}
				if (expression.isEscape()) {
					stringValue = escapeHTML(stringValue);
				}
				result.append(stringValue);
			}
		}

		return result.toString();
	}

	private static String escapeHTML(String string) {
		return StringEscapeUtils.escapeHtml4(string);
	}

	public static String interpolate(String string, PugModel model, boolean escape, ExpressionHandler expressionHandler) throws ExpressionException {
		return interpolate(prepareInterpolate(string, escape), model,expressionHandler);
	}
}
