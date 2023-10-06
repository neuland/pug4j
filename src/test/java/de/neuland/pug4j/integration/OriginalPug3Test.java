package de.neuland.pug4j.integration;

import de.neuland.pug4j.ParameterizedTestCaseHelper;
import de.neuland.pug4j.PugConfiguration;
import de.neuland.pug4j.TestFileHelper;
import de.neuland.pug4j.expression.GraalJsExpressionHandler;
import org.apache.commons.lang3.ArrayUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.*;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class OriginalPug3Test {

    private static String[] ignoredCases = new String[] {

            // try to read files in ../
            //unsupported
            "filters.include", //unsupported filters
            "filters.stylus", //missing filter
            "filters.less", // missing filter
            "filters.nested", // missing filter
            "filter-in-include", // missing less filter
            "pipeless-filters", //maybe missing markdown-it or different markdown syntax as in js markdown
            "code.iteration", // function block not working in buffered code. Maybe report to GraalVM Bugtracker.
            "filters.coffeescript", // missing filter
            "blocks-in-if" // blocks in buffered code not recognozed. Should be fixable.

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
        String basePath = "cases";
        String fileTemplateLoaderPath = TestFileHelper.getOriginalPug3ResourcePath("");
        String extension = "pug";
        GraalJsExpressionHandler expressionHandler = new GraalJsExpressionHandler();
        ParameterizedTestCaseHelper testHelper = new ParameterizedTestCaseHelper(fileTemplateLoaderPath,basePath,extension,expressionHandler);
        PugConfiguration pugConfiguration = testHelper.getPugConfiguration();

        HashMap<String, Object> model = new HashMap<String, Object>();
        model.put("title","Pug");

        String actual = testHelper.getActualHtml(file,pugConfiguration, model);
        String expected = testHelper.getExpectedHtml(file);

        if (ArrayUtils.contains(casesWithoutLinebreak, file.replace("."+extension, ""))) {
            actual = actual.replaceAll("\\n| ","");
            expected = expected.replaceAll("\\n| ","");
        }

        assertEquals(file, expected, actual);
    }



    @Parameterized.Parameters(name="{0}")
    public static Collection<String[]> data() throws FileNotFoundException, URISyntaxException {
        String resourcePath = TestFileHelper.getOriginalPug3ResourcePath("/cases");
        String extension = "pug";
        return ParameterizedTestCaseHelper.createTestFileData(resourcePath, extension, ignoredCases);
    }

}
