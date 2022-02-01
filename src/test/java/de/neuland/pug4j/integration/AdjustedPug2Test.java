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
import java.util.Collection;
import java.util.HashMap;

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
//            "styles", // unsupported map syntax
            "regression.784",       // javascript replace not supported
            "filters.stylus", //missing filter
            "filters.less", // missing filter
//            "attrs-data", // nice to have
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
        String fileTemplateLoaderPath = TestFileHelper.getAdjustedPug2ResourcePath("");
        String basePath = "cases";
        String extension = "pug";
        ExpressionHandler expressionHandler = new JexlExpressionHandler();
        ParameterizedTestCaseHelper testHelper = new ParameterizedTestCaseHelper(fileTemplateLoaderPath,basePath,extension,expressionHandler);
        PugConfiguration pugConfiguration = testHelper.getPugConfiguration();
        pugConfiguration.setFilter("custom2", (source, attributes, model) -> {
            Object opt = attributes.get("opt");
            Object num = attributes.get("num");
            assertEquals("val",opt);
            assertEquals(2,num);
            return "START"+source+"STOP";
        });
        HashMap<String, Object> model = new HashMap<String, Object>();
        model.put("title","Pug");

        String actual = testHelper.getActualHtml(file,pugConfiguration,model);
        String expected = testHelper.getExpectedHtml(file);

        assertEquals(file, expected, actual);
    }

    @Parameterized.Parameters(name="{0}")
    public static Collection<String[]> data() throws FileNotFoundException, URISyntaxException {
        String resourcePath = TestFileHelper.getAdjustedPug2ResourcePath("/cases");
        String extension = "pug";
        return ParameterizedTestCaseHelper.createTestFileData(resourcePath, extension, ignoredCases);
    }
}
