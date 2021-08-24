package de.neuland.pug4j.parser.node;

import de.neuland.pug4j.compiler.IndentWriter;
import de.neuland.pug4j.exceptions.ExpressionException;
import de.neuland.pug4j.exceptions.PugCompilerException;
import de.neuland.pug4j.model.PugModel;
import de.neuland.pug4j.parser.Parser;
import de.neuland.pug4j.template.PugTemplate;

public class BlockNode extends Node {

	private boolean yield = false;
	private String mode;
	private Parser parser;
	private boolean namedBlock;

	public void execute(IndentWriter writer, PugModel model, PugTemplate template) throws PugCompilerException {

		// Pretty print multi-line text
		if (writer.isPp() && getNodes().size() > 1 && !writer.isEscape() && isTextNode(getNodes().get(0)) && isTextNode(getNodes().get(1)))
			writer.prettyIndent(1, true);
		String bufferedExpressionString = "";
		for (int i = 0; i < getNodes().size(); ++i) {
			// Pretty print text
			Node node = getNodes().get(i);
			if (writer.isPp() && i > 0 && !writer.isEscape() && isTextNode(node) && isTextNode(getNodes().get(i - 1)) && (getNodes().get(i - 1).getValue() != null && getNodes().get(i - 1).getValue().contains("\n")))
				writer.prettyIndent(1, false);
			if(node instanceof ExpressionNode && node.hasBlock()){
				((ExpressionNode) node).setBufferedExpressionString(bufferedExpressionString);
			}
			node.execute(writer, model, template);
			if(node instanceof ExpressionNode && node.hasBlock()){
				bufferedExpressionString = ((ExpressionNode) node).getBufferedExpressionString();
			}

			Node nextNode = null;
			if(i+1 < getNodes().size())
				nextNode = getNodes().get(i + 1);

			//If multiple expressions in a row evaluate buffered code
			if(node instanceof ExpressionNode && node.hasBlock() && (nextNode==null || !(nextNode!=null && nextNode instanceof ExpressionNode && nextNode.hasBlock()))){
				try {
					Object result = template.getExpressionHandler().evaluateExpression(bufferedExpressionString, model);
				} catch (ExpressionException e) {
					throw new PugCompilerException(this, template.getTemplateLoader(), e);
				}
				bufferedExpressionString = "";
			}
		}


	}

	public void setYield(boolean yield) {
		this.yield = yield;
	}

	public boolean isYield() {
		return yield;
	}

	public BlockNode getYieldBlock() {
		BlockNode ret = this;
		for (Node node : getNodes()) {
			if (node instanceof BlockNode && ((BlockNode) node).isYield()) {
				return (BlockNode) node;
			}
			else if (node instanceof TagNode && ((TagNode) node).isTextOnly()) {
				continue;
			}
			else if (node instanceof BlockNode && ((BlockNode) node).getYieldBlock() != null) {
				ret =  ((BlockNode) node).getYieldBlock();
			}
			else if (node.hasBlock()) {
				ret =  ((BlockNode) node.getBlock()).getYieldBlock();
			}
			if(ret instanceof BlockNode && ((BlockNode) ret).isYield()){
				return ret;
			}
		}
		return ret;
	}

	public String getMode() {
		return mode;
	}

	public void setMode(String mode) {
		this.mode = mode;
	}

	public void setParser(Parser parser) {
		this.parser = parser;
	}

	public Parser getParser() {
		return parser;
	}

	public boolean isNamedBlock() {
		return namedBlock;
	}

	public void setNamedBlock(boolean namedBlock) {
		this.namedBlock = namedBlock;
	}
}
