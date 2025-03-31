package de.neuland.pug4j.parser.node;

public class IfConditionNode extends Node {

    private boolean defaultNode = false;
    private boolean isInverse = false;

    public IfConditionNode(String condition, int lineNumber) {
        this.value = condition;
        this.lineNumber = lineNumber;
    }

    public void setDefault(boolean defaultNode) {
        this.defaultNode = defaultNode;
    }

    public boolean isDefault() {
        return defaultNode;
    }

    public boolean isInverse() {
        return isInverse;
    }

    public void setInverse(boolean isInverse) {
        this.isInverse = isInverse;
    }
}
