package de.neuland.pug4j.parser.node;

import java.util.*;

import de.neuland.pug4j.compiler.IndentWriter;
import de.neuland.pug4j.compiler.Utils;
import de.neuland.pug4j.exceptions.ExpressionException;
import de.neuland.pug4j.exceptions.PugCompilerException;
import de.neuland.pug4j.expression.ExpressionHandler;
import de.neuland.pug4j.filter.Filter;
import de.neuland.pug4j.model.PugModel;
import de.neuland.pug4j.template.PugTemplate;
import org.apache.commons.lang3.StringUtils;

public class FilterNode extends AttrsNode {

	private LinkedList<IncludeFilterNode> filters = new LinkedList<>();

	@Override
	public void execute(IndentWriter writer, PugModel model, PugTemplate template) throws PugCompilerException {
		ArrayList<String> values = new ArrayList<String>();
		LinkedList<Node> nodes = block.getNodes();
		for (Node node : nodes) {
			values.add(node.getValue());
		}
		ExpressionHandler expressionHandler = template.getExpressionHandler();
		String result = StringUtils.join(values, "");
		Filter filter = model.getFilter(getValue());
		if (filter != null) {
			result = filter.convert(result, convertToFilterAttributes(template,model,attributes), model);
		}
		for (IncludeFilterNode filterValue : filters) {
			filter = model.getFilter(filterValue.getValue());
			if (filter != null) {
				result = filter.convert(result, convertToFilterAttributes(template, model, filterValue.getAttributes()), model);
			}
		}

		try {

			result = Utils.interpolate(result, model, false, expressionHandler);
		} catch (ExpressionException e) {
			throw new PugCompilerException(this, template.getTemplateLoader(), e);
		}
		writer.append(result);
	}

	private Map<String, Object> convertToFilterAttributes(PugTemplate template, PugModel model, LinkedList<Attr> attributes) {
		Map evaluatedAttributes = new HashMap<String,Object>() ;
		for (Attr attribute : attributes) {
			if(attribute.getValue() instanceof ExpressionString) {
				try {
					evaluatedAttributes.put(attribute.getName(),template.getExpressionHandler().evaluateExpression(((ExpressionString)attribute.getValue()).getValue(),model));
				} catch (ExpressionException e) {
					throw new PugCompilerException(this, template.getTemplateLoader(), e);
				}
			}
			else
				evaluatedAttributes.put(attribute.getName(),attribute.getValue());
		}
		return evaluatedAttributes;
	}

	public void setFilter(LinkedList<IncludeFilterNode> filters) {
		this.filters = filters;
	}
}
