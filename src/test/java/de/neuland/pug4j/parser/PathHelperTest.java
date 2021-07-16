package de.neuland.pug4j.parser;

import de.neuland.pug4j.TestFileHelper;
import de.neuland.pug4j.template.FileTemplateLoader;
import org.apache.commons.io.FilenameUtils;
import org.junit.Test;

import java.io.IOException;

import static junit.framework.TestCase.assertNull;
import static org.junit.Assert.assertEquals;

public class PathHelperTest extends ParserTest {

    PathHelper pathHelper = new PathHelper();
    private String RESOURCE_PATH = TestFileHelper.getLoaderResourcePath("");
    @Test
    public void name() throws IOException {
        String resolvePath = pathHelper.resolvePath("/pages/index.pug", "subdir/layout.pug","");
        assertEquals("/pages/subdir/layout.pug",resolvePath);
    }
    @Test
    public void name2() {
        String resolvePath = pathHelper.resolvePath("/pages/subdir/test.pug", "layout.pug","");
        assertEquals("/pages/subdir/layout.pug",resolvePath);
    }
    @Test
    public void name3() {
        String resolvePath = pathHelper.resolvePath("pages/subdir/test.pug", "/index.pug","pages/");
        assertEquals("pages/index.pug",resolvePath);
    }
    @Test
    public void name4() {
        String resolvePath = pathHelper.resolvePath("pages/subdir/test.pug", "../index.pug","pages/");
        assertEquals(   "pages/index.pug",resolvePath);
    }

    @Test
    public void name5() {
        String resolvePath = pathHelper.resolvePath("pages/subdir/test.pug", "../../modules/include.pug","pages/");
        assertEquals("modules/include.pug",resolvePath);
    }

    @Test
    public void name6() {
        String resolvePath = pathHelper.resolvePath("pages/subdir/test.pug", "layout.pug","pages/");
        assertEquals("pages/subdir/layout.pug",resolvePath);
    }
    @Test
    public void name7() {
        String resolvePath = pathHelper.resolvePath("pages/subdir/test.pug", "/subdir/layout.pug","pages/");
        assertEquals("pages/subdir/layout.pug",resolvePath);
    }
    @Test
    public void name8() {
        String resolvePath = pathHelper.resolvePath("/pages/subdir/test.pug", "/layout.pug","");
        assertEquals("/layout.pug",resolvePath);

    }

    @Test
    public void shouldResolvePathWindows() throws Exception {
        String result = pathHelper.resolvePath("C:/kek/index.jade","../_layout.jade", "C:/kek");
        assertEquals(FilenameUtils.separatorsToSystem("C:\\_layout.jade"),result);

    }
}
