package de.neuland.pug4j.parser.node;

import java.util.*;

import de.neuland.pug4j.compiler.Utils;
import de.neuland.pug4j.exceptions.ExpressionException;
import de.neuland.pug4j.exceptions.PugCompilerException;
import de.neuland.pug4j.expression.ExpressionHandler;
import de.neuland.pug4j.model.PugModel;
import de.neuland.pug4j.template.PugTemplate;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;

public abstract class AttrsNode extends Node {

    private static final String[] bodylessTags = {"area", "base", "br", "col", "embed", "hr", "img", "input", "keygen", "link", "menuitem", "meta", "param", "source", "track", "wbr"};
    protected LinkedList<Attr> attributes = new LinkedList<Attr>();
	protected LinkedList<String> attributeBlocks = new LinkedList<String>();
	protected List<String> attributeNames = new LinkedList<String>();
	protected boolean selfClosing = false;
	protected Node codeNode;
	private boolean textOnly;


	public AttrsNode setAttribute(String key, Object value, boolean escaped) {
		if (!"class".equals(key) && this.attributeNames.indexOf(key) != -1) {
			throw new Error("Duplicate attribute '" + key + "' is not allowed.");
		}
        this.attributeNames.add(key);
        Attr attr = new Attr(key,value,escaped);
        this.attributes.add(attr);
        return this;
	}

	public String getAttribute(String key) {
		for (int i = 0, len = this.attributes.size(); i < len; ++i) {
			if (this.attributes.get(i) != null && this.attributes.get(i).getName().equals(key)) {
				return attributeValueToString(this.attributes.get(i).getValue());
			}
		}
		return null;
	}

	private String attributeValueToString(Object value) {
		if (value instanceof ExpressionString) {
			String expression = ((ExpressionString) value).getValue();
			return "#{" + expression + "}";
		}
		return value.toString();
	}
//	protected Map<String, Object> mergeInheritedAttributes(JadeModel model) {
//		List<Attr> mergedAttributes = this.attributes;
//
//		if (inheritsAttributes) {
//			Object o = model.get("attributes");
//			if (o != null && o instanceof Map) {
//				@SuppressWarnings("unchecked")
//				Map<String, Object> inheritedAttributes = (Map<String, Object>) o;
//
//				for (Entry<String, Object> entry : inheritedAttributes.entrySet()) {
//					setAttribute(mergedAttributes, (String) entry.getKey(), entry.getValue());
//				}
//			}
//		}
//		return mergedAttributes;
//	}

	@Override
	public AttrsNode clone() throws CloneNotSupportedException {
		AttrsNode clone = (AttrsNode) super.clone();

        // shallow copy
		if (this.attributes != null) {
			clone.attributes = new LinkedList<Attr>(this.attributes);

		}
        if (this.attributes != null) {
            clone.attributeBlocks = new LinkedList<String>(this.attributeBlocks);
        }
		return clone;
	}
	public void addAttributes(String src){
		this.attributeBlocks.add(src);
	}

	public void setSelfClosing(boolean selfClosing) {
        this.selfClosing = selfClosing;
    }

	public boolean isSelfClosing() {
        return selfClosing;
    }

	public void setTextOnly(boolean textOnly) {
        this.textOnly = textOnly;

    }

	public boolean isTextOnly() {
        return this.textOnly;
    }

	public void setCodeNode(Node codeNode) {
        this.codeNode = codeNode;
    }

    public Node getCodeNode() {
        return codeNode;
    }

    public boolean hasCodeNode() {
        return codeNode != null;
    }

	protected String visitAttributes(PugModel model, PugTemplate template) {
        LinkedList<Attr> newAttributes = new LinkedList<Attr>(attributes);
        if(attributeBlocks.size()>0){
            //Todo: AttributesBlock needs to be evaluated
            for (String attributeBlock : attributeBlocks) {
                Object o = null;
                try {
                    o = template.getExpressionHandler().evaluateExpression(attributeBlock, model);
                } catch (ExpressionException e) {
                    e.printStackTrace();
                }
                 if(o instanceof Map) {
                    Map<String, String> map = (Map<String, String>) o;
                    for (Map.Entry<String, String> entry : map.entrySet()) {
                        Attr attr = new Attr(String.valueOf(entry.getKey()),entry.getValue(),false);
                        newAttributes.add(attr);
                    }
                }
                if(o instanceof ArrayList){
                    ArrayList<Object> list = (ArrayList<Object>) o;
                    for (Object o1 : list) {

                    }
                }

            }
            LinkedHashMap<String,String> attrs = attrs(model, template,newAttributes);
            return attrsToString(attrs, template);
        }else{
            LinkedHashMap<String,String> attrs = attrs(model, template, newAttributes);
            return attrsToString(attrs, template);
        }


    }

    private String attrsToString(LinkedHashMap<String, String> attrs, PugTemplate template) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : attrs.entrySet()) {
            sb.append(" ");
            sb.append(entry.getKey());
            if(entry.getValue() != null) {
                sb.append("=").append('"');
                sb.append(entry.getValue());
                sb.append('"');
            }
        }
        return sb.toString();
    }

    protected LinkedHashMap<String,String> attrs(PugModel model, PugTemplate template, LinkedList<Attr> attrs) {
        ArrayList<String> classes = new ArrayList<String>();
        ArrayList<Boolean> classEscaping = new ArrayList<Boolean>();
        LinkedHashMap<String,String> newAttributes = new LinkedHashMap<String,String>();
        for (Attr attribute : attrs) {
            try {
                addAttributesToMap(newAttributes,classes,classEscaping, attribute, model, template);
            } catch (ExpressionException e) {
                throw new PugCompilerException(this, template.getTemplateLoader(), e);
            }
        }
        LinkedHashMap<String,String> finalAttributes = new LinkedHashMap<String,String>();
        if(!classes.isEmpty()){
            finalAttributes.put("class", StringUtils.join(classes," "));
        }
        finalAttributes.putAll(newAttributes);
        return finalAttributes;
    }

    private void addAttributesToMap(HashMap<String, String> newAttributes, ArrayList<String> classes, ArrayList<Boolean> classEscaping, Attr attribute, PugModel model, PugTemplate template) throws ExpressionException {
        String name = attribute.getName();
        boolean escaped = attribute.isEscaped();

        String value = null;
        Object attributeValue = attribute.getValue();
        if("class".equals(name)) {
            if (attributeValue instanceof String) {
                escaped = attribute.isEscaped();
                value = getInterpolatedAttributeValue(name, attributeValue,escaped, model, template);
            } else if (attributeValue instanceof ExpressionString) {
                escaped = ((ExpressionString) attributeValue).isEscape();
                Object expressionValue = evaluateExpression((ExpressionString) attributeValue, model,template.getExpressionHandler());

                //List to String
                if (expressionValue != null && expressionValue instanceof List){
                    StringBuffer s = new StringBuffer("");
                    List list = (List) expressionValue;

                    boolean first = true;
                    for (Object o : list) {
                        if (!first)
                            s.append(" ");
                        s.append(o.toString());
                        first = false;
                    }
                    value = s.toString();
                }
                //Array to String
                else if (expressionValue != null && expressionValue.getClass().isArray()) {
                    StringBuffer s = new StringBuffer("");
                    boolean first = true;
                    if (expressionValue instanceof int[]) {
                        for (int o : (int[]) expressionValue) {
                            if (!first)
                                s.append(" ");
                            s.append(o);
                            first = false;
                        }
                    } else {
                        for (Object o : (Object[]) expressionValue) {
                            if (!first)
                                s.append(" ");
                            s.append(o.toString());
                            first = false;
                        }
                    }
                    value = s.toString();
                }else if (expressionValue != null && expressionValue instanceof Map) {
                    Map<String,Object> map = (Map<String,Object>) expressionValue;
                    for (Map.Entry<String,Object> entry : map.entrySet()) {
                        if(entry.getValue() instanceof Boolean){
                            if(((Boolean) entry.getValue()) == true){
                                classes.add(entry.getKey());
                                classEscaping.add(false);
                            }
                        }
                    }
                }else if(expressionValue!=null && expressionValue instanceof Boolean){
                    if((Boolean) expressionValue)
                        value = expressionValue.toString();
                }else if(expressionValue!=null){
                    value = expressionValue.toString();
                }
            }
            if(!StringUtils.isBlank(value)) {
                classes.add(value);
                classEscaping.add(escaped);
            }
            return;
        } else if (attributeValue instanceof ExpressionString) {
//            isConstant
            ExpressionString expressionString = (ExpressionString) attributeValue;
            escaped = expressionString.isEscape();
            Object expressionValue = evaluateExpression(expressionString, model, template.getExpressionHandler());
            if (expressionValue == null) {
                return;
            }
            // TODO: refactor
            if (expressionValue instanceof Boolean) {
                Boolean booleanValue = (Boolean) expressionValue;
                if (booleanValue) {
                    value = name;
                } else {
                    return;
                }
                if (template.isTerse()) {
                    value = null;
                }
            }else{
                value = expressionValue.toString();
                if(escaped)
                    value = StringEscapeUtils.escapeHtml4(value);
            }
        }else if (attributeValue instanceof String) {
            escaped = attribute.isEscaped();
            value = getInterpolatedAttributeValue(name, attributeValue, escaped, model, template);
        } else if (attributeValue instanceof Boolean) {
            Boolean booleanValue = (Boolean) attributeValue;
            if (booleanValue) {
                value = name;
            } else {
                return;
            }
            if (template.isTerse()) {
                value = null;
            }
        }
        newAttributes.put(name,value);
    }
	private Object evaluateExpression(ExpressionString attribute, PugModel model, ExpressionHandler expressionHandler) throws ExpressionException {
        String expression = ((ExpressionString) attribute).getValue();
	    Object result = expressionHandler.evaluateExpression(expression, model);
        if (result instanceof ExpressionString) {
            return evaluateExpression((ExpressionString) result, model, expressionHandler);
        }
        return result;
    }

	private String getInterpolatedAttributeValue(String name, Object attribute, boolean escaped, PugModel model, PugTemplate template)
            throws PugCompilerException {
//        if (!preparedAttributeValues.containsKey(name)) {
//            preparedAttributeValues.put(name, Utils.prepareInterpolate((String) attribute, escaped));
//        }
        List<Object> prepared = Utils.prepareInterpolate((String) attribute, escaped);
        try {
            return Utils.interpolate(prepared, model,template.getExpressionHandler());
        } catch (ExpressionException e) {
            throw new PugCompilerException(this, template.getTemplateLoader(), e);
        }
    }

    protected boolean isBodyless() {
        return ArrayUtils.contains(bodylessTags, name);
    }

}
