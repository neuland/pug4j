package de.neuland.pug4j;

import junit.framework.TestCase;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;

public class Pug4JTest extends TestCase {

    public void testRenderDefault() throws Exception{
        final Path path = Paths.get("src/test/resources/compiler/extends.pug");
        System.out.println(path.toAbsolutePath().toString());
        final String html = Pug4J.render(path.toAbsolutePath().toString(), new HashMap<String, Object>());
        System.out.println(html);
    }
    public void testRenderDefaultWithTemplateOutsideTemplateLoaderPath() throws Exception{
        final Path path = Paths.get("src/test/resources/compiler/subdir/extends.pug");
        System.out.println(path.toAbsolutePath().toString());
        final String html = Pug4J.render(path.toAbsolutePath().toString(), new HashMap<String, Object>());
        System.out.println(html);
    }
}