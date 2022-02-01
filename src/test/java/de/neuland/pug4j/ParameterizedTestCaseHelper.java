package de.neuland.pug4j;

import de.neuland.pug4j.expression.ExpressionHandler;
import de.neuland.pug4j.filter.CDATAFilter;
import de.neuland.pug4j.filter.MarkdownFilter;
import de.neuland.pug4j.filter.PlainFilter;
import de.neuland.pug4j.template.FileTemplateLoader;
import de.neuland.pug4j.template.PugTemplate;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;

public class ParameterizedTestCaseHelper {

    private String fileTemplateLoaderPath;
    private String basePath;
    private String extension;
    private ExpressionHandler expressionHandler;

    public ParameterizedTestCaseHelper(String fileTemplateLoaderPath, String basePath, String extension, ExpressionHandler expressionHandler) {
        this.fileTemplateLoaderPath = fileTemplateLoaderPath;
        this.basePath = basePath;
        this.extension = extension;
        this.expressionHandler = expressionHandler;
    }

    @NotNull
    public PugConfiguration getPugConfiguration() {
        PugConfiguration pugConfiguration = new PugConfiguration();
        FileTemplateLoader fileTemplateLoader = new FileTemplateLoader(fileTemplateLoaderPath, extension);
        fileTemplateLoader.setBase(basePath);
        pugConfiguration.setTemplateLoader(fileTemplateLoader);
        pugConfiguration.setExpressionHandler(expressionHandler);
        pugConfiguration.setMode(Pug4J.Mode.XHTML); // original pugConfiguration uses xhtml by default
        pugConfiguration.setFilter("plain", new PlainFilter());
        pugConfiguration.setFilter("cdata", new CDATAFilter());
        pugConfiguration.setFilter("markdown", new MarkdownFilter());
        pugConfiguration.setFilter("markdown-it", new MarkdownFilter());
        pugConfiguration.setFilter("custom", (source, attributes, model) -> {
            Object opt = attributes.get("opt");
            Object num = attributes.get("num");
            assertEquals("val",opt);
            assertEquals(2,num);
            return "BEGIN"+source+"END";
        });
        pugConfiguration.setFilter("verbatim", (source, attributes, model) -> "\n"+source+"\n");
        pugConfiguration.setPrettyPrint(true);
        return pugConfiguration;
    }
    @NotNull
    public String getActualHtml(String filename,PugConfiguration pugConfiguration, HashMap<String, Object> model) throws IOException {
        PugTemplate template = pugConfiguration.getTemplate(File.separator+filename);
        Writer writer = new StringWriter();
        pugConfiguration.renderTemplate(template, model, writer);
        String html = writer.toString();
        String actual = html.trim().replaceAll("\r", "");
        return actual;
    }


    @NotNull
    public String getExpectedHtml(String filename) throws IOException {
        String filePath = fileTemplateLoaderPath + File.separator+ basePath + File.separator + filename.replace("."+extension, ".html");
        String content = FileUtils.readFileToString(new File(filePath), "UTF-8");
        String expected = content.trim().replaceAll("\r", "");
        return expected;
    }

    @NotNull
    public static Collection<String[]> createTestFileData(String resourcePath, String extension, String[] ignoredCases) {
        File folder = new File(resourcePath);
        Collection<File> files = FileUtils.listFiles(folder, new String[]{extension}, false);

        Collection<String[]> data = new ArrayList<String[]>();
        for (File file : files) {
            if (!ArrayUtils.contains(ignoredCases, file.getName().replace("."+ extension, ""))) {
                data.add(new String[]{file.getName()});
            }

        }
        return data;
    }

}
