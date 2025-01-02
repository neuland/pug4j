package de.neuland.pug4j;

import java.io.*;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Map;

import de.neuland.pug4j.exceptions.PugCompilerException;
import de.neuland.pug4j.expression.ExpressionHandler;
import de.neuland.pug4j.expression.JexlExpressionHandler;
import de.neuland.pug4j.model.PugModel;
import de.neuland.pug4j.parser.Parser;
import de.neuland.pug4j.parser.node.Node;
import de.neuland.pug4j.template.FileTemplateLoader;
import de.neuland.pug4j.template.PugTemplate;
import de.neuland.pug4j.template.ReaderTemplateLoader;
import de.neuland.pug4j.template.TemplateLoader;
import org.apache.commons.io.FilenameUtils;

public class Pug4J {

	public enum Mode {
		HTML, XML, XHTML
	}

	public static String render(String filename, Map<String, Object> model) throws IOException, PugCompilerException {
		return render(filename, model, false);
	}

	public static String render(String filename, Map<String, Object> model, boolean pretty) throws IOException, PugCompilerException {
		PugTemplate template = getTemplate(filename);
		template.setPrettyPrint(pretty);
		return templateToString(template, model);
	}

	public static void render(String filename, Map<String, Object> model, Writer writer) throws IOException, PugCompilerException {
		render(filename, model, writer, false);
	}

	public static void render(String filename, Map<String, Object> model, Writer writer, boolean pretty) throws IOException,
			PugCompilerException {
		PugTemplate template = getTemplate(filename);
		render(template,model,writer,pretty);
	}

	public static String render(PugTemplate template, Map<String, Object> model) throws PugCompilerException {
		return render(template, model, false);
	}

	public static String render(PugTemplate template, Map<String, Object> model, boolean pretty) throws PugCompilerException {
		template.setPrettyPrint(pretty);
		return templateToString(template, model);
	}

	public static void render(PugTemplate template, Map<String, Object> model, Writer writer) throws PugCompilerException {
		render(template, model, writer, false);
	}

	public static void render(PugTemplate template, Map<String, Object> model, Writer writer, boolean pretty) throws PugCompilerException {
		template.setPrettyPrint(pretty);
		template.process(new PugModel(model), writer);
	}

    public static String render(URL url, Map<String, Object> model) throws IOException, PugCompilerException {
        return render(url, model, false);
    }

    public static String render(URL url, Map<String, Object> model, boolean pretty) throws IOException, PugCompilerException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
        PugTemplate template = getTemplate(reader, url.getPath());
        return render(template, model, pretty);
    }

    public static String render(Reader reader, String filename, Map<String, Object> model) throws IOException, PugCompilerException {
        return render(reader, filename, model, false);
    }

    public static String render(Reader reader, String filename, Map<String, Object> model, boolean pretty) throws IOException, PugCompilerException {
        PugTemplate template = getTemplate(reader, filename);
        return render(template, model, pretty);
    }

	public static PugTemplate getTemplate(String filename) throws IOException {
		if(filename==null){
			throw new IllegalArgumentException("Filename can not be null");
		}
		String prefix = FilenameUtils.getFullPath(filename);
		String filePath = FilenameUtils.getName(filename);
		FileTemplateLoader loader = new FileTemplateLoader(prefix,Charset.forName("UTF-8"));
		return createTemplate(filePath, loader, new JexlExpressionHandler());
	}

	public static PugTemplate getTemplate(String filename, String extension) throws IOException {
		if(filename==null){
			throw new IllegalArgumentException("Filename can not be null");
		}
		String prefix = FilenameUtils.getFullPath(filename);
		String filePath = FilenameUtils.getName(filename);
		FileTemplateLoader loader = new FileTemplateLoader(prefix,Charset.forName("UTF-8"), extension);
		return createTemplate(filePath, loader, new JexlExpressionHandler());
	}

	private static PugTemplate getTemplate(Reader reader, String name) throws IOException {
		return createTemplate(name, new ReaderTemplateLoader(reader, name), new JexlExpressionHandler());
	}

	private static PugTemplate createTemplate(String filename, TemplateLoader loader, ExpressionHandler expressionHandler) throws IOException {
		Parser parser = new Parser(filename, loader, expressionHandler);
		Node root = parser.parse();
		PugTemplate template = new PugTemplate(root);
		template.setExpressionHandler(expressionHandler);
		template.setTemplateLoader(loader);
		return template;
	}

	private static String templateToString(PugTemplate template, Map<String, Object> model) throws PugCompilerException {
		PugModel pugModel = new PugModel(model);
		StringWriter writer = new StringWriter();
		template.process(pugModel, writer);
		return writer.toString();
	}
}