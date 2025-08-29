package de.neuland.pug4j.parser.node;

public class TextNode extends Node {

    private String value = "";
    boolean isHtml = false;

    public void setValue(final String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public boolean isHtml() {
        return isHtml;
    }

    public void setHtml(boolean html) {
        isHtml = html;
    }
}
