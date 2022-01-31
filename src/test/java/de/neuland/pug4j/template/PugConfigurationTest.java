package de.neuland.pug4j.template;

import de.neuland.pug4j.PugConfiguration;
import de.neuland.pug4j.TestFileHelper;
import de.neuland.pug4j.parser.Parser;
import de.neuland.pug4j.parser.node.Node;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;

import static org.junit.Assert.*;

public class PugConfigurationTest {

    protected Parser parser;
    protected Node root;

    private String templatePath;

    @Before
    public void setUp() throws Exception {
        templatePath = TestFileHelper.getParserResourcePath("assignment.jade");
    }

    @Test
    public void testGetTemplate() throws IOException {
        PugConfiguration config = new PugConfiguration();
        PugTemplate template = config.getTemplate(templatePath);
        assertNotNull(template);
    }

    @Test
    public void testGetTemplateWithBasepath() throws IOException, URISyntaxException {
        PugConfiguration config = new PugConfiguration();
        config.setTemplateLoader(new FileTemplateLoader(TestFileHelper.getRootResourcePath() + "/parser/", "jade"));
        PugTemplate template = config.getTemplate("assignment");
        assertNotNull(template);
    }


    @Test
    public void testCache() throws IOException {
        PugConfiguration config = new PugConfiguration();
        config.setCaching(true);
        PugTemplate template = config.getTemplate(templatePath);
        assertNotNull(template);
        PugTemplate template2 = config.getTemplate(templatePath);
        assertNotNull(template2);
        assertSame(template, template2);
    }

    @Test
	public void testExceptionOnUnknowwTemplate() throws IOException {
    	PugConfiguration config = new PugConfiguration();
    	PugTemplate template = null;
    	try {
    		template = config.getTemplate("UNKNOWN_PATH");
    		fail("Did expect TemplatException!");
    	} catch (IOException | UncheckedIOException ignore) {
    		
    	}
    	assertNull(template);
    }

    @Test
    public void testPrettyPrint() throws IOException {
        PugConfiguration config = new PugConfiguration();
        config.setPrettyPrint(true);
        PugTemplate template = config.getTemplate(templatePath);
        assertTrue(template.isPrettyPrint());
    }

    @Test
    public void testRootNode() throws IOException {
        PugConfiguration config = new PugConfiguration();
        PugTemplate template = config.getTemplate(templatePath);
        assertNotNull(template.getRootNode());
    }

}
