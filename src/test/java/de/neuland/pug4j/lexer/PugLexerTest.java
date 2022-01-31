package de.neuland.pug4j.lexer;

import au.com.origin.snapshots.Expect;
import au.com.origin.snapshots.junit4.SnapshotClassRule;
import au.com.origin.snapshots.junit4.SnapshotRule;
import de.neuland.pug4j.TestFileHelper;
import de.neuland.pug4j.expression.JexlExpressionHandler;
import de.neuland.pug4j.lexer.token.Token;
import de.neuland.pug4j.template.FileTemplateLoader;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.*;

import static org.apache.commons.io.comparator.NameFileComparator.*;


@RunWith(Parameterized.class)
public class PugLexerTest {
    private Expect expect;

//    public class SnapshotRule implements TestRule {
//        public SnapshotRule() {
//        }
//
//        public Statement apply(final Statement base, final Description description) {
//            return new Statement() {
//                public void evaluate() throws Throwable {
//                    SnapshotMatcher.setTestMethod(description.getTestClass().getMethod("shouldCompileJadeToHtml"));
//                    base.evaluate();
//                }
//            };
//        }
//    }
    private static String[] ignoredCases = new String[]{
            "regression.784"
            //"attr-es2015",
            //"attrs-data",
            //"attrs.js",
            //"attrs",
            //"tags.self-closing",
            //"text"
    };
    // Ensure you instantiate these rules
    @ClassRule
    public static SnapshotClassRule snapshotClassRule = new SnapshotClassRule();
    @Rule
    public SnapshotRule snapshotRule = new SnapshotRule(snapshotClassRule);
    @Rule public TestName testName = new TestName();

    private String file;

    public PugLexerTest(String file) {
        this.file = file;
    }

    @Test
    public void shouldCompileJadeToHtml() throws IOException, URISyntaxException {
        String filename = file;
        String basePath = TestFileHelper.getLexerResourcePath("/cases");
        FileTemplateLoader templateLoader = new FileTemplateLoader(basePath,  "pug");
        Lexer lexer = new Lexer(filename, templateLoader, new JexlExpressionHandler());
        LinkedList<Token> tokens = lexer.getTokens();

        expect.serializer("json").scenario(filename).toMatchSnapshot(tokens);

    }

    private String readFile(String fileName) throws IOException {
        return FileUtils.readFileToString(new File(fileName),"UTF-8");
    }

    @Parameterized.Parameters(name="{0}")
    public static Collection<String[]> data() throws FileNotFoundException, URISyntaxException {
        File folder = new File(TestFileHelper.getLexerResourcePath("/cases"));
        Collection<File> files = FileUtils.listFiles(folder, new String[]{"pug"}, false);
        File[] objects = files.stream().toArray(File[]::new);
        Arrays.sort(objects, NAME_COMPARATOR);


        Collection<String[]> data = new LinkedList<String[]>();
        for (File file : objects) {
            if (!ArrayUtils.contains(ignoredCases, file.getName().replace(".pug", ""))) {
                LoggerFactory.getLogger(PugLexerTest.class).debug(file.getName());
                data.add(new String[]{file.getName()});

            }

        }
        return data;
    }

}
