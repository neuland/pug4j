package de.neuland.pug4j.parser.node;

import java.util.*;

import de.neuland.pug4j.PugConfiguration;
import de.neuland.pug4j.exceptions.ExpressionException;
import de.neuland.pug4j.exceptions.PugCompilerException;
import de.neuland.pug4j.model.PugModel;
import de.neuland.pug4j.parser.ArgumentSplitter;

public class CallNode extends AttrsNode {

	protected List<String> arguments = new ArrayList<String>();
	boolean call = false;

	public List<MixinBlockNode> getInjectionPoints(Node block) {
        List<MixinBlockNode> result = new ArrayList<MixinBlockNode>();
		for (Node node : block.getNodes()) {
			if (node instanceof MixinBlockNode && !node.hasNodes()) {
                result.add((MixinBlockNode) node);
			} else if(node instanceof ConditionalNode){
                for (IfConditionNode condition : ((ConditionalNode) node).getConditions()) {
                    result.addAll(getInjectionPoints(condition.getBlock()));
                }
            } else if (node.hasBlock()) {
                result.addAll(getInjectionPoints(node.getBlock()));
            }
		}
		return result;
	}

	public void writeVariables(PugModel model, MixinNode mixin, PugConfiguration configuration) {
		List<String> names = mixin.getArguments();
		List<String> values = arguments;
		if (names == null) {
			return;
		}

		for (int i = 0; i < names.size(); i++) {
			String key = names.get(i);
			String valueExpression = mixin.getDefaultValues().get(key);
			Object value = null;
			if (i < values.size()) {
				valueExpression = values.get(i);
			}
			if (valueExpression != null) {
				try {
					value = configuration.getExpressionHandler().evaluateExpression(valueExpression, model);
				} catch (Throwable e) {
					throw new PugCompilerException(this, configuration.getTemplateLoader(), e);
				}
			}
			if (key != null) {
				model.putLocal(key, value);
			}
		}
		if(mixin.getRest()!=null) {
			ArrayList<Object> restArguments = new ArrayList<Object>();
			for (int i = names.size(); i < arguments.size(); i++) {
				Object value = null;
				if (i < values.size()) {
					value = values.get(i);
				}
				if (value != null) {
					try {
						value = configuration.getExpressionHandler().evaluateExpression(values.get(i), model);
					} catch (Throwable e) {
						throw new PugCompilerException(this, configuration.getTemplateLoader(), e);
					}
				}
				restArguments.add(value);
			}
			model.putLocal(mixin.getRest(), restArguments);
		}
	}

	public void writeAttributes(PugModel model, MixinNode mixin, PugConfiguration configuration, boolean terse) {
		LinkedList<Attr> newAttributes = new LinkedList<Attr>(attributes);
		if (!attributeBlocks.isEmpty()) {
			for (String attributeBlock : attributeBlocks) {
			   Object o = null;
			   try {
				   o = configuration.getExpressionHandler().evaluateExpression(attributeBlock, model);
			   } catch (ExpressionException e) {
				   throw new PugCompilerException(this, configuration.getTemplateLoader(), e);
			   }
			   if(o instanceof Map) {
                   for (Map.Entry<String, String> entry : ((Map<String, String>) o).entrySet()) {
                       Attr attr = new Attr(entry.getKey(), entry.getValue(), false);
                       newAttributes.add(attr);
                   }
               }
		   }
  		}

		if (!newAttributes.isEmpty()) {
			Map<String,String> attrs = attrs(model, configuration, newAttributes,terse);
			model.putLocal("attributes", attrs);
  		}else{
			model.putLocal("attributes", new LinkedHashMap<>());
		}

	}

	public List<String> getArguments() {
		return arguments;
	}

	public void setArguments(List<String> arguments) {
		this.arguments = arguments;
	}

	public void setArguments(String arguments) {
		this.arguments.clear();
		this.arguments = ArgumentSplitter.split(arguments);
	}

	public boolean isCall() {
		return call;
	}

	public void setCall(boolean call) {
		this.call = call;
	}
}
