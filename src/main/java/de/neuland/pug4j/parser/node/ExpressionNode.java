package de.neuland.pug4j.parser.node;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;

import de.neuland.pug4j.compiler.IndentWriter;
import de.neuland.pug4j.exceptions.ExpressionException;
import de.neuland.pug4j.exceptions.PugCompilerException;
import de.neuland.pug4j.model.PugModel;
import de.neuland.pug4j.template.PugTemplate;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static de.neuland.pug4j.model.PugModel.PUG4J_MODEL_PREFIX;

public class ExpressionNode extends Node {

	private boolean escape;
	private boolean buffer;
	private boolean inline;
	private String bufferedExpressionString = "";
	private String nodeId;
	public ExpressionNode() {
		super();
		nodeId = createNodeId();
	}

	public void setEscape(boolean escape) {
		this.escape = escape;
	}

	public void setBuffer(boolean buffer) {
		this.buffer = buffer;
	}

	public boolean isInline() {
		return inline;
	}

	public void setInline(boolean inline) {
		this.inline = inline;
	}
	private String createNodeId(){
		return UUID.randomUUID().toString().replace("-","");
	}
	public String getBufferedExpressionString(){
		return bufferedExpressionString;
	}

	public void setBufferedExpressionString(String bufferedExpressionString) {
		this.bufferedExpressionString = bufferedExpressionString;
	}

	@Override
	public void execute(IndentWriter writer, PugModel model, PugTemplate template) throws PugCompilerException {

			String value = getValue();
			if (hasBlock() || value.trim().startsWith("}")) {
				String pug4j_buffer = bufferedExpressionString;
				if(pug4j_buffer.length()==0) {
					value = getValue();
				} else {
					if(getValue().trim().startsWith("}") && pug4j_buffer.trim().endsWith("}")){
						value = pug4j_buffer + " " + getValue().trim().substring(1);
					}else {
						value = pug4j_buffer + " " + getValue();
					}
				}
				if(hasBlock()) {
					model.put(PUG4J_MODEL_PREFIX + "innerblock_" + nodeId, block);
					model.put(PUG4J_MODEL_PREFIX + "template_" + nodeId, template);
					model.put(PUG4J_MODEL_PREFIX + "model", model);
					model.put(PUG4J_MODEL_PREFIX + "writer", writer);
					StringBuilder stringBuilder = new StringBuilder()
							.append(value);
					if (!value.trim().endsWith("{")) {
						stringBuilder = stringBuilder.append("{");
					}

					bufferedExpressionString = stringBuilder
							.append(PUG4J_MODEL_PREFIX)
							.append("model.pushScope();")
							.append(PUG4J_MODEL_PREFIX)
							.append("innerblock_")
							.append(nodeId)
							.append(".execute(")
							.append(PUG4J_MODEL_PREFIX)
							.append("writer,")
							.append(PUG4J_MODEL_PREFIX)
							.append("model,pug4j__template_")
							.append(nodeId).append(");")
							.append(PUG4J_MODEL_PREFIX)
							.append("model.popScope();")
							.append("}")
							.toString();
				}else{
					bufferedExpressionString = value;
				}
			}else {
				Object result = null;
				try {
					result = template.getExpressionHandler().evaluateExpression(value, model);
				} catch (ExpressionException e) {
					throw new PugCompilerException(this, template.getTemplateLoader(), e);
				}
				if (result == null || !buffer) {
					return;
				}
				String expressionValue;
				if(result.getClass().isArray()){
					Object[] resultArray = (Object[])result;
					expressionValue = StringUtils.joinWith(",",resultArray);
				}else if(result instanceof List){
					List resultArray = (List)result;
					expressionValue = StringUtils.joinWith(",",resultArray.toArray());
				}else if(result instanceof Map){
					expressionValue = new LinkedHashMap<String,Object>((Map)result).toString();
				}else{
					expressionValue = result.toString();
				}
				if (escape) {
					expressionValue = StringEscapeUtils.escapeHtml4(expressionValue);
				}
				writer.append(expressionValue);
			}

	}

	@Override
	public void setValue(String value) {
		super.setValue(value.trim());
	}
}
