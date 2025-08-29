package de.neuland.pug4j.exceptions;

import de.neuland.pug4j.template.TemplateLoader;

public class PugParserException extends PugException {

    private static final long serialVersionUID = 2022663314591205451L;
    String code = "";

    public PugParserException(String filename, int lineNumber, int column, TemplateLoader templateLoader, String message) {
        super(message, filename, lineNumber, column, templateLoader, null);
    }

    public PugParserException(String filename, int lineNumber, int column, TemplateLoader templateLoader, String message, String code) {
        super(message, filename, lineNumber, column, templateLoader, null);
        this.code = code;
    }

}
