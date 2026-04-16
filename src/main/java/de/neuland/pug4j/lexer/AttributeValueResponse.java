package de.neuland.pug4j.lexer;

public record AttributeValueResponse(String value, boolean mustEscape, String remainingSource) {}
