package de.neuland.pug4j;

import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.net.URISyntaxException;
import java.nio.file.Paths;


public class TestFileHelper {

    public static final String TESTFILE_LEXER_FOLDER = "/lexer/";
    public static final String TESTFILE_LEXER_0_0_8_FOLDER = "/lexer_0.0.8/";
    public static final String TESTFILE_PARSER_FOLDER = "/parser/";
    public static final String TESTFILE_COMPILER_FOLDER = "/compiler/";
    public static final String TESTFILE_LOADER_FOLDER = "/loader/";
    public static final String TESTFILE_ORIGINAL_FOLDER = "/originalTests/";
	public static final String TESTFILE_20190911_ORIGINAL_FOLDER = "/originalTests20190911/";
	public static final String TESTFILE_20150927_ORIGINAL_FOLDER = "/originalTests20150927/";
    public static final String TESTFILE_20150515_ORIGINAL_FOLDER = "/originalTests20150515/";
    public static final String TESTFILE_PUG2_ORIGINAL_FOLDER = "/pug@2.0.4/";
    public static final String TESTFILE_PUG3_ORIGINAL_FOLDER = "/pugjs@3.0.2/";
    public static final String TESTFILE_PUG4J_FOLDER = "/tests/";
    public static final String TESTFILE_PUG4J_GRAALVM_FOLDER = "/testsGraalVM/";
    public static final String TESTFILE_PUG2_ADJUSTED_FOLDER = "/pug@2.0.4_adjusted/";
    public static final String TESTFILE_ISSUES_FOLDER = "/issues/";
    public static final String TESTFILE_COMPILER_ERROR_FOLDER = "/compiler/errors/";
    
	public static String getResourcePath(String fileName) throws FileNotFoundException,URISyntaxException {
		String path = Paths.get(TestFileHelper.class.getResource(fileName).toURI()).toString();
		LoggerFactory.getLogger(TestFileHelper.class.getClass()).debug(path);
		return path;
	}

	public static String getRootResourcePath() throws FileNotFoundException,URISyntaxException {
		return getResourcePath("/");
	}

	public static String getLexerResourcePath(String fileName) throws FileNotFoundException,URISyntaxException {
		return getResourcePath(TESTFILE_LEXER_FOLDER + fileName);
	}
    public static String getLexer_0_0_8_ResourcePath(String fileName) throws FileNotFoundException,URISyntaxException {
        return getResourcePath(TESTFILE_LEXER_0_0_8_FOLDER + fileName);
    }
    
	public static String getParserResourcePath(String fileName) throws FileNotFoundException,URISyntaxException {
		return getResourcePath(TESTFILE_PARSER_FOLDER + fileName);
	}
    
	public static String getCompilerResourcePath(String fileName) throws FileNotFoundException,URISyntaxException {
		return getResourcePath(TESTFILE_COMPILER_FOLDER + fileName);
	}
	
	public static String getOriginalResourcePath(String fileName) throws FileNotFoundException,URISyntaxException {
		return getResourcePath(TESTFILE_ORIGINAL_FOLDER + fileName);
	}
	public static String getLoaderResourcePath(String fileName) throws FileNotFoundException,URISyntaxException {
		return getResourcePath(TESTFILE_LOADER_FOLDER + fileName);
	}
	public static String getOriginal20150927ResourcePath(String fileName) throws FileNotFoundException,URISyntaxException {
		return getResourcePath(TESTFILE_20150927_ORIGINAL_FOLDER + fileName);
	}

	public static String getOriginal20190911ResourcePath(String fileName) throws FileNotFoundException,URISyntaxException {
		return getResourcePath(TESTFILE_20190911_ORIGINAL_FOLDER + "/cases/" + fileName);
	}

	public static String getOriginal20150515ResourcePath(String fileName) throws FileNotFoundException,URISyntaxException {
		return getResourcePath(TESTFILE_20150515_ORIGINAL_FOLDER + fileName);
	}
	public static String getOriginalPug2ResourcePath(String fileName) throws FileNotFoundException,URISyntaxException {
		return getResourcePath(TESTFILE_PUG2_ORIGINAL_FOLDER + fileName);
	}
	public static String getOriginalPug3ResourcePath(String fileName) throws FileNotFoundException,URISyntaxException {
		return getResourcePath(TESTFILE_PUG3_ORIGINAL_FOLDER + fileName);
	}
	public static String getPug4JTestsResourcePath(String fileName) throws FileNotFoundException,URISyntaxException {
		return getResourcePath(TESTFILE_PUG4J_FOLDER + fileName);
	}
	public static String getPug4JGraalVMTestsResourcePath(String fileName) throws FileNotFoundException,URISyntaxException {
		return getResourcePath(TESTFILE_PUG4J_GRAALVM_FOLDER + fileName);
	}
	public static String getAdjustedPug2ResourcePath(String fileName) throws FileNotFoundException,URISyntaxException {
		return getResourcePath(TESTFILE_PUG2_ADJUSTED_FOLDER + fileName);
	}
	public static String getIssuesResourcePath(String fileName) throws FileNotFoundException,URISyntaxException {
		return getResourcePath(TESTFILE_ISSUES_FOLDER + fileName);
	}

	public static String getCompilerErrorsResourcePath(String fileName) throws FileNotFoundException,URISyntaxException  {
		return getResourcePath(TESTFILE_COMPILER_ERROR_FOLDER + fileName);
	}
}
