package de.neuland.pug4j.template;

import de.neuland.pug4j.TestFileHelper;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

public class FileTemplateLoaderTest {
    private String RESOURCE_PATH;

    @Before
    public void setUp() throws Exception {
        RESOURCE_PATH = TestFileHelper.getLoaderResourcePath("");
    }

    @Test
    public void shouldThrowExceptionIfNull() {
        FileTemplateLoader fileTemplateLoader = new FileTemplateLoader();
        assertThrows(IllegalArgumentException.class, () -> fileTemplateLoader.getReader(null));
    }

    @Test
    public void shouldGetAbsoluteFile() throws IOException {
        FileTemplateLoader fileTemplateLoader = new FileTemplateLoader();
        fileTemplateLoader.getReader(RESOURCE_PATH+"/pages/subdir/test.pug");
    }
    @Test
    public void shouldGetAbsoluteFileWithBasePath() throws IOException {
        FileTemplateLoader fileTemplateLoader = new FileTemplateLoader();
        fileTemplateLoader.setBase("pages");
        fileTemplateLoader.getReader(RESOURCE_PATH+"/pages/subdir/test.pug");
    }
    @Test
    public void shouldGetRelativeFile() throws IOException {
        FileTemplateLoader fileTemplateLoader = new FileTemplateLoader(RESOURCE_PATH);
        fileTemplateLoader.getReader("/pages/subdir/test.pug");
    }

}