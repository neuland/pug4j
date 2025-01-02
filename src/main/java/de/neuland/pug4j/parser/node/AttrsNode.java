package de.neuland.pug4j.parser.node;

import java.time.Instant;
import java.util.*;

import com.google.gson.Gson;
import de.neuland.pug4j.exceptions.ExpressionException;
import de.neuland.pug4j.exceptions.PugCompilerException;
import de.neuland.pug4j.expression.ExpressionHandler;
import de.neuland.pug4j.model.PugModel;
import de.neuland.pug4j.template.PugTemplate;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;

public abstract class AttrsNode extends Node {

    private static final String[] selfClosingTags = {"area", "base", "br", "col", "embed", "hr", "img", "input", "keygen", "link", "menuitem", "meta", "param", "source", "track", "wbr"};
    protected LinkedList<Attr> attributes = new LinkedList<Attr>();
	protected LinkedList<String> attributeBlocks = new LinkedList<String>();
	protected List<String> attributeNames = new LinkedList<String>();
	protected boolean selfClosing = false;
	protected Node codeNode;
	private boolean textOnly;
    private static Gson gson = new Gson();


    public AttrsNode setAttribute(String key, Object value, boolean escaped) {
		if (!"class".equals(key) && this.attributeNames.indexOf(key) != -1) {
			throw new Error("Duplicate attribute '" + key + "' is not allowed.");
		}
        this.attributeNames.add(key);
        Attr attr = new Attr(key,value,escaped);
        this.attributes.add(attr);
        return this;
	}

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
            for (String attributeBlock : attributeBlocks) {
                Object o = null;
                try {
                    o = template.getExpressionHandler().evaluateExpression(attributeBlock, model);
                } catch (ExpressionException e) {
                    throw new PugCompilerException(this, template.getTemplateLoader(), e);
                }
                if(o instanceof Map) {
                    Map<String, String> map = (Map<String, String>) o;
                    for (Map.Entry<String, String> entry : map.entrySet()) {
                        Attr attr = new Attr(String.valueOf(entry.getKey()),entry.getValue(),false);
                        newAttributes.add(attr);
                    }
                }
            }
            Map<String,String> attrs = attrs(model, template,newAttributes);
            return attrsToString(attrs);
        }else{
            Map<String,String> attrs = attrs(model, template, newAttributes);
            return attrsToString(attrs);
        }
    }

    private String attrsToString(Map<String, String> attrs) {
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

    protected Map<String,String> attrs(PugModel model, PugTemplate template, LinkedList<Attr> attrs) {
        ArrayList<String> classes = new ArrayList<>();
        ArrayList<Boolean> classEscaping = new ArrayList<>();
        Map<String,String> newAttributes = new LinkedHashMap<>();

        for (Attr attribute : attrs) {
            try {
                addAttributesToMap(newAttributes,classes,classEscaping, attribute, model, template);
            } catch (ExpressionException e) {
                throw new PugCompilerException(this, template.getTemplateLoader(), e);
            }
        }

        Map<String,String> finalAttributes = new LinkedHashMap<>();
        if(!classes.isEmpty()){
            String out = "";
            for (int i = 0; i < classes.size(); i++) {
                String classname;
                if(classEscaping.get(i))
                    classname = StringEscapeUtils.escapeHtml4(classes.get(i));
                else
                    classname = classes.get(i);

                if(i==0)
                    out = classname;
                else
                    out = out + " " + classname;
            }
            finalAttributes.put("class", out);
        }
        finalAttributes.putAll(newAttributes);
        return finalAttributes;
    }

    private void addAttributesToMap(Map<String, String> newAttributes, ArrayList<String> classes, ArrayList<Boolean> classEscaping, Attr attribute, PugModel model, PugTemplate template) throws ExpressionException {
        String name = attribute.getName();
        boolean escaped = attribute.isEscaped();

        String value = null;
        Object attributeValue = attribute.getValue();
        if("class".equals(name)) {
            if (attributeValue instanceof String) {
                value = (String) attributeValue;
            } else if (attributeValue instanceof ExpressionString) {
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
                    if((Boolean) expressionValue) {
                        value = expressionValue.toString();
                    }
                }else if(expressionValue!=null){
                    value = expressionValue.toString();
                }
            }
            if(!StringUtils.isBlank(value)) {
                classes.add(value);
                classEscaping.add(escaped);
            }
            return;
        } else {
            if("style".equals(name)){
                if (attributeValue instanceof ExpressionString) { //isConstant
                    ExpressionString expressionString = (ExpressionString) attributeValue;
                    Object expressionValue = evaluateExpression(expressionString, model, template.getExpressionHandler());
                    if (expressionValue == null) {
                        return;
                    }
                    attributeValue = style(expressionValue);
                } else {
                    attributeValue = style(attributeValue);
                }
            }
            if (attributeValue instanceof ExpressionString) {
                ExpressionString expressionString = (ExpressionString) attributeValue;
                Object expressionValue = evaluateExpression(expressionString, model, template.getExpressionHandler());
                if (expressionValue == null) {
                    return;
                }

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
                } else if (expressionValue instanceof Instant) {
                    Instant instantValue = (Instant) expressionValue;
                    value = instantValue.toString();
                } else if (
                    expressionValue.getClass().isArray()
                    || expressionValue instanceof Map
                    || expressionValue instanceof List
                ) {
                    value = StringEscapeUtils.unescapeJava(gson.toJson(expressionValue));
                }else{
                    value = expressionValue.toString();
                }
            }else if (attributeValue instanceof String) {
                value = (String) attributeValue;
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
        }
        if(escaped)
            value = StringEscapeUtils.escapeHtml4(value);
        newAttributes.put(name,value);
    }

    private String style(Object value) {
        if(value instanceof Boolean && !(Boolean) value){
            return "";
        }
        if(value instanceof Map){
            String out = "";
            Set<Map.Entry<String, String>> entries = ((Map<String, String>) value).entrySet();
            for (Map.Entry<String, String> style : entries) {
                out = out + style.getKey() + ":" + style.getValue()+";";
            }
            return out;
        }else{
            return String.valueOf(value);
        }
    }

    private Object evaluateExpression(ExpressionString attribute, PugModel model, ExpressionHandler expressionHandler) throws ExpressionException {
        String expression = attribute.getValue();
	    Object result = expressionHandler.evaluateExpression(expression, model);
        if (result instanceof ExpressionString) {
            return evaluateExpression((ExpressionString) result, model, expressionHandler);
        }
        return result;
    }

    protected boolean isSelfClosingTag() {
        return ArrayUtils.contains(selfClosingTags, name);
    }

    public LinkedList<Attr> getAttributes() {
        return attributes;
    }
}
