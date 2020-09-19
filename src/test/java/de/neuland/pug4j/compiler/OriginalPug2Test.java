package de.neuland.pug4j.compiler;

import de.neuland.pug4j.Pug4J;
import de.neuland.pug4j.PugConfiguration;
import de.neuland.pug4j.TestFileHelper;
import de.neuland.pug4j.filter.CDATAFilter;
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
import java.util.*;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class OriginalPug2Test {
//    private static String[] ignoredCases = new String[]{};

    private static String[] ignoredCases = new String[] {
            "regression.784",       // javascript replace not supported

            "filters.stylus",
            "filters.less",

            // try to read files in ../
            "attrs-data",
            "layout.append.without-block",
            "layout.prepend.without-block",
            "filter-in-include",
            "filters.nested",
            "escape-test",
            "filters.custom",
            "styles",
            "block-code",
            "filters.include",
            "filters.include.custom",
            "attrs.js",
            "includes",
            "pipeless-filters",
            "layout.prepend",
            "each.else",
            "filters.inline",
            "code.conditionals",
            "filters.coffeescript",
            "layout.append",
            "blocks-in-if"

    };

    private String file;

    public OriginalPug2Test(String file) {
        this.file = file;
    }

    @Test
    public void shouldCompileJadeToHtml() throws Exception {
        PugConfiguration jade = new PugConfiguration();
        String fileTemplateLoaderPath = TestFileHelper.getOriginalPug2ResourcePath("");
        FileTemplateLoader fileTemplateLoader = new FileTemplateLoader(fileTemplateLoaderPath, "pug");
        String basePath = "cases";
        fileTemplateLoader.setBase(basePath);
        jade.setTemplateLoader(fileTemplateLoader);
        jade.setMode(Pug4J.Mode.XHTML); // original jade uses xhtml by default
        jade.setFilter("plain", new PlainFilter());
        jade.setFilter("cdata", new CDATAFilter());
        jade.setFilter("markdown", new MarkdownFilter());
        jade.setFilter("markdown-it", new MarkdownFilter());
        jade.setPrettyPrint(true);
        PugTemplate template = jade.getTemplate("" + file);
        Writer writer = new StringWriter();
        HashMap<String, Object> model = new HashMap<String, Object>();
        model.put("title","Pug");
        jade.renderTemplate(template,model, writer);
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
        File folder = new File(TestFileHelper.getOriginalPug2ResourcePath("/cases"));
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
