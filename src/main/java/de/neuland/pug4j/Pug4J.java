package de.neuland.pug4j;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import de.neuland.pug4j.exceptions.PugCompilerException;
import de.neuland.pug4j.model.PugModel;
import de.neuland.pug4j.parser.Parser;
import de.neuland.pug4j.parser.node.Node;
import de.neuland.pug4j.template.FileTemplateLoader;
import de.neuland.pug4j.template.PugTemplate;
import de.neuland.pug4j.template.ReaderTemplateLoader;
import org.apache.commons.io.FilenameUtils;

public class Pug4J {

    public enum Mode {
        HTML, XML, XHTML
    }

    public static String render(String filename, Map<String, Object> model) throws IOException, PugCompilerException {
        return render(filename, model, false);
    }

    public static String render(String filename, Map<String, Object> model, boolean pretty) throws IOException, PugCompilerException {
        final StringWriter writer = new StringWriter();
        render(filename,model,writer,pretty);
        return writer.toString();
    }

    public static void render(String filename, Map<String, Object> model, Writer writer) throws IOException, PugCompilerException {
        render(filename, model, writer, false);
    }

    public static void render(String filename, Map<String, Object> model, Writer writer, boolean pretty) throws IOException,
            PugCompilerException {
        final PugConfiguration pugConfiguration = new PugConfiguration();
        pugConfiguration.setPrettyPrint(pretty);
        PugTemplate template = getTemplate(filename,pugConfiguration);
        template.process(new PugModel(model), writer, pugConfiguration);
    }

    public static String render(PugTemplate template, Map<String, Object> model) throws PugCompilerException {
        return render(template, model, false);
    }

    public static String render(PugTemplate template, Map<String, Object> model, boolean pretty) throws PugCompilerException {
        final PugConfiguration pugConfiguration = new PugConfiguration();
        pugConfiguration.setPrettyPrint(pretty);
        return templateToString(template, model, pugConfiguration);
    }

    public static void render(PugTemplate template, Map<String, Object> model, Writer writer) throws PugCompilerException {
        render(template, model, writer, false);
    }

    public static void render(PugTemplate template, Map<String, Object> model, Writer writer, boolean pretty) throws PugCompilerException {
        final PugConfiguration pugConfiguration = new PugConfiguration();
        pugConfiguration.setPrettyPrint(pretty);
        template.process(new PugModel(model), writer, pugConfiguration);
    }

    public static String render(URL url, Map<String, Object> model) throws IOException, PugCompilerException {
        return render(url, model, false);
    }

    public static String render(URL url, Map<String, Object> model, boolean pretty) throws IOException, PugCompilerException {
        PugConfiguration pugConfiguration = new PugConfiguration();
        pugConfiguration.setPrettyPrint(pretty);
        BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
        PugTemplate template = getTemplate(reader, url.getPath(),pugConfiguration);
        return templateToString(template, model, pugConfiguration);
    }

    public static String render(Reader reader, String filename, Map<String, Object> model) throws IOException, PugCompilerException {
        return render(reader, filename, model, false);
    }

    public static String render(Reader reader, String filename, Map<String, Object> model, boolean pretty) throws IOException, PugCompilerException {
        PugConfiguration pugConfiguration = new PugConfiguration();
        pugConfiguration.setPrettyPrint(pretty);
        PugTemplate template = getTemplate(reader, filename, pugConfiguration);
        return templateToString(template, model, pugConfiguration);
    }

    public static PugTemplate getTemplate(String filename) throws IOException {
        return getTemplate(filename, new PugConfiguration());
    }

    private static PugTemplate getTemplate(String filename, PugConfiguration pugConfiguration) throws IOException {
        if (filename == null) {
            throw new IllegalArgumentException("Filename can not be null");
        }

        String prefix = FilenameUtils.getFullPath(filename);
        String filePath = FilenameUtils.getName(filename);
        FileTemplateLoader loader = new FileTemplateLoader(prefix, StandardCharsets.UTF_8);
        pugConfiguration.setTemplateLoader(loader);
        return createTemplate(filePath, pugConfiguration);
    }

    public static PugTemplate getTemplate(String filename, String extension) throws IOException {
        if (filename == null) {
            throw new IllegalArgumentException("Filename can not be null");
        }

        String prefix = FilenameUtils.getFullPath(filename);
        String filePath = FilenameUtils.getName(filename);
        FileTemplateLoader loader = new FileTemplateLoader(prefix, StandardCharsets.UTF_8, extension);
        PugConfiguration pugConfiguration = new PugConfiguration();
        pugConfiguration.setTemplateLoader(loader);
        return createTemplate(filePath, pugConfiguration);
    }

    private static PugTemplate getTemplate(Reader reader, String name, PugConfiguration pugConfiguration) throws IOException {
        final ReaderTemplateLoader readerTemplateLoader = new ReaderTemplateLoader(reader, name);
        pugConfiguration.setTemplateLoader(readerTemplateLoader);
        return createTemplate(name, pugConfiguration);
    }

    private static PugTemplate createTemplate(String filename, PugConfiguration pugConfiguration) throws IOException {
        Parser parser = new Parser(filename, pugConfiguration.getTemplateLoader(), pugConfiguration.getExpressionHandler());
        Node root = parser.parse();
        return new PugTemplate(root,pugConfiguration.getMode());
    }

    private static String templateToString(PugTemplate template, Map<String, Object> model, PugConfiguration pugConfiguration) throws PugCompilerException {
        PugModel pugModel = new PugModel(model);
        StringWriter writer = new StringWriter();
        template.process(pugModel, writer, pugConfiguration);
        return writer.toString();
    }
}