package de.neuland.pug4j.lexer.token;

/**
 * Created by christoph on 04.03.16.
 */
public class Attribute extends Token {
    String name;
    Object attributeValue;
    boolean mustEscape;

    public Attribute() {
    }

    public Attribute(String name, Object attributeValue, boolean mustEscape) {
        this.name = name;
        this.attributeValue = attributeValue;
        this.mustEscape = mustEscape;
    }

    public String getName() {
        return name;
    }

    public Object getAttributeValue() {
        return attributeValue;
    }

    public boolean mustEscape() {
        return mustEscape;
    }

    public void setMustEscape(boolean mustEscape) {
        this.mustEscape = mustEscape;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    public void setAttributeValue(Object attributeValue) {
        this.attributeValue = attributeValue;
    }
}
