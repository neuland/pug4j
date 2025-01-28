package de.neuland.pug4j.compiler;

import java.io.StringWriter;
import java.io.Writer;
import java.util.*;
import java.util.function.Consumer;

import de.neuland.pug4j.PugConfiguration;
import de.neuland.pug4j.exceptions.ExpressionException;
import de.neuland.pug4j.exceptions.PugCompilerException;
import de.neuland.pug4j.expression.ExpressionHandler;
import de.neuland.pug4j.filter.Filter;
import de.neuland.pug4j.model.PugModel;
import de.neuland.pug4j.parser.node.*;
import de.neuland.pug4j.template.PugTemplate;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;

import static de.neuland.pug4j.model.PugModel.PUG4J_MODEL_PREFIX;

public class Compiler {

	private PugTemplate template;
	private PugConfiguration configuration;
	private String bufferedExpressionString="";
	public Compiler(PugTemplate pugTemplate, PugConfiguration configuration) {
		this.template = pugTemplate;
		this.configuration = configuration;
	}

	public String compileToString(PugModel model) throws PugCompilerException {
		StringWriter writer = new StringWriter();
		compile(model, writer);
		return writer.toString();
	}

	public void compile(PugModel model, Writer w) throws PugCompilerException {
		IndentWriter writer = new IndentWriter(w);
		writer.setUseIndent(configuration.isPrettyPrint());
		visit(writer,model,template.getRootNode());

	}

	private void visit(final IndentWriter writer, final PugModel model, final Node node) {
		if(node instanceof TagNode) {
			visitTag(writer,model,(TagNode)node);
		} else if (node instanceof BlockNode) {
			visitBlockNode(writer,model,(BlockNode)node);
		} else if (node instanceof BlockCommentNode) {
			visitBlockCommentNode(writer,model,(BlockCommentNode)node);
		} else if (node instanceof MixinNode) {//Needs to be before CallNode
			visitMixinNode(writer,model,(MixinNode)node);
		} else if (node instanceof CallNode) {
			visitCallNode(writer,model,(CallNode)node);
		} else if (node instanceof CaseNode) {
			visitCaseNode(writer,model,(CaseNode)node);
		} else if (node instanceof CaseNode.When) {
			visitCaseNodeWhen(writer,model,(CaseNode.When)node);
		} else if (node instanceof CommentNode) {
			visitCommentNode(writer,model,(CommentNode)node);
		} else if (node instanceof ConditionalNode) {
			visitConditionalNode(writer,model,(ConditionalNode)node);
		} else if (node instanceof DoctypeNode) {
			visitDoctypeNode(writer,model,(DoctypeNode)node);
		} else if (node instanceof EachNode) {
			visitEachNode(writer,model,(EachNode)node);
		} else if (node instanceof ExpressionNode) {
			visitExpressionNode(writer,model,(ExpressionNode)node);
		} else if (node instanceof FilterNode) {
			visitFilterNode(writer,model,(FilterNode)node);
		} else if (node instanceof IfConditionNode) {
			visitIfConditionNode(writer,model,(IfConditionNode) node);
		} else if (node instanceof IncludeFilterNode) {
			visitIncludeFilterNode(writer,model,(IncludeFilterNode) node);
		} else if (node instanceof LiteralNode) {
			visitLiteralNode(writer,model,(LiteralNode) node);
		} else if (node instanceof MixinBlockNode) {
			visitMixinBlockNode(writer,model,(MixinBlockNode) node);
		} else if (node instanceof TextNode) {
			visitTextNode(writer,model,(TextNode)node);
		} else if (node instanceof WhileNode) {
			visitWhileNode(writer,model,(WhileNode)node);
		}

	}

	private void visitCaseNodeWhen(final IndentWriter writer, final PugModel model, final CaseNode.When node) {
		visit(writer,model,node.getBlock());
	}


	private void visitTag(final IndentWriter writer, final PugModel model, final TagNode node) {
		writer.increment();

		if (node.isWhitespaceSensitive()) {
			writer.setEscape(true);
		}

		// pretty print
		if (writer.isPp() && !node.isInline()) {
			writer.prettyIndent(0, true);
		}

		if (node.isSelfClosing() || (!template.isXml() && node.isSelfClosingTag())) {
			node.openTag(writer, model, configuration, !(template.isTerse() && !node.isSelfClosing()),template.isTerse());
			// TODO: if it is non-empty throw an error
//            if (tag.code ||
//                    tag.block &&
//                            !(tag.block.type === 'Block' && tag.block.nodes.length === 0) &&
//                            tag.block.nodes.some(function (tag) {
//                return tag.type !== 'Text' || !/^\s*$/.test(tag.val)
//            })) {
//                this.error(name + ' is a self closing element: <'+name+'/> but contains nested content.', 'SELF_CLOSING_CONTENT', tag);
//            }
		} else {
			node.openTag(writer, model, configuration, false, template.isTerse());

			if (node.hasCodeNode()) {
				visit(writer, model, node.getCodeNode());
			}
			if (node.hasBlock()) {
				visit(writer,model,node.getBlock());
			}
			if (writer.isPp() && !node.isInline() && !node.isWhitespaceSensitive() && !node.canInline()) {
				writer.prettyIndent(0, true);
			}
			writer.append("</");
			writer.append(node.bufferName(configuration, model));
			writer.append(">");
		}

		if (node.isWhitespaceSensitive()) {
			writer.setEscape(false);
		}
		writer.decrement();
	}

	private void visitBlockNode(final IndentWriter writer, final PugModel model, final BlockNode node) {

		// Pretty print multi-line text
		if (writer.isPp() && node.getNodes().size() > 1 && !writer.isEscape() && node.isTextNode(node.getNodes().get(0)) && node.isTextNode(node.getNodes().get(1)))
			writer.prettyIndent(1, true);
		bufferedExpressionString = "";
		for (int i = 0; i < node.getNodes().size(); ++i) {
			// Pretty print text
			Node node1 = node.getNodes().get(i);
			if (writer.isPp() && i > 0 && !writer.isEscape() && node.isTextNode(node1) && node.isTextNode(node.getNodes().get(i - 1)) && (node.getNodes().get(i - 1).getValue() != null && node.getNodes().get(i - 1).getValue().contains("\n")))
				writer.prettyIndent(1, false);
			if(node1 instanceof ExpressionNode && (node1.hasBlock()|| node1.getValue().trim().startsWith("}"))){
//				((ExpressionNode) node1).setBufferedExpressionString(bufferedExpressionString);//TODO: don't set on node
			}
			visit(writer, model, node1);
			if(node1 instanceof ExpressionNode && (node1.hasBlock()|| node1.getValue().trim().startsWith("}"))){
//				bufferedExpressionString = ((ExpressionNode) node1).getBufferedExpressionString();
			}

			Node nextNode = null;
			if(i+1 < node.getNodes().size())
				nextNode = node.getNodes().get(i + 1);

			//If multiple expressions in a row evaluate buffered code
			if(bufferedExpressionString.length()>0 && (nextNode==null || !(nextNode!=null && nextNode instanceof ExpressionNode && (nextNode.hasBlock()||nextNode.getValue().trim().startsWith("}"))))){
				try {
					configuration.getExpressionHandler().evaluateExpression(bufferedExpressionString, model);
				} catch (ExpressionException e) {
					throw new PugCompilerException(node, configuration.getTemplateLoader(), e);
				}
				bufferedExpressionString = "";
			}
		}


	}
	private void visitBlockCommentNode(final IndentWriter writer, final PugModel model, final BlockCommentNode node) {
		if (!node.isBuffered()) {
				  return;
			  }
		if(writer.isPp()) {
		  writer.prettyIndent(1, true);
	  }
		writer.append("<!--" + node.getValue());
		visit(writer,model,node.getBlock());
		if(writer.isPp()) {
			writer.prettyIndent(1, true);
		}
		writer.append("-->");
	}

	private void visitCallNode(final IndentWriter writer, final PugModel model, final CallNode node) {
		boolean dynamic = node.getName().charAt(0)=='#';

		String newname = (dynamic ? node.getName().substring(2, node.getName().length()-1):'"'+ node.getName()+'"');
		try {
			newname = (String) configuration.getExpressionHandler().evaluateExpression(newname, model);
		} catch (ExpressionException e) {
			throw new PugCompilerException(node, configuration.getTemplateLoader(), e);
		}

		MixinNode mixin;
		if(dynamic)
			mixin = model.getMixin(newname);
		else
			mixin = model.getMixin(node.getName());

		if (mixin == null) {
			throw new PugCompilerException(node, configuration.getTemplateLoader(), "mixin " + node.getName() + " is not defined");
		}

		// Clone mixin
		try {
			mixin = (MixinNode) mixin.clone();
		} catch (CloneNotSupportedException e) {
			// Can't happen
			throw new IllegalStateException(e);
		}

		if (node.hasBlock()) {
			List<MixinBlockNode> injectionPoints = node.getInjectionPoints(mixin.getBlock());
            for (MixinBlockNode point : injectionPoints) {
				point.getNodes().add(node.getBlock());
            }
		}

		if (node.isCall()) {
			model.pushScope();
			model.putLocal("block", node.getBlock());
			node.writeVariables(model, mixin, configuration);
			node.writeAttributes(model, mixin, configuration,template.isTerse());
			visit(writer,model,mixin.getBlock());
			model.putLocal("block",null);
			model.popScope();
		}
	}

	private void visitCaseNode(final IndentWriter writer, final PugModel model, final CaseNode node) {
		try {
			boolean skip = false;
			for (Node when : node.getBlock().getNodes()) {
				if (skip || "default".equals(when.getValue()) || node.checkCondition(model, when, configuration.getExpressionHandler())) {
					if(when.getBlock()!=null) {
						visit(writer,model,when);
						break;
					}else {
						skip = true;
					}
				}
			}
		} catch (ExpressionException e) {
			throw new PugCompilerException(node, configuration.getTemplateLoader(), e);
		}
	}

	private void visitCommentNode(final IndentWriter writer, final PugModel model, final CommentNode node) {
		if (!node.isBuffered()) {
				  return;
			  }
		if(writer.isPp()) {
		  writer.prettyIndent(1, true);
	  }
		writer.append("<!--");
		writer.append(node.getValue());
		writer.append("-->");
	}

	private void visitConditionalNode(final IndentWriter writer, final PugModel model, final ConditionalNode node) {
		for (IfConditionNode conditionNode : node.getConditions()) {
			try {
				if (conditionNode.isDefault() || node.checkCondition(model, conditionNode.getValue(), configuration.getExpressionHandler()) ^ conditionNode.isInverse()) {
					visit(writer,model,conditionNode.getBlock());
					return;
				}
			} catch (ExpressionException e) {
				throw new PugCompilerException(conditionNode, configuration.getTemplateLoader(), e);
			}
		}
	}

	private void visitDoctypeNode(final IndentWriter writer, final PugModel model, final DoctypeNode node) {
        writer.append(node.getDoctypeLine());
	}

	private void visitEachNode(final IndentWriter writer, final PugModel model, final EachNode node) {
		Object result;
		try {
			result = configuration.getExpressionHandler().evaluateExpression(node.getCode(), model);
		} catch (ExpressionException e) {
			throw new PugCompilerException(node, configuration.getTemplateLoader(), e);
		}
		if (result == null) {
			throw new PugCompilerException(node, configuration.getTemplateLoader(), "[" + node.getCode() + "] has to be iterable but was null");
		}
		model.pushScope();
		final Consumer<Node> nodeConsumer = (Node lambdaNode) -> visit(writer, model, lambdaNode);
		node.run(writer, model, result, configuration, nodeConsumer);
		model.popScope();
	}

	private void visitExpressionNode(final IndentWriter writer, final PugModel model, final ExpressionNode node) {

		String value = node.getValue();
		if (node.hasBlock() || value.trim().startsWith("}")) {
			String pug4j_buffer = bufferedExpressionString;
			if(pug4j_buffer.isEmpty()) {
				value = node.getValue();
			} else {
				if(node.getValue().trim().startsWith("}") && pug4j_buffer.trim().endsWith("}")){
					value = pug4j_buffer + " " + node.getValue().trim().substring(1);
				}else {
					value = pug4j_buffer + " " + node.getValue();
				}
			}
			if(node.hasBlock()) {
				final Node block = node.getBlock();
				final Runnable runnable = () -> {
					model.pushScope();
					visit(writer, model, block);
					model.popScope();
				};
				final String runnableKey = PUG4J_MODEL_PREFIX + "runnable_" + node.getNodeId();
				model.put(runnableKey, runnable);
				StringBuilder stringBuilder = new StringBuilder()
						.append(value);
				if (!value.trim().endsWith("{")) {
                    stringBuilder.append("{");
                }

				bufferedExpressionString = stringBuilder
						.append(runnableKey)
						.append(".run();")
						.append("}")
						.toString();
			}else{
				bufferedExpressionString = value;
			}
		}else {
			Object result = null;
			try {
				result = configuration.getExpressionHandler().evaluateExpression(value, model);
			} catch (ExpressionException e) {
				throw new PugCompilerException(node, configuration.getTemplateLoader(), e);
			}
			if (result == null || !node.isBuffer()) {
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
			if (node.isEscape()) {
				expressionValue = StringEscapeUtils.escapeHtml4(expressionValue);
			}
			writer.append(expressionValue);
		}

	}

	private void visitFilterNode(final IndentWriter writer, final PugModel model, final FilterNode node) {
		ArrayList<String> values = new ArrayList<String>();
		LinkedList<Node> nodes = node.getBlock().getNodes();
		LinkedList<FilterNode> nestedFilterNodes = new LinkedList<>();

		//Find deepest FilterNode and get its nodes
		while(nodes.size()>0 && nodes.get(0) instanceof FilterNode){
			FilterNode node1 = (FilterNode)nodes.get(0);
			nestedFilterNodes.push(node1);
			nodes = node1.getBlock().getNodes();
		}

		for (Node node1 : nodes) {
			values.add(node1.getValue());
		}

		ExpressionHandler expressionHandler = configuration.getExpressionHandler();
		String result = StringUtils.join(values, "");
		//For example:
		//:cdata:custom():custom1()
		for (FilterNode filterValue : nestedFilterNodes) {
			Filter filter = model.getFilter(filterValue.getValue());
			if (filter != null) {
				result = filter.convert(result, node.convertToFilterAttributes(configuration, model, filterValue.getAttributes()), model);
			}
		}

		//For example:
		//:cdata
		Filter filter = model.getFilter(node.getValue());
		if (filter != null) {
			result = filter.convert(result, node.convertToFilterAttributes(configuration, model, node.getAttributes()), model);
		}

		//For example:
		//include:filter1():filter2 file.ext
		for (IncludeFilterNode filterValue : node.getFilters()) {
			filter = model.getFilter(filterValue.getValue());
			if (filter != null) {
				result = filter.convert(result, node.convertToFilterAttributes(configuration, model, filterValue.getAttributes()), model);
			}
		}
		writer.append(result);
	}

	private void visitIfConditionNode(final IndentWriter writer, final PugModel model, final IfConditionNode node) {
		visit(writer,model,node.getBlock());
	}

	private void visitIncludeFilterNode(final IndentWriter writer, final PugModel model, final IncludeFilterNode node) {
		//nothing happens
	}

	private void visitLiteralNode(final IndentWriter writer, final PugModel model, final LiteralNode node) {
		writer.append(node.getValue());
	}

	private void visitMixinBlockNode(final IndentWriter writer, final PugModel model, final MixinBlockNode node) {
		LinkedList<Node> nodes = node.getNodes();
		if(nodes.size()==1) {
			Node node1 = nodes.get(0);
			if (node1 != null)
				visit(writer,model,node1);
		}
	}

	private void visitMixinNode(final IndentWriter writer, final PugModel model, final MixinNode node) {
		if (node.isCall()) {
			visitCallNode(writer, model, node);
		} else {
			model.setMixin(node.getName(), node);
		}
	}

	private void visitTextNode(final IndentWriter writer, final PugModel model, final TextNode node) {
		writer.append(node.getValue());
	}

	private void visitWhileNode(final IndentWriter writer, final PugModel model, final WhileNode node) {
		try {
			model.pushScope();
			while (configuration.getExpressionHandler().evaluateBooleanExpression(node.getValue(), model)) {
				visit(writer,model,node.getBlock());
			}
			model.popScope();
		} catch (ExpressionException e) {
			throw new PugCompilerException(node, configuration.getTemplateLoader(), e);
		}
	}



}