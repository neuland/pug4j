package de.neuland.pug4j.parser.node;

import java.time.Instant;
import java.util.*;

import com.google.gson.Gson;
import de.neuland.pug4j.exceptions.ExpressionException;
import de.neuland.pug4j.exceptions.PugCompilerException;
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
        LinkedList<Attr> attributesList = new LinkedList<Attr>(attributes);
        //if attributes block than add to attributes from tag
        if(attributeBlocks.size()>0) {
            for (String attributeBlockExpression : attributeBlocks) {
                addAttributesBlockToAttributesList(model, template, attributeBlockExpression, attributesList);
            }
        }
        Map<String,String> attrs = attrs(model, template, attributesList);
        return attrsToString(attrs);
    }

    private void addAttributesBlockToAttributesList(final PugModel model, final PugTemplate template, final String attributeBlockExpression, final LinkedList<Attr> newAttributes) {
        Object attributesBlock = null;
        try {
            attributesBlock = template.getExpressionHandler().evaluateExpression(attributeBlockExpression, model);
        } catch (ExpressionException e) {
            throw new PugCompilerException(this, template.getTemplateLoader(), e);
        }
        if (attributesBlock instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) attributesBlock;
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                //Attributes applied using &attributes are not automatically escaped. You must be sure to sanitize any user inputs to avoid cross-site scripting (XSS). If passing in attributes from a mixin call, this is done automatically.
                Attr attr = new Attr(String.valueOf(entry.getKey()), entry.getValue(), false);
                newAttributes.add(attr);
            }
        }else{
            throw new PugCompilerException(this, template.getTemplateLoader(), "attribute block '" + attributeBlockExpression + "' is not a Map");
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
        Map<String,String> normalAttributes = new LinkedHashMap<>();

        for (Attr attribute : attrs) {
            createAttributeValues(normalAttributes, classes, classEscaping, attribute, model, template);
        }

        //Put class as the first attribute
        Map<String,String> finalAttributes = new LinkedHashMap<>();
        if(!classes.isEmpty()){
            final String classList = renderClassList(classes, classEscaping);
            finalAttributes.put("class", classList);
        }
        finalAttributes.putAll(normalAttributes);
        return finalAttributes;
    }

    private String renderClassList(final ArrayList<String> classes, final ArrayList<Boolean> classEscaping) {
        final StringBuilder classList = new StringBuilder();
        for (int i = 0; i < classes.size(); i++) {
            final String className;
            final Boolean escaped = classEscaping.get(i);

            if(escaped)
                className = StringEscapeUtils.escapeHtml4(classes.get(i));
            else
                className = classes.get(i);

            if(i>0)
                classList.append(" ");
            classList.append(className);
        }
        return classList.toString();
    }

    private void createAttributeValues(Map<String, String> newAttributes, ArrayList<String> classes, ArrayList<Boolean> classEscaping, Attr attribute, PugModel model, PugTemplate template)  {
        final String name = attribute.getName();
        boolean escaped = attribute.isEscaped();

        String value = null;
        Object attributeValue = attribute.getValue();
        if(attributeValue instanceof ExpressionString){
            ExpressionString expressionString = (ExpressionString) attributeValue;
            attributeValue = evaluateExpression(expressionString, model, template);
        }

        if(skipAttribute(attributeValue)){
            return;
        }

        if("class".equals(name)) {
            addClassValueToClassArray(classes, classEscaping, attributeValue, escaped);
            return;
        } else if("style".equals(name)){
            value = renderStyleValue(attributeValue);
        } else {
            value = renderNormalValue(template, attributeValue, name);
        }

        if(escaped)
            value = StringEscapeUtils.escapeHtml4(value);

        newAttributes.put(name,value);
    }

    private Boolean skipAttribute(final Object attributeValue) {
        Boolean skipAttribute = false;
        if(attributeValue == null){
            skipAttribute = true;
        }
        if(attributeValue instanceof Boolean){
            if (!(Boolean) attributeValue) {
                skipAttribute = true;
            }
        }
        return skipAttribute;
    }

    private String renderNormalValue(final PugTemplate template, final Object attributeValue, final String name) {
        String value=null;
        if (attributeValue instanceof Boolean) {
            Boolean booleanValue = (Boolean) attributeValue;
            if (booleanValue) {
                value = name;
            }
            if (template.isTerse()) {
                value = null;
            }
        } else if (attributeValue instanceof Instant) {
            Instant instantValue = (Instant) attributeValue;
            value = instantValue.toString();
        } else if (attributeValue != null && (
                attributeValue.getClass().isArray()
                        || attributeValue instanceof Map
                        || attributeValue instanceof List)
        ) {
            value = StringEscapeUtils.unescapeJava(gson.toJson(attributeValue));
        } else if (attributeValue instanceof String) {
            value = (String) attributeValue;
        } else if (attributeValue != null) {
            value = attributeValue.toString();
        }
        return value;
    }

    private void addClassValueToClassArray(final ArrayList<String> classes, final ArrayList<Boolean> classEscaping, final Object attributeValue, final boolean escaped) {
        //List to String
        String value = null;
        if (attributeValue instanceof List){
            List list = (List) attributeValue;
            for (Object o : list) {
                classes.add(o.toString());
                classEscaping.add(escaped);
            }
        }
        //Array to String
        else if (attributeValue != null && attributeValue.getClass().isArray()) {
            if (attributeValue instanceof int[]) {
                for (int o : (int[]) attributeValue) {
                    classes.add(String.valueOf(o));
                    classEscaping.add(escaped);
                }
            } else {
                for (Object o : (Object[]) attributeValue) {
                    classes.add(o.toString());
                    classEscaping.add(escaped);
                }
            }
        }else if (attributeValue instanceof Map) {
            Map<String,Object> map = (Map<String,Object>) attributeValue;
            for (Map.Entry<String,Object> entry : map.entrySet()) {
                if(entry.getValue() instanceof Boolean){
                    if(((Boolean) entry.getValue())){
                        classes.add(entry.getKey());
                        classEscaping.add(false);
                    }
                }
            }
        }else if(attributeValue instanceof Boolean){
            if((Boolean) attributeValue) {
                value = attributeValue.toString();
            }
        }else if(attributeValue !=null){
            value = attributeValue.toString();
        }
        if (!StringUtils.isBlank(value)) {
            classes.add(value);
            classEscaping.add(escaped);
        }

    }

    private String renderStyleValue(Object value) {
        if(value instanceof Boolean && !(Boolean) value){
            return "";
        }
        if(value instanceof Map){
            StringBuilder out = new StringBuilder();
            Set<Map.Entry<String, String>> entries = ((Map<String, String>) value).entrySet();
            for (Map.Entry<String, String> style : entries) {
                out.append(style.getKey()).append(":").append(style.getValue()).append(";");
            }
            return out.toString();
        }else{
            return String.valueOf(value);
        }
    }

    private Object evaluateExpression(ExpressionString attribute, PugModel model, PugTemplate pugTemplate) {
        String expression = attribute.getValue();
        try {
            return pugTemplate.getExpressionHandler().evaluateExpression(expression, model);
        } catch (ExpressionException e) {
            throw new PugCompilerException(this, pugTemplate.getTemplateLoader(), e);
        }
    }

    protected boolean isSelfClosingTag() {
        return ArrayUtils.contains(selfClosingTags, name);
    }

    public LinkedList<Attr> getAttributes() {
        return attributes;
    }
}
