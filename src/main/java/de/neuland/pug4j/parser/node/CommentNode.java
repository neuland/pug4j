package de.neuland.pug4j.parser.node;

public class CommentNode extends Node {
    private boolean buffered;

    public boolean isBuffered() {
        return buffered;
    }

    public void setBuffered(boolean buffered) {
        this.buffered = buffered;
    }

}
