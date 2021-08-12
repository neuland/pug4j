package de.neuland.pug4j.compiler;

public class InterpolatedString {
    private String value = null;
    private boolean escape = false;

    public boolean isEscape() {
        return escape;
    }

    public InterpolatedString(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public void setEscape(boolean escape) {
        this.escape  = escape;
    }
}
