package de.neuland.pug4j.util;


public class CharacterParserException  extends RuntimeException {
    private Integer index;
    private String code;

    /**
     * Constructs a new runtime exception with the specified detail message.
     * The cause is not initialized, and may subsequently be initialized by a
     * call to {@link #initCause}.
     *
     * @param message the detail message. The detail message is saved for
     *                later retrieval by the {@link #getMessage()} method.
     */
    public CharacterParserException(String message) {
        super(message);
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public Integer getIndex() {
        return index;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
