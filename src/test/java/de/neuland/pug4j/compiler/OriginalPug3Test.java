package de.neuland.pug4j.compiler;

import de.neuland.pug4j.Pug4J;
import de.neuland.pug4j.PugConfiguration;
import de.neuland.pug4j.TestFileHelper;
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

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class OriginalPug3Test {

    private static String[] ignoredCases = new String[] {

            // try to read files in ../
            //unsupported
            "styles", // wrong indent, rest ok.
            "filters.include", //unsupported filters
            "filters.stylus", //missing filter
            "filters.less", // missing filter
            "filters.nested", // missing filter
            "attrs-data", // only timeformat different
            "attrs", // only timeformat different
            "filter-in-include", // missing less filter
            "pipeless-filters", //maybe missing markdown-it or different markdown syntax as in js markdown
//            "code.iteration", // function block not working in buffered code. Maybe report to GraalVM Bugtracker.
            "filters.coffeescript", // missing filter
//            "blocks-in-if" // blocks in buffered code not recognozed. Should be fixable.

    };

    private static String[] casesWithoutLinebreak = new String[] {
            "filters.nested"
    };

    private String file;

    public OriginalPug3Test(String file) {
        this.file = file;
    }

    @Test
    public void shouldCompileJadeToHtml() throws Exception {
        PugConfiguration pugConfiguration = new PugConfiguration();
        String fileTemplateLoaderPath = TestFileHelper.getOriginalPug3ResourcePath("");
        FileTemplateLoader fileTemplateLoader = new FileTemplateLoader(fileTemplateLoaderPath, "pug");
        String basePath = "cases";
        fileTemplateLoader.setBase(basePath);
        pugConfiguration.setTemplateLoader(fileTemplateLoader);
        pugConfiguration.setExpressionHandler(new GraalJsExpressionHandler());
        pugConfiguration.setMode(Pug4J.Mode.XHTML); // original pugConfiguration uses xhtml by default
        pugConfiguration.setFilter("plain", new PlainFilter());
        pugConfiguration.setFilter("cdata", new CDATAFilter());
        pugConfiguration.setFilter("markdown", new MarkdownFilter());
        pugConfiguration.setFilter("markdown-it", new MarkdownFilter());
        pugConfiguration.setFilter("custom", new Filter() {
            @Override
            public String convert(String source, Map<String, Object> attributes, Map<String, Object> model) {
                Object opt = attributes.get("opt");
                Object num = attributes.get("num");
                assertEquals("val",opt);
                assertEquals(2,num);
                return "BEGIN"+source+"END";
            }
        });
        pugConfiguration.setFilter("verbatim", new Filter() {
            @Override
            public String convert(String source, Map<String, Object> attributes, Map<String, Object> model) {
                return "\n"+source+"\n";
            }
        });
        pugConfiguration.setPrettyPrint(true);
        PugTemplate template = pugConfiguration.getTemplate("" + file);
        Writer writer = new StringWriter();
        HashMap<String, Object> model = new HashMap<String, Object>();
        model.put("title","Pug");
        pugConfiguration.renderTemplate(template,model, writer);
        String html = writer.toString();

        String pathToExpectedHtml = fileTemplateLoaderPath +basePath+ file.replace(".pug", ".html");
        String expected = readFile(pathToExpectedHtml).trim().replaceAll("\r", "");
        if (ArrayUtils.contains(casesWithoutLinebreak, file.replace(".pug", ""))) {
            html = html.replaceAll("\\n| ","");
            expected = expected.replaceAll("\\n| ","");
        }
        assertEquals(file, expected, html.trim());
    }

    private String readFile(String fileName) throws IOException {
        return FileUtils.readFileToString(new File(fileName));
    }

    @Parameterized.Parameters(name="{0}")
    public static Collection<String[]> data() {
        File folder = new File(TestFileHelper.getOriginalPug3ResourcePath("/cases"));
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
