package de.neuland.pug4j.compiler;

import java.io.StringWriter;
import java.io.Writer;

import de.neuland.pug4j.exceptions.PugCompilerException;
import de.neuland.pug4j.model.PugModel;
import de.neuland.pug4j.parser.node.Node;
import de.neuland.pug4j.template.PugTemplate;

public class Compiler {

	private PugTemplate template;

	public Compiler(PugTemplate pugTemplate) {
		this.template = pugTemplate;
	}
	//@TODO: Deprecated: Remove in 3.0.0
	public Compiler(Node rootNode) {
		template = new PugTemplate(rootNode);
	}

	public String compileToString(PugModel model) throws PugCompilerException {
		StringWriter writer = new StringWriter();
		compile(model, writer);
		return writer.toString();
	}

	public void compile(PugModel model, Writer w) throws PugCompilerException {
		IndentWriter writer = new IndentWriter(w);
		writer.setUseIndent(template.isPrettyPrint());
		template.getRootNode().execute(writer, model, template);
	}

	//@TODO: Deprecated: Remove in 3.0.0
	public void setPrettyPrint(boolean prettyPrint) {
		this.template.setPrettyPrint(prettyPrint);
	}

	//@TODO: Deprecated: Remove in 3.0.0
	public void setTemplate(PugTemplate pugTemplate) {
		this.template = pugTemplate;
	}
}