package de.neuland.pug4j.lexer.token;


public class Tag extends Token {
    public Tag(String value) {
        super(value);
    }

    public Tag(String value, int lineNumber) {
        super(value, lineNumber);
    }

}
