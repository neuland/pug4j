package de.neuland.pug4j.exceptions;

import de.neuland.pug4j.template.TemplateLoader;

public class PugLexerException extends PugException {

    private static final long serialVersionUID = -4390591022593362563L;

    public PugLexerException(String message, String filename, int lineNumber, int column, TemplateLoader templateLoader) {
        super(message, filename, lineNumber, column, templateLoader, null);
    }

    public PugLexerException(String code, String message, String filename, int lineNumber, int column, TemplateLoader templateLoader) {
        this(message, filename, lineNumber, column, templateLoader);
    }
}
