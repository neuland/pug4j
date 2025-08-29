package de.neuland.pug4j.parser.node;

public class Attr {
    private final String name;
    private final Object value;
    private final boolean escaped;

    public Attr(final String name, final Object value, final boolean escaped) {
        this.name = name;
        this.value = value;
        this.escaped = escaped;
    }

    public String getName() {
        return name;
    }

    public Object getValue() {
        return value;
    }

    public boolean isEscaped() {
        return escaped;
    }
}
