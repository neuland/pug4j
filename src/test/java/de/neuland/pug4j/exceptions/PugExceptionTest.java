package de.neuland.pug4j.exceptions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.nio.file.Paths;
import java.util.HashMap;

import de.neuland.pug4j.template.FileTemplateLoader;
import org.apache.commons.io.FileUtils;
import org.junit.Ignore;
import org.junit.Test;

import de.neuland.pug4j.Pug4J;
import de.neuland.pug4j.TestFileHelper;
import org.slf4j.LoggerFactory;

public class PugExceptionTest {

	@Test
	public void test() throws Exception {
		String errorJade = TestFileHelper.getCompilerResourcePath("exceptions/error.jade");
		String exceptionHtml = TestFileHelper.getCompilerResourcePath("exceptions/error.html");
		try {
			Pug4J.render(errorJade, new HashMap<String, Object>());
			fail();
		} catch (PugException e) {
			assertTrue(e.getMessage().startsWith("unable to evaluate [non.existing.query()]"));
			assertEquals(9, e.getLineNumber());
			assertEquals(errorJade, e.getFilename());
			String expectedHtml = readFile(exceptionHtml);
			String html = e.toHtmlString("<html><head><title>broken");
			LoggerFactory.getLogger(this.getClass()).debug("Expected: "+ expectedHtml+", Actual: "+html);
			assertEquals(removeAbsolutePath(expectedHtml.replaceAll("\r", "")), removeAbsolutePath(html.replaceAll("\r", "")));
		}
	}

	@Test
	public void testMessage() {
		try {
			throw new PugLexerException("invalid indentation; expecting 2 spaces", "index.jade", 10,20, new FileTemplateLoader(TestFileHelper.getLexerResourcePath("")));
		}catch(Exception e){
			assertEquals("invalid indentation; expecting 2 spaces",e.getMessage());
			assertEquals("class de.neuland.pug4j.exceptions.PugLexerException: index.jade:10:20\n" +
					"\n" +
					"invalid indentation; expecting 2 spaces",e.toString());
		}

	}

	private String removeAbsolutePath(String html) {
		html = html.replaceAll("(<h2>In ).*(compiler/exceptions/error\\.jade at line 9, column 0\\.</h2>)", "$1\\.\\./compiler/exceptions/error\\.jade at line 9, column 0\\.</h2>");
		html = html.replaceAll("(\\s)[^\\s]*(compiler/exceptions/error\\.jade:9)", "$1\\.\\./compiler/exceptions/error\\.jade:9");
		html = html.replaceAll("(<h2>In ).*(compiler\\\\exceptions\\\\error\\.jade at line 9, column 0\\.</h2>)", "$1\\.\\./compiler/exceptions/error\\.jade at line 9, column 0\\.</h2>");
		html = html.replaceAll("(\\s)[^\\s]*(compiler\\\\exceptions\\\\error\\.jade:9)", "$1\\.\\./compiler/exceptions/error\\.jade:9");
		return html;
	}

	private String readFile(String fileName) {
		try {
			return FileUtils.readFileToString(new File(fileName),"UTF-8");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}
}
