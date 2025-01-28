package de.neuland.pug4j.parser.node;

public class BlockCommentNode extends Node {
    private boolean buffered;

    public boolean isBuffered() {
        return buffered;
    }

    public void setBuffered(boolean buffered) {
        this.buffered = buffered;
    }
}
