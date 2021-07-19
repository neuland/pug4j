package de.neuland.pug4j.filter;

import java.util.Map;

public class CDATAFilter implements Filter {

	@Override
	public String convert(String source, Map<String, Object> attributes, Map<String, Object> model) {
		return "<![CDATA[" + source + "]]>";
	}

}
