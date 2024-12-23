package de.neuland.pug4j.compiler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.neuland.pug4j.PugConfiguration;
import de.neuland.pug4j.exceptions.PugLexerException;
import de.neuland.pug4j.expression.ExpressionHandler;
import de.neuland.pug4j.filter.CssFilter;
import de.neuland.pug4j.filter.JsFilter;
import de.neuland.pug4j.template.PugTemplate;
import org.apache.commons.io.FileUtils;
import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import de.neuland.pug4j.Pug4J;
import de.neuland.pug4j.TestFileHelper;
import de.neuland.pug4j.exceptions.PugCompilerException;
import de.neuland.pug4j.filter.MarkdownFilter;
import de.neuland.pug4j.filter.PlainFilter;
import de.neuland.pug4j.helper.beans.Level2TestBean;
import de.neuland.pug4j.helper.beans.TestBean;
import de.neuland.pug4j.model.PugModel;
import de.neuland.pug4j.parser.Parser;
import de.neuland.pug4j.parser.node.Node;
import de.neuland.pug4j.template.FileTemplateLoader;

public class CompilerTest {

    private String expectedFileNameExtension = ".html";

    @Test
    public void oneTag() {
        run("one_tag");
    }

    @Test
    public void nestedTags() {
        run("nested_tags");
    }

    public void lageBodyTextWithoutPipes() {
        run("large_body_text_without_pipes");
    }

    @Test
    public void complexIndentOutdentFile() {
        run("complex_indent_outdent_file",true);
    }

    @Test
    public void blockQuote() {
        run("blockquote");
    }

    @Test
    public void blockquotes() {
        run("blockquotes");
    }

    @Test
    public void cssClassAndId() {
        run("css_class_and_id");
    }

    @Test
    public void blockExpansionShorthand() {
        run("block_expansion_shorthands");
    }

    @Test
    public void tagWithAttributes() {
        run("tags_with_attributes");
    }

    @Test
    public void tagWithText() {
        run("tags_with_text");
    }

    @Test
    public void blockExpansion() {
        run("block_expansion");
    }

    @Test
    public void whileTag() {
        run("while");
    }

    @Test
    public void minusMinusPlusPlusMatching() {
        run("plusminus");
    }
    
    @Test
    public void caseTag() {
        run("case");
    }

    @Test
    public void scriptTag() {
        run("script_tag");
    }

    @Test
    public void scriptTemplate() {
        run("script-template");
    }

    @Test
    public void variable() {
        run("variable");
    }

    @Test
    public void condition() {
        run("condition");
    }

    @Test
    public void conditionTypes() {
        run("condition_types");
    }

    @Test
    public void escape() {
        run("escape");
    }

    @Test
    public void whitespace() {
        run("whitespace");
    }

    @Test
    public void locals() {
        run("locals");
    }

    @Test
    public void complexCondition() {
        run("complex_condition");
    }

    @Test
    public void doctype() {
        run("doctype");
    }

    @Test
    public void terseDoctype() {
        run("terse_doctype");
    }

    @Test
    public void notTerseDoctype() {
        run("not_terse_doctype");
    }

    @Test
    public void beanPropertyCondition() {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("bean", getTestBean("beanie"));
        List<TestBean> moreBeans = new ArrayList<TestBean>();
        for (int i = 0; i < 5; i++) {
            moreBeans.add(getTestBean("x"));
        }
        map.put("beans", moreBeans);
        PugModel model = new PugModel(map);
        run("bean_property_condition", false, model);
    }

    private TestBean getTestBean(String name) {
        TestBean b = new TestBean();
        Level2TestBean b2 = new Level2TestBean();
        b2.setName(name);
        b.setLevel2(b2);
        return b;
    }

    @Test
    public void fuzzyBooleanCondition() {
        run("fuzzy_boolean_condition");
    }

    @Test
    public void assignment() {
        run("assignment");
    }

    @Test
    public void comment() {
        run("comment");
    }

    @Test
//    @Ignore("not supported since Jade 1.0 anymore")
    public void conditionalComment() {
        run("conditional_comment");
    }

    @Test
    public void each() {
        run("each");
    }

    @Test
    public void eachElse() {
        run("each_else");
    }

    @Test
    public void attribute() {
        run("attribute",true);
    }

    @Test
    public void prettyPrint() {
        run("prettyprint", true);
    }

    @Test
    public void scope() {
        run("scope");
    }

    @Test
    public void mixin() {
        run("mixin");
    }

    @Test
    public void mixinBlocks() {
        run("mixin_blocks",true);
    }

    @Test
    public void mixinMultipleBlocks() {
        run("mixin_multiple_blocks");
    }

    @Test
    public void mixinMultipleBlocksIf() {
        run("mixin_multiple_blocks_if");
    }

    @Test
    public void mixinMultipleBlocksCase() {
        run("mixin_multiple_blocks_case");
    }

    @Test
    public void mixinDefaultBlock() {
        run("mixin_default_block");
    }

    @Test
    public void mixinDefaultBlockNested() {
        run("mixin_default_block_nested");
    }

    @Test
    public void selfClosingTag() {
        run("self_closing_tag");
    }

    @Test
    public void mixinNested() {
        run("mixin_nested");
    }

    @Test
    public void mixinAttrs() {
        run("mixin_attrs", true);
    }

    @Test
    public void mixinMerge() {
        run("mixin_merge", true);
    }

    @Test
    public void mixin_with_conditional() {
        run("mixin_with_conditional");
    }

    @Test
    public void include1() {
        run("include_1");
    }

    @Test
    public void include2() {
        run("include_2");
    }

    @Test
    public void indentTabs() { run("indent_tabs"); }

    @Test
    public void extendsLayout() {
        run("extends");
    }

    @Test
    public void extendsLayoutWithInclude() {
        run("extends_layout_include");
    }

    @Test
    public void mixinWithCommaSinglearg() {
        run("mixin_with_comma_singlearg");
    }

    @Test
    public void mixinWithCommaMorearg() {
        run("mixin_with_comma_morearg");
    }

    @Test
    public void mixinWithComplexParameter() {
        run("mixin_with_complex_parameter");
    }

    @Test
    public void largeBodyTextWithPipes() {
        run("large_body_text_with_pipes");
    }

    @Test
    public void filterPlain() {
        run("filter_plain");
    }

    @Test(expected = PugCompilerException.class)
    public void expressionException() throws IOException, URISyntaxException {
        tryToRender("expression_exception");
    }

    @Test(expected = PugCompilerException.class)
    public void expressionWrongMethodCall() throws IOException, URISyntaxException {
        tryToRender("expression_method_invocation_exception");
    }

    private void tryToRender(String file) throws IOException, URISyntaxException {
        Pug4J.render(TestFileHelper.getCompilerResourcePath(file + ".jade"), new HashMap<String, Object>());
    }


    @Test
    public void filterMarkdown() {
        run("filter_markdown");
    }

    @Test
    public void interpolation() {
        run("interpolation");
    }

    @Test
    public void includeNonJade() {
        run("include_non_jade",true);
    }

    @Test
    public void includeNonJadeSimple() {
        run("include_non_jade_simple",true);
    }

    @Test
    public void mixinVariableAttribute() {
        run("mixin_variable_attribute");
    }

    @Test
    public void inlineTextAndContent() {
        run("inline_text_and_content");
    }

    @Test(expected = PugLexerException.class)
    public void shouldThrowGoodExceptions() {
        run("invalid");
    }

    @Test(expected = PugLexerException.class)
    public void shouldThrowGoodExceptions2() {
        run("invalid2");
    }

    @Test
    public void xml() {
        String tmp = expectedFileNameExtension;
        expectedFileNameExtension = ".xml";
        run("xml_doctype");
        expectedFileNameExtension = tmp;
    }

    @Test
    public void reportedIssue90() {
        run("reportedIssue89",true);
    }

    @Test
    public void mixinExtendInclude() {
        run("mixin_extend_include");
    }

    @Test
    public void mixinExtend() {
        run("mixin_extend");
    }

    private void run(String testName) {
        run(testName, false);
    }

    private void run(String testName, boolean pretty) {
        PugModel model = new PugModel(getModelMap(testName));
        run(testName, pretty, model);
    }

    private void run(String testName, boolean pretty, PugModel model) {
        Parser parser = null;
        ExpressionHandler expressionHandler = new PugConfiguration().getExpressionHandler();
        FileTemplateLoader loader = null;
        try {
            loader = new FileTemplateLoader(
                    TestFileHelper.getCompilerResourcePath(""), "jade");
            parser = new Parser(testName, loader, expressionHandler);
        } catch (IOException | URISyntaxException e) {
            e.printStackTrace();
        }


        Node root = parser.parse();
        PugTemplate pugTemplate = new PugTemplate(root);
        pugTemplate.setPrettyPrint(pretty);
        pugTemplate.setTemplateLoader(loader);
        pugTemplate.setExpressionHandler(expressionHandler);
        Compiler compiler = new Compiler(pugTemplate);

        String expected = readFile(testName + expectedFileNameExtension);
        model.addFilter("markdown", new MarkdownFilter());
        model.addFilter("plain", new PlainFilter());
        model.addFilter("js", new JsFilter());
        model.addFilter("css", new CssFilter());
        model.addFilter("svg", new PlainFilter());
        String html;
        try {
            html = compiler.compileToString(model);
            assertEquals(testName, expected.trim().replaceAll("\r", ""), html.trim().replaceAll("\r", ""));
        } catch (PugCompilerException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @SuppressWarnings("unused")
    private void debugOutput(String testName) {
        System.out.println(testName + " >>>> ");
        System.out.println("[jade]");
        System.out.println(readFile(testName + ".jade").trim() + "\n");
        System.out.println("[model]");
        System.out.println(readFile(testName + ".json") + "\n");
        System.out.println("[html]");
        System.out.println(readFile(testName + ".html").trim() + "\n");
    }

    private Map<String, Object> getModelMap(String testName) {
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

    private String readFile(String fileName) {
        try {
            return FileUtils.readFileToString(new File(TestFileHelper
                    .getCompilerResourcePath(fileName)),"UTF-8");
        } catch (Exception e) {
            // e.printStackTrace();
        }
        return "";
    }

}
