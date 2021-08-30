package de.neuland.pug4j.parser.node;

import com.google.gson.Gson;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;

import de.neuland.pug4j.compiler.IndentWriter;
import de.neuland.pug4j.exceptions.ExpressionException;
import de.neuland.pug4j.exceptions.PugCompilerException;
import de.neuland.pug4j.model.PugModel;
import de.neuland.pug4j.template.PugTemplate;

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
	private static Gson gson = new Gson();
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
			if (hasBlock()) {
				String pug4j_buffer = bufferedExpressionString;
				if(pug4j_buffer.length()==0)
					value = getValue();
				else
					value = pug4j_buffer+" "+getValue();

				model.put(PUG4J_MODEL_PREFIX+"innerblock_"+nodeId,block);
				model.put(PUG4J_MODEL_PREFIX+"template_"+nodeId,template);
				model.put(PUG4J_MODEL_PREFIX+"model",model);
				model.put(PUG4J_MODEL_PREFIX+"writer",writer);
				bufferedExpressionString = value+"{"+PUG4J_MODEL_PREFIX+"model.pushScope();"+PUG4J_MODEL_PREFIX+"innerblock_"+nodeId+".execute("+PUG4J_MODEL_PREFIX+"writer,"+PUG4J_MODEL_PREFIX+"model,pug4j__template_"+nodeId+");"+PUG4J_MODEL_PREFIX+"model.popScope();}";
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
