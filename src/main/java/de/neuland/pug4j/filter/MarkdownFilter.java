package de.neuland.pug4j.filter;

import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;

import java.util.Map;

public class MarkdownFilter extends CachingFilter {

    private final Parser parser = Parser.builder().build();
    private final HtmlRenderer renderer = HtmlRenderer.builder().build();

	@Override
	protected String convert(String source, Map<String, Object> attributes) {
		return renderer.render(parser.parse(source)).trim();
	}

}
