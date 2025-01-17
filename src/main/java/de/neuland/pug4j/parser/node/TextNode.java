package de.neuland.pug4j.parser.node;

import de.neuland.pug4j.compiler.IndentWriter;
import de.neuland.pug4j.exceptions.PugCompilerException;
import de.neuland.pug4j.model.PugModel;
import de.neuland.pug4j.template.PugTemplate;

public class TextNode extends Node {

	private String value = "";
	boolean isHtml = false;

	public void setValue(final String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}

	@Override
	public void execute(IndentWriter writer, PugModel model, PugTemplate template) throws PugCompilerException {
		writer.append(value);
	}

	public boolean isHtml() {
		return isHtml;
	}

	public void setHtml(boolean html) {
		isHtml = html;
	}
}
