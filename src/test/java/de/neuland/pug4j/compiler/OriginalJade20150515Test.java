package de.neuland.pug4j.compiler;

import de.neuland.pug4j.Pug4J;
import de.neuland.pug4j.PugConfiguration;
import de.neuland.pug4j.TestFileHelper;
import de.neuland.pug4j.filter.*;
import de.neuland.pug4j.template.FileTemplateLoader;
import de.neuland.pug4j.template.PugTemplate;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.junit.Ignore;
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

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
@Ignore("Replaced By Pug2 Tests")
public class OriginalJade20150515Test {
    private static String[] ignoredCases = new String[]{"attrs", "attrs.js", "code.conditionals", "code.iteration",
            "filters.coffeescript", "filters.less", "filters.markdown", "filters.stylus",
            "mixin.merge", "styles", "regression.784", "mixin.attrs", "include-filter-stylus",
            "include-filter", "filters-empty", "each.else", "classes", "blocks-in-if", "filters.cdata", "block-code",
            "attrs-data", "attrs.interpolation"};

    private String file;

    public OriginalJade20150515Test(String file) {
        this.file = file;
    }

    @Test
    public void shouldCompileJadeToHtml() throws Exception {
        PugConfiguration jade = new PugConfiguration();
        String basePath = TestFileHelper.getOriginal20150515ResourcePath("");
        jade.setTemplateLoader(new FileTemplateLoader(basePath,"jade"));
        jade.setMode(Pug4J.Mode.XHTML); // original jade uses xhtml by default
        //jade.setExpressionHandler(new JsExpressionHandler());
        jade.setFilter("plain", new PlainFilter());
        jade.setFilter("cdata", new CDATAFilter());
        jade.setFilter("custom-filter", new CustomTestFilter());
        jade.setFilter("marked", new MarkdownFilter());
        jade.setFilter("verbatim", new VerbatimFilter());
        jade.setFilter("js", new JsFilter());
        jade.setFilter("css", new CssFilter());
        jade.setPrettyPrint(true);
        PugTemplate template = jade.getTemplate("/cases/" + file);
        Writer writer = new StringWriter();
        HashMap<String, Object> model = new HashMap<String, Object>();
        model.put("title", "Jade");
        jade.renderTemplate(template, model, writer);
        String html = writer.toString();

        String pathToExpectedHtml = basePath + "/cases/" + file.replace(".jade", ".html");
        String expected = readFile(pathToExpectedHtml).trim().replaceAll("\r", "");

        assertEquals(file, expected, html.trim());
    }

    private String readFile(String fileName) throws IOException {
        return FileUtils.readFileToString(new File(fileName));
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<String[]> data() {
        File folder = new File(TestFileHelper.getOriginal20150515ResourcePath("/cases/"));
        Collection<File> files = FileUtils.listFiles(folder, new String[]{"jade"}, false);

        Collection<String[]> data = new ArrayList<String[]>();
        for (File file : files) {
            if (!ArrayUtils.contains(ignoredCases, file.getName().replace(".jade", ""))) {
                data.add(new String[]{file.getName()});
            }

        }
        return data;
    }
}
