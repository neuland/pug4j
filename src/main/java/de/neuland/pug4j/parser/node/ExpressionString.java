package de.neuland.pug4j.parser.node;

public class ExpressionString {
	private String value = null;

	public ExpressionString(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}
}
