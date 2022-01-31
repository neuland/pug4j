package de.neuland.pug4j.compiler;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import de.neuland.pug4j.expression.JexlExpressionHandler;
import org.apache.commons.io.FileUtils;
import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import de.neuland.pug4j.TestFileHelper;
import de.neuland.pug4j.exceptions.PugCompilerException;
import de.neuland.pug4j.filter.MarkdownFilter;
import de.neuland.pug4j.filter.PlainFilter;
import de.neuland.pug4j.model.PugModel;
import de.neuland.pug4j.parser.Parser;
import de.neuland.pug4j.parser.node.Node;
import de.neuland.pug4j.template.FileTemplateLoader;

public class CompilerTagErrorTest {

    @Test(expected=Exception.class)
    public void testTagsWithErrors() throws IOException, URISyntaxException {
        run("tags_with_errors");
    }

    private void run(String testName) throws IOException, URISyntaxException {
        run(testName, false);
    }

    private void run(String testName, boolean pretty) throws IOException, URISyntaxException {
        PugModel model = new PugModel(getModelMap(testName));
        run(testName, pretty, model);
    }

    private void run(String testName, boolean pretty, PugModel model) throws IOException, URISyntaxException {
        Parser parser = null;
        FileTemplateLoader loader = new FileTemplateLoader(TestFileHelper.getCompilerErrorsResourcePath(""),
                    "jade");
        parser = new Parser(testName, loader, new JexlExpressionHandler());
        Node root = parser.parse();
        Compiler compiler = new Compiler(root);
        compiler.setPrettyPrint(pretty);
        String expected = readFile(testName + ".html");
        model.addFilter("markdown", new MarkdownFilter());
        model.addFilter("plain", new PlainFilter());
        String html;
        html = compiler.compileToString(model);
        assertEquals(testName, expected.trim(), html.trim());
    }

    private Map<String, Object> getModelMap(String testName) throws IOException, URISyntaxException {
        String json = readFile(testName + ".json");
        Gson gson = new Gson();
        Type type = new TypeToken<Map<String, Object>>() {
        }.getType();
        Map<String, Object> model = gson.fromJson(json, type);
        if (model == null) {
            model = new HashMap<String, Object>();
        }
        return model;
    }

    private String readFile(String fileName) throws IOException, URISyntaxException {
        return FileUtils.readFileToString(new File(TestFileHelper.getCompilerErrorsResourcePath(fileName)),"UTF-8");
    }

}
