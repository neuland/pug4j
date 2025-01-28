package de.neuland.pug4j.parser.node;

import java.util.HashMap;

public class MixinNode extends CallNode {
	private String rest;
	private HashMap<String,String> defaultValues = new HashMap<String,String>();

	public void setRest(String rest) {
		this.rest = rest;
	}

	public String getRest() {
		return rest;
	}

	public HashMap<String,String> getDefaultValues() {
		return defaultValues;
	}

	public void setDefaultValues(final HashMap<String,String> defaultValues) {
		this.defaultValues = defaultValues;
	}
}
