package de.neuland.pug4j.parser.node;

import java.util.*;

public abstract class AttrsNode extends Node {

    protected LinkedList<Attr> attributes = new LinkedList<>();
    protected LinkedList<String> attributeBlocks = new LinkedList<>();
    protected List<String> attributeNames = new LinkedList<>();

    protected boolean selfClosing = false;
    private boolean textOnly;


    public void setAttribute(String key, Object value, boolean escaped) {
        if (!"class".equals(key) && this.attributeNames.contains(key)) {
            throw new Error("Duplicate attribute '" + key + "' is not allowed.");
        }
        this.attributeNames.add(key);
        Attr attr = new Attr(key, value, escaped);
        this.attributes.add(attr);
    }

    @Override
    public AttrsNode clone() throws CloneNotSupportedException {
        AttrsNode clone = (AttrsNode) super.clone();

        // shallow copy
        if (this.attributes != null) {
            clone.attributes = new LinkedList<>(this.attributes);

        }
        if (this.attributes != null) {
            clone.attributeBlocks = new LinkedList<>(this.attributeBlocks);
        }
        return clone;
    }

    public void addAttributes(String src) {
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

    public LinkedList<Attr> getAttributes() {
        return attributes;
    }

    public LinkedList<String> getAttributeBlocks() {
        return attributeBlocks;
    }
}
