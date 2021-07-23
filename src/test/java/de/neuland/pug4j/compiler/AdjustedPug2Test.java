package de.neuland.pug4j.compiler;

import de.neuland.pug4j.Pug4J;
import de.neuland.pug4j.PugConfiguration;
import de.neuland.pug4j.TestFileHelper;
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
public class AdjustedPug2Test {

    private static String[] ignoredCases = new String[] {
//            "block-code", //unsupported Javascript
//            "filters.include", //unsupported filters
//            "attrs.js", // unsupported map syntax
//            "filter-in-include", // missing filter
//            "filters.nested", //missing filters :uglify-js:coffee-script, replace with customerfilter to test
//            "pipeless-filters", //different markdown result but it works.

            //unsupported: adjust to work with pug4j
            "styles", // unsupported map syntax
            "regression.784",       // javascript replace not supported
            "filters.stylus", //missing filter
            "filters.less", // missing filter
            "attrs-data", // nice to have
            "each.else", //js issues
            "code.conditionals", //maybe js conditionals problem
            "filters.coffeescript", // missing filter
            "blocks-in-if" // js block not suppoerted

    };

    private String file;

    public AdjustedPug2Test(String file) {
        this.file = file;
    }

    @Test
    public void shouldCompilePugToHtml() throws Exception {
        PugConfiguration pug = new PugConfiguration();
        String fileTemplateLoaderPath = TestFileHelper.getAdjustedPug2ResourcePath("");
        FileTemplateLoader fileTemplateLoader = new FileTemplateLoader(fileTemplateLoaderPath, "pug");
        String basePath = "cases";
        fileTemplateLoader.setBase(basePath);
        pug.setTemplateLoader(fileTemplateLoader);
        pug.setMode(Pug4J.Mode.XHTML); // original pug uses xhtml by default
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
        pug.setFilter("custom2", new Filter() {
            @Override
            public String convert(String source, Map<String, Object> attributes, Map<String, Object> model) {
                Object opt = attributes.get("opt");
                Object num = attributes.get("num");
                assertEquals("val",opt);
                assertEquals(2,num);
                return "START"+source+"STOP";
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

        String pathToExpectedHtml = fileTemplateLoaderPath +basePath+ file.replace(".pug", ".html");
        String expected = readFile(pathToExpectedHtml).trim().replaceAll("\r", "");

        assertEquals(file, expected, html.trim());
    }

    private String readFile(String fileName) throws IOException {
        return FileUtils.readFileToString(new File(fileName));
    }

    @Parameterized.Parameters(name="{0}")
    public static Collection<String[]> data() {
        File folder = new File(TestFileHelper.getAdjustedPug2ResourcePath("/cases"));
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
