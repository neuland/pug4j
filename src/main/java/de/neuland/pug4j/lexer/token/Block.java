package de.neuland.pug4j.lexer.token;

public class Block extends Token {

    public Block(String value) {
        super(value);
    }

    public Block(String value, int lineNumber) {
        super(value, lineNumber);
    }

    public Block() {

    }
}
