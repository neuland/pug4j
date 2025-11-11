package de.neuland.pug4j.compiler;

import com.google.gson.Gson;
import de.neuland.pug4j.exceptions.ExpressionException;
import de.neuland.pug4j.exceptions.PugCompilerException;
import de.neuland.pug4j.expression.ExpressionHandler;
import de.neuland.pug4j.model.PugModel;
import de.neuland.pug4j.parser.node.Attr;
import de.neuland.pug4j.parser.node.AttrsNode;
import de.neuland.pug4j.parser.node.ExpressionString;
import de.neuland.pug4j.template.TemplateLoader;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;

import java.time.Instant;
import java.util.*;

public class AttributesCompiler {

    private final de.neuland.pug4j.PugEngine engine;
    private static final Gson gson = new Gson();

    public AttributesCompiler(final de.neuland.pug4j.PugEngine engine) {
        this.engine = engine;
    }

    private ExpressionHandler getExpressionHandler() {
        return engine.getExpressionHandler();
    }

    private TemplateLoader getTemplateLoader() {
        return engine.getTemplateLoader();
    }

    protected String visitAttributes(PugModel model, final AttrsNode node, boolean terse) {
        Map<String, String> attrs = getAttributesMap(model, node, terse);
        return attrsToString(attrs);
    }

    public Map<String, String> getAttributesMap(final PugModel model, final AttrsNode node, final boolean terse) {
        LinkedList<Attr> attributesList = new LinkedList<Attr>(node.getAttributes());
        //if attributes block than add to attributes from tag
        if (!node.getAttributeBlocks().isEmpty()) {
            for (String attributeBlockExpression : node.getAttributeBlocks()) {
                addAttributesBlockToAttributesList(model, node, attributeBlockExpression, attributesList);
            }
        }
        Map<String, String> attrs = attrs(model, node, attributesList, terse);
        return attrs;
    }

    private void addAttributesBlockToAttributesList(final PugModel model, final AttrsNode node, final String attributeBlockExpression, final LinkedList<Attr> newAttributes) {
        Object attributesBlock = null;
        try {
            attributesBlock = getExpressionHandler().evaluateExpression(attributeBlockExpression, model);
        } catch (ExpressionException e) {
            throw new PugCompilerException(node, getTemplateLoader(), e);
        }
        if (attributesBlock instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) attributesBlock;
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                //Attributes applied using &attributes are not automatically escaped. You must be sure to sanitize any user inputs to avoid cross-site scripting (XSS). If passing in attributes from a mixin call, this is done automatically.
                Attr attr = new Attr(String.valueOf(entry.getKey()), entry.getValue(), false);
                newAttributes.add(attr);
            }
        } else {
            throw new PugCompilerException(node, getTemplateLoader(), "attribute block '" + attributeBlockExpression + "' is not a Map");
        }
    }

    private String attrsToString(Map<String, String> attrs) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : attrs.entrySet()) {
            sb.append(" ");
            sb.append(entry.getKey());
            if (entry.getValue() != null) {
                sb.append("=").append('"');
                sb.append(entry.getValue());
                sb.append('"');
            }
        }
        return sb.toString();
    }

    private Map<String, String> attrs(PugModel model, final AttrsNode node, LinkedList<Attr> attrs, boolean terse) {
        ArrayList<String> classes = new ArrayList<>();
        ArrayList<Boolean> classEscaping = new ArrayList<>();
        Map<String, String> normalAttributes = new LinkedHashMap<>();

        for (Attr attribute : attrs) {
            createAttributeValues(normalAttributes, classes, classEscaping, attribute, model, node, terse);
        }

        //Put class as the first attribute
        Map<String, String> finalAttributes = new LinkedHashMap<>();
        if (!classes.isEmpty()) {
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

            if (escaped)
                className = StringEscapeUtils.escapeHtml4(classes.get(i));
            else
                className = classes.get(i);

            if (i > 0)
                classList.append(" ");
            classList.append(className);
        }
        return classList.toString();
    }

    private void createAttributeValues(Map<String, String> newAttributes, ArrayList<String> classes, ArrayList<Boolean> classEscaping, Attr attribute, PugModel model, final AttrsNode node, boolean terse) {
        final String name = attribute.getName();
        boolean escaped = attribute.isEscaped();

        String value = null;
        Object attributeValue = attribute.getValue();
        if (attributeValue instanceof ExpressionString) {
            ExpressionString expressionString = (ExpressionString) attributeValue;
            attributeValue = evaluateExpression(expressionString, model, node);
        }

        if (skipAttribute(attributeValue)) {
            return;
        }

        if ("class".equals(name)) {
            addClassValueToClassArray(classes, classEscaping, attributeValue, escaped);
            return;
        } else if ("style".equals(name)) {
            value = renderStyleValue(attributeValue);
        } else {
            value = renderNormalValue(attributeValue, name, terse);
        }

        if (escaped)
            value = StringEscapeUtils.escapeHtml4(value);

        newAttributes.put(name, value);
    }

    private Boolean skipAttribute(final Object attributeValue) {
        boolean skipAttribute = attributeValue == null;
        if (attributeValue instanceof Boolean) {
            if (!(Boolean) attributeValue) {
                skipAttribute = true;
            }
        }
        return skipAttribute;
    }

    private String renderNormalValue(final Object attributeValue, final String name, boolean terse) {
        String value = null;
        if (attributeValue instanceof Boolean) {
            Boolean booleanValue = (Boolean) attributeValue;
            if (booleanValue) {
                value = name;
            }
            if (terse) {
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
        if (attributeValue instanceof List) {
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
        } else if (attributeValue instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) attributeValue;
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                if (entry.getValue() instanceof Boolean) {
                    if (((Boolean) entry.getValue())) {
                        classes.add(entry.getKey());
                        classEscaping.add(false);
                    }
                }
            }
        } else if (attributeValue instanceof Boolean) {
            if ((Boolean) attributeValue) {
                value = attributeValue.toString();
            }
        } else if (attributeValue != null) {
            value = attributeValue.toString();
        }
        if (!StringUtils.isBlank(value)) {
            classes.add(value);
            classEscaping.add(escaped);
        }

    }

    private String renderStyleValue(Object value) {
        if (value instanceof Boolean && !(Boolean) value) {
            return "";
        }
        if (value instanceof Map) {
            StringBuilder out = new StringBuilder();
            Set<Map.Entry<String, String>> entries = ((Map<String, String>) value).entrySet();
            for (Map.Entry<String, String> style : entries) {
                out.append(style.getKey()).append(":").append(style.getValue()).append(";");
            }
            return out.toString();
        } else {
            return String.valueOf(value);
        }
    }

    private Object evaluateExpression(ExpressionString attribute, PugModel model, final AttrsNode node) {
        String expression = attribute.getValue();
        try {
            return getExpressionHandler().evaluateExpression(expression, model);
        } catch (ExpressionException e) {
            throw new PugCompilerException(node, getTemplateLoader(), e);
        }
    }

}
