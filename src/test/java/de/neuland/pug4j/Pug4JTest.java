package de.neuland.pug4j;

import de.neuland.pug4j.exceptions.PugLexerException;
import de.neuland.pug4j.exceptions.PugParserException;
import de.neuland.pug4j.template.FileTemplateLoader;
import de.neuland.pug4j.template.PugTemplate;
import org.apache.commons.io.FilenameUtils;
import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class Pug4JTest {

    @Test
    public void testRenderDefault() throws Exception{
        final Path path = Paths.get("src/test/resources/compiler/extends.pug");
        final String html = Pug4J.render(path.toAbsolutePath().toString(), new HashMap<String, Object>());
        assertEquals("<h1>hello world</h1><p>default foo</p><p>special bar</p><div class=\"prepend\">hello world</div><div class=\"append\">hello world</div><ul><li>1</li><li>2</li><li>3</li><li>4</li></ul><ul><li>a</li><li>b</li><li>c</li><li>d</li></ul>",html);
    }
    @Test(expected = FileNotFoundException.class)
    public void testRenderUnknownFile() throws Exception{
        final Path path = Paths.get("src/test/resources/compiler/extendsDoesNotExist.pug");
        final String html = Pug4J.render(path.toAbsolutePath().toString(), new HashMap<String, Object>());
        assertEquals("<h1>hello world</h1><p>default foo</p><p>special bar</p><div class=\"prepend\">hello world</div><div class=\"append\">hello world</div><ul><li>1</li><li>2</li><li>3</li><li>4</li></ul><ul><li>a</li><li>b</li><li>c</li><li>d</li></ul>",html);
    }
    @Test(expected = IllegalArgumentException.class)
    public void testRenderNull() throws Exception{
        String fullPath = null;
        final String html = Pug4J.render(fullPath, new HashMap<String, Object>());
        assertEquals("<h1>hello world</h1><p>default foo</p><p>special bar</p><div class=\"prepend\">hello world</div><div class=\"append\">hello world</div><ul><li>1</li><li>2</li><li>3</li><li>4</li></ul><ul><li>a</li><li>b</li><li>c</li><li>d</li></ul>",html);
    }
    @Test(expected = PugParserException.class) // layout does not exist
    public void testRenderUnknownInclude() throws Exception{
        final Path path = Paths.get("src/test/resources/compiler/extendsUnknownInclude.pug");
        final String html = Pug4J.render(path.toAbsolutePath().toString(), new HashMap<String, Object>());
        assertEquals("<h1>hello world</h1><p>default foo</p><p>special bar</p><div class=\"prepend\">hello world</div><div class=\"append\">hello world</div><ul><li>1</li><li>2</li><li>3</li><li>4</li></ul><ul><li>a</li><li>b</li><li>c</li><li>d</li></ul>",html);
    }
    @Test
    public void testRenderDefaultRelativePath() throws Exception{
        final String html = Pug4J.render("./src/test/resources/compiler/extends.pug", new HashMap<String, Object>());
        assertEquals("<h1>hello world</h1><p>default foo</p><p>special bar</p><div class=\"prepend\">hello world</div><div class=\"append\">hello world</div><ul><li>1</li><li>2</li><li>3</li><li>4</li></ul><ul><li>a</li><li>b</li><li>c</li><li>d</li></ul>",html);
    }
    @Test
    public void testTemplateDefault() throws Exception{
        final Path path = Paths.get("src/test/resources/compiler/extends.pug");
        PugTemplate template = Pug4J.getTemplate(path.toAbsolutePath().toString());
        final String html = Pug4J.render(template, new HashMap<String, Object>());
        assertEquals("<h1>hello world</h1><p>default foo</p><p>special bar</p><div class=\"prepend\">hello world</div><div class=\"append\">hello world</div><ul><li>1</li><li>2</li><li>3</li><li>4</li></ul><ul><li>a</li><li>b</li><li>c</li><li>d</li></ul>",html);
    }
    @Test
    public void testConfigurationDefault() throws Exception{
        final Path path = Paths.get("src/test/resources/compiler/extends.pug");
        PugConfiguration config = new PugConfiguration();
        String fileName = path.toAbsolutePath().toString();
        config.setTemplateLoader(new FileTemplateLoader(FilenameUtils.getFullPath(fileName)));
        PugTemplate template = config.getTemplate(FilenameUtils.getName(fileName));
        Map<String, Object> model = new HashMap<String, Object>();
        final String html = config.renderTemplate(template, model);
        assertEquals("<h1>hello world</h1><p>default foo</p><p>special bar</p><div class=\"prepend\">hello world</div><div class=\"append\">hello world</div><ul><li>1</li><li>2</li><li>3</li><li>4</li></ul><ul><li>a</li><li>b</li><li>c</li><li>d</li></ul>",html);
    }
    @Test
    public void testConfigurationDefaultWithRelativePath() throws Exception{
        PugConfiguration config = new PugConfiguration();
        PugTemplate template = config.getTemplate("src/test/resources/compiler/extends.pug");
        Map<String, Object> model = new HashMap<String, Object>();
        final String html = config.renderTemplate(template, model);
        assertEquals("<h1>hello world</h1><p>default foo</p><p>special bar</p><div class=\"prepend\">hello world</div><div class=\"append\">hello world</div><ul><li>1</li><li>2</li><li>3</li><li>4</li></ul><ul><li>a</li><li>b</li><li>c</li><li>d</li></ul>",html);
    }
    @Test(expected = PugParserException.class)
    public void testRenderDefaultWithTemplateOutsideTemplateLoaderPath() throws Exception{
        final Path path = Paths.get("src/test/resources/compiler/subdir/extends.pug");
        final String html = Pug4J.render(path.toAbsolutePath().toString(), new HashMap<String, Object>());
        assertEquals("<h1>hello world</h1><p>default foo</p><p>special bar</p><div class=\"prepend\">hello world</div><div class=\"append\">hello world</div><ul><li>1</li><li>2</li><li>3</li><li>4</li></ul><ul><li>a</li><li>b</li><li>c</li><li>d</li></ul>",html);
    }
    @Test(expected = PugParserException.class) //Jade is not supported anymore
    public void testRenderJade() throws Exception{
        final Path path = Paths.get("src/test/resources/compiler/extends.jade");
        final String html = Pug4J.render(path.toAbsolutePath().toString(), new HashMap<String, Object>());
        assertEquals("<h1>hello world</h1><p>default foo</p><p>special bar</p><div class=\"prepend\">hello world</div><div class=\"append\">hello world</div><ul><li>1</li><li>2</li><li>3</li><li>4</li></ul><ul><li>a</li><li>b</li><li>c</li><li>d</li></ul>",html);
    }
    @Test(expected = PugParserException.class) //Jade is not supported anymore
    public void testRenderJadeWithTemplateOutsideTemplateLoaderPath() throws Exception{
        final Path path = Paths.get("src/test/resources/compiler/subdir/extends.jade");
        final String html = Pug4J.render(path.toAbsolutePath().toString(), new HashMap<String, Object>());
    }
    @Test(expected = PugLexerException.class)
    public void testpug006() throws Exception{
        try {
            final String html = Pug4J.render("src/test/resources/errors/pug006.pug", new HashMap<String, Object>());
        }catch(PugLexerException exception){
            assertEquals("End of line was reached with no closing bracket for interpolation.",exception.getMessage());
            assertEquals("class de.neuland.pug4j.exceptions.PugLexerException: pug006.pug:1:16\n" +
                    "  > 1| h1 #{variable'}\n" +
                    "----------------------^\n" +
                    "\n" +
                    "End of line was reached with no closing bracket for interpolation.",exception.toString());
            throw exception;
        }
    }
}