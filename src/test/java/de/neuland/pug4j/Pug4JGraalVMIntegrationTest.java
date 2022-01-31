package de.neuland.pug4j;

import de.neuland.pug4j.expression.GraalJsExpressionHandler;
import de.neuland.pug4j.filter.CDATAFilter;
import de.neuland.pug4j.filter.Filter;
import de.neuland.pug4j.filter.MarkdownFilter;
import de.neuland.pug4j.filter.PlainFilter;
import de.neuland.pug4j.template.FileTemplateLoader;
import de.neuland.pug4j.template.PugTemplate;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.*;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class Pug4JGraalVMIntegrationTest {

    private static String[] ignoredCases = new String[] {
            "include-with-filter"
    };

    private String file;

    public Pug4JGraalVMIntegrationTest(String file) {
        this.file = file;
    }

    @Test
    public void shouldCompilePugToHtml() throws Exception {
        PugConfiguration pug = new PugConfiguration();
        pug.setExpressionHandler(new GraalJsExpressionHandler());
        String fileTemplateLoaderPath = TestFileHelper.getPug4JGraalVMTestsResourcePath("");
        FileTemplateLoader fileTemplateLoader = new FileTemplateLoader(fileTemplateLoaderPath, "pug");
        String basePath = "cases";
        fileTemplateLoader.setBase(basePath);
        pug.setTemplateLoader(fileTemplateLoader);
        pug.setMode(Pug4J.Mode.XHTML); // original jade uses xhtml by default
        pug.setFilter("plain", new PlainFilter());
        pug.setFilter("cdata", new CDATAFilter());
        pug.setFilter("markdown", new MarkdownFilter());
        pug.setFilter("markdown-it", new MarkdownFilter());
        pug.setFilter("custom", new Filter() {
            @Override
            public String convert(String source, Map<String, Object> attributes, Map<String, Object> model) {
                Object opt = attributes.get("opt");
                Object num = attributes.get("num");
                assertEquals("val",opt);
                assertEquals(2,num);
                return "BEGIN"+source+"END";
            }
        });
        pug.setFilter("verbatim", new Filter() {
            @Override
            public String convert(String source, Map<String, Object> attributes, Map<String, Object> model) {
                return "\n"+source+"\n";
            }
        });
        pug.setPrettyPrint(true);
        PugTemplate template = pug.getTemplate("" + file);
        Writer writer = new StringWriter();
        HashMap<String, Object> model = new HashMap<String, Object>();
        model.put("title","Pug");
        pug.renderTemplate(template,model, writer);
        String html = writer.toString();
        String pathToExpectedHtml = fileTemplateLoaderPath +File.separator+basePath+ file.replace(".pug", ".html");
        String expected = readFile(pathToExpectedHtml).trim().replaceAll("\r", "");

        assertEquals(file, expected, html.trim().replaceAll("\r", ""));
    }

    private String readFile(String fileName) throws IOException {
        return FileUtils.readFileToString(new File(fileName), "UTF-8");
    }

    @Parameterized.Parameters(name="{0}")
    public static Collection<String[]> data() throws FileNotFoundException, URISyntaxException {
        File folder = new File(TestFileHelper.getPug4JGraalVMTestsResourcePath("/cases"));
        Collection<File> files = FileUtils.listFiles(folder, new String[]{"pug"}, false);

        Collection<String[]> data = new ArrayList<String[]>();
        for (File file : files) {
            if (!ArrayUtils.contains(ignoredCases, file.getName().replace(".pug", ""))) {
                data.add(new String[]{"/"+file.getName()});
            }

        }
        return data;
    }
}
