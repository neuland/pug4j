package de.neuland.pug4j.integration;

import de.neuland.pug4j.ParameterizedTestCaseHelper;
import de.neuland.pug4j.PugConfiguration;
import de.neuland.pug4j.TestFileHelper;
import de.neuland.pug4j.expression.ExpressionHandler;
import de.neuland.pug4j.expression.JexlExpressionHandler;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.*;
import java.net.URISyntaxException;
import java.util.*;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class OriginalPug2Test {
//    private static String[] ignoredCases = new String[]{};

    private static String[] ignoredCases = new String[] {

            // try to read files in ../
            //unsupported
            "styles", // unsupported map syntax
            "block-code", //unsupported Javascript
            "filters.include", //unsupported filters
            "attrs.js", // unsupported map syntax
            "regression.784",       // javascript replace not supported
            "filters.stylus", //missing filter
            "filters.less", // missing filter
            "attrs-data", // nice to have
            "filter-in-include", // missing less filter
            "filters.nested", //missing filters :uglify-js:coffee-script, replace with customerfilter to test
            "pipeless-filters", //maybe missing markdown-it or different markdown syntax as in js markdown
            "each.else", //js issues
            "code.conditionals", //maybe js conditionals problem
            "filters.coffeescript", // missing filter
            "blocks-in-if" // js block not suppoerted

    };

    private String file;

    public OriginalPug2Test(String file) {
        this.file = file;
    }

    @Test
    public void shouldCompileJadeToHtml() throws Exception {
        String basePath = "cases";
        String fileTemplateLoaderPath = TestFileHelper.getOriginalPug2ResourcePath("");
        String extension = "pug";
        ExpressionHandler expressionHandler = new JexlExpressionHandler();
        ParameterizedTestCaseHelper testHelper = new ParameterizedTestCaseHelper(fileTemplateLoaderPath,basePath,extension,expressionHandler);
        PugConfiguration pugConfiguration = testHelper.getPugConfiguration();

        HashMap<String, Object> model = new HashMap<String, Object>();
        model.put("title","Pug");

        String actual = testHelper.getActualHtml(file,pugConfiguration,model);
        String expected = testHelper.getExpectedHtml(file);

        assertEquals(file, expected, actual);
    }

    @Parameterized.Parameters(name="{0}")
    public static Collection<String[]> data() throws FileNotFoundException, URISyntaxException {
        String resourcePath = TestFileHelper.getOriginalPug2ResourcePath("/cases");
        String extension = "pug";
        return ParameterizedTestCaseHelper.createTestFileData(resourcePath, extension, ignoredCases);
    }
}
