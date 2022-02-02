package de.neuland.pug4j.parser;

import de.neuland.pug4j.TestFileHelper;
import de.neuland.pug4j.exceptions.PugTemplateLoaderException;
import de.neuland.pug4j.template.FileTemplateLoader;
import org.apache.commons.io.FilenameUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static junit.framework.TestCase.assertNull;
import static org.junit.Assert.assertEquals;

public class PathHelperTest extends ParserTest {

    PathHelper pathHelper = new PathHelper();

    @Test
    public void name() throws IOException {
        String resolvePath = pathHelper.resolvePath("pages/index.pug", "subdir/layout.pug","");
        assertEquals(FilenameUtils.separatorsToSystem("pages/subdir/layout.pug"),resolvePath);
    }
    @Test
    public void name2() {
        String resolvePath = pathHelper.resolvePath("pages/subdir/test.pug", "layout.pug","");
        assertEquals(FilenameUtils.separatorsToSystem("pages/subdir/layout.pug"),resolvePath);
    }
    @Test
    public void name3() {
        String resolvePath = pathHelper.resolvePath("pages/subdir/test.pug", "/index.pug","pages/");
        assertEquals(FilenameUtils.separatorsToSystem("pages/index.pug"),resolvePath);
    }
    @Test
    public void name4() {
        String resolvePath = pathHelper.resolvePath("pages/subdir/test.pug", "../index.pug","pages/");
        assertEquals(FilenameUtils.separatorsToSystem(   "pages/index.pug"),resolvePath);
    }

    @Test
    public void name5() {
        String resolvePath = pathHelper.resolvePath("pages/subdir/test.pug", "../../modules/include.pug","pages/");
        assertEquals(FilenameUtils.separatorsToSystem("modules/include.pug"),resolvePath);
    }

    @Test
    public void name6() {
        String resolvePath = pathHelper.resolvePath("pages/subdir/test.pug", "layout.pug","pages/");
        assertEquals(FilenameUtils.separatorsToSystem("pages/subdir/layout.pug"),resolvePath);
    }
    @Test
    public void name7() {
        String resolvePath = pathHelper.resolvePath("pages/subdir/test.pug", "/subdir/layout.pug","pages/");
        assertEquals(FilenameUtils.separatorsToSystem("pages/subdir/layout.pug"),resolvePath);
    }
    @Test
    public void name8() {
        String resolvePath = pathHelper.resolvePath("pages/subdir/test.pug", "/layout.pug","");
        assertEquals(FilenameUtils.separatorsToSystem("/layout.pug"),resolvePath);

    }

    @Test
    public void name9() {
        String resolvePath = pathHelper.resolvePath("pages/subdir/test.pug", "../../layout.pug","");
        assertEquals(FilenameUtils.separatorsToSystem("layout.pug"),resolvePath);

    }

    @Test
    public void shouldThrowExceptionOnAbsoluteParentTemplateName() throws Exception {
        String result = pathHelper.resolvePath("/bla/index.jade","../_layout.jade", "kek");
        assertEquals(FilenameUtils.separatorsToSystem("kek/_layout.jade"),result);

    }
    @Test(expected = PugTemplateLoaderException.class)
    public void shouldThrowExceptionOnAbsoluteBasePath() throws Exception {
        String result = pathHelper.resolvePath("kek/index.jade","../_layout.jade", "/kek");
        assertEquals(FilenameUtils.separatorsToSystem("_layout.jade"),result);

    }
    @Test()
    public void shouldResolvePathWindows2() throws Exception {
        String result = pathHelper.resolvePath("kek/index.jade","../_layout.jade", "");
        assertEquals(FilenameUtils.separatorsToSystem("_layout.jade"),result);

    }
    @Test()
    public void shouldResolveClasspath() throws Exception {
        String result = pathHelper.resolvePath("kek/index.jade","../_layout.jade", "");
        assertEquals(FilenameUtils.separatorsToSystem("_layout.jade"),result);

    }
}
