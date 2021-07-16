package de.neuland.pug4j;

import de.neuland.pug4j.exceptions.PugLexerException;
import junit.framework.TestCase;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;

public class Pug4JTest {

    @Test
    public void testRenderDefault() throws Exception{
        final Path path = Paths.get("src/test/resources/compiler/extends.pug");
        final String html = Pug4J.render(path.toAbsolutePath().toString(), new HashMap<String, Object>());
        assertEquals("<h1>hello world</h1><p>default foo</p><p>special bar</p><div class=\"prepend\">hello world</div><div class=\"append\">hello world</div><ul><li>1</li><li>2</li><li>3</li><li>4</li></ul><ul><li>a</li><li>b</li><li>c</li><li>d</li></ul>",html);
    }
    @Test
    public void testRenderDefaultWithTemplateOutsideTemplateLoaderPath() throws Exception{
        final Path path = Paths.get("src/test/resources/compiler/subdir/extends.pug");
        final String html = Pug4J.render(path.toAbsolutePath().toString(), new HashMap<String, Object>());
        assertEquals("<h1>hello world</h1><p>default foo</p><p>special bar</p><div class=\"prepend\">hello world</div><div class=\"append\">hello world</div><ul><li>1</li><li>2</li><li>3</li><li>4</li></ul><ul><li>a</li><li>b</li><li>c</li><li>d</li></ul>",html);
    }
    @Test
    public void testRenderJade() throws Exception{
        final Path path = Paths.get("src/test/resources/compiler/extends.jade");
        final String html = Pug4J.render(path.toAbsolutePath().toString(), new HashMap<String, Object>());
        assertEquals("<h1>hello world</h1><p>default foo</p><p>special bar</p><div class=\"prepend\">hello world</div><div class=\"append\">hello world</div><ul><li>1</li><li>2</li><li>3</li><li>4</li></ul><ul><li>a</li><li>b</li><li>c</li><li>d</li></ul>",html);
    }
    @Test
    public void testRenderJadeWithTemplateOutsideTemplateLoaderPath() throws Exception{
        final Path path = Paths.get("src/test/resources/compiler/subdir/extends.jade");
        final String html = Pug4J.render(path.toAbsolutePath().toString(), new HashMap<String, Object>());
        assertEquals("<h1>hello world</h1><p>default foo</p><p>special bar</p><div class=\"prepend\">hello world</div><div class=\"append\">hello world</div><ul><li>1</li><li>2</li><li>3</li><li>4</li></ul><ul><li>a</li><li>b</li><li>c</li><li>d</li></ul>",html);
    }
    @Test(expected = PugLexerException.class)
    public void testpug006() throws Exception{
        final Path path = Paths.get("src/test/resources/errors/pug006.pug");
        try {
            final String html = Pug4J.render(path.toAbsolutePath().toString(), new HashMap<String, Object>());
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