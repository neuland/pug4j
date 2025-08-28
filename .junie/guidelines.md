### Project-specific build and configuration

- Build system: Maven (pom.xml). Minimum Maven 3.0 enforced by maven-enforcer-plugin. Artifact coordinates: de.neuland-bfi:pug4j.
- Java version: Code is compiled for Java 8 bytecode (maven.compiler.source/target=1.8). When building with JDK ≥ 11, profile `javac-release` sets `<maven.compiler.release>8</maven.compiler.release>` to produce Java 8 compatible binaries.
- Quick build/install:
  - mvn -v  # verify Maven is available
  - mvn -T1C -DskipTests install  # parallel build, skip tests if desired
- Key plugins:
  - maven-compiler-plugin 3.13.0
  - maven-surefire-plugin 3.5.2 (JUnit 4 provider, module path disabled)
  - maven-source-plugin, maven-javadoc-plugin for publishing
- Dependencies relevant to runtime vs testing:
  - Runtime: slf4j-api 2.0.x, commons-* libraries, gson, caffeine, flexmark, optional GraalVM runtime deps (js, js-scriptengine, tools) are declared with scope runtime. You don’t need a GraalVM JDK; standard HotSpot JDK works, but Graal JS will be available at runtime via those deps when selected.

### Testing: how to configure and run

- Frameworks:
  - JUnit 4.13.2
  - Hamcrest 1.3 (assertions)
  - Snapshot testing: io.github.origin-energy java-snapshot-testing-junit4 (+ Jackson plugin) for data/regression tests. Snapshot files live under __snapshots__ directories alongside tests (e.g., src/test/java/.../__snapshots__).
  - Logging during tests: slf4j-simple (test scope) prints to console.
- Running tests:
  - All tests: mvn test
  - Single class: mvn -Dtest=Pug4JTest test
  - Single method: mvn -Dtest=Pug4JTest#testRenderDefault test
  - Repeat failed tests only: mvn -Dsurefire.rerunFailingTestsCount=1 test
- Snapshot tests:
  - Snapshot tests use Expect + SnapshotRule/ClassRule (see lexer/PugLexerTest.java). Snapshots are stored in .snap files. If behavior changes legitimately, update snapshots. The Origin Energy Snapshot Testing lib supports updating snapshots via a system property. Typical usage is:
    - mvn -Dsnapshots.update=true -Dtest=PugLexerTest test
    Consult the library docs if your environment needs a different property name/version behavior.
- Test resources/layout helpers:
  - TestFileHelper centralizes resource roots (lexer, parser, compiler, etc.) and resolves absolute paths via ClassLoader resources. Prefer using these helpers to avoid brittle paths.
  - Example: TestFileHelper.getLexerResourcePath("/cases") returns a filesystem path to src/test/resources/lexer/cases.
- Surefire configuration specifics:
  - useModulePath=false prevents JPMS/module path interference when testing on JDK 9+.
  - useFile=false prints test output to console instead of dumping to files.

### Verified example test run

- I executed a representative test locally in this repository to validate the test setup:
  - mvn -Dtest=Pug4JTest#testRenderDefault test
  - Result: BUILD SUCCESS, 1 test run, 0 failures.

### Adding a new test (recommended patterns)

- JUnit 4 style is used throughout. For basic rendering logic you can use either the static convenience API or a configured PugConfiguration. To avoid adding files when not necessary, ReaderTemplateLoader is available for in-memory templates. If your test exercises includes/extends, use FileTemplateLoader and place associated .pug files in src/test/resources.

- Minimal in-memory render test (no filesystem I/O):
  - File: src/test/java/de/neuland/pug4j/SimpleRenderTest.java
  - Contents:
    - import de.neuland.pug4j.PugConfiguration;
    - import de.neuland.pug4j.template.ReaderTemplateLoader;
    - import de.neuland.pug4j.template.PugTemplate;
    - import org.junit.Test;
    - import java.io.StringReader;
    - import java.util.HashMap;
    - import static org.junit.Assert.assertEquals;
    - public class SimpleRenderTest {
        @Test
        public void rendersInlineTemplate() throws Exception {
          String pug = "h1 Hello\np Welcome to pug4j";
          PugConfiguration config = new PugConfiguration();
          ReaderTemplateLoader loader = new ReaderTemplateLoader(new StringReader(pug), "inline");
          config.setTemplateLoader(loader);
          PugTemplate template = config.getTemplate("inline");
          String html = config.renderTemplate(template, new HashMap<>());
          assertEquals("<h1>Hello</h1><p>Welcome to pug4j</p>", html);
        }
      }
  - Run: mvn -Dtest=SimpleRenderTest test

- Filesystem-based template test (for includes/extends):
  - Place templates under src/test/resources/mycase, e.g., layout.pug and index.pug.
  - Use FileTemplateLoader with a safe base path (see Template Loader notes below).
  - Example snippet:
    - import de.neuland.pug4j.template.FileTemplateLoader;
    - Path baseDir = Paths.get("src/test/resources/mycase");
    - FileTemplateLoader loader = new FileTemplateLoader(baseDir.toString(), "pug");
    - PugConfiguration cfg = new PugConfiguration();
    - cfg.setTemplateLoader(loader);
    - PugTemplate tpl = cfg.getTemplate("index.pug");

- Parameterized/snapshot patterns:
  - See lexer/PugLexerTest.java for a canonical parameterized test that lists PUG cases from a directory and snapshot-verifies token streams. If you add new cases, run tests once to see snapshot diffs, then update snapshots if the new expected behavior is correct.

### Project-specific development notes

- Template loader security and base path
  - FileTemplateLoader constrains template resolution to the configured base directory. Absolute-rooted references (starting with "/") are resolved relative to the loader’s base; parent directory traversal outside the base is rejected. Tests assert this behavior (e.g., Pug4JTest#testRenderDefaultWithTemplateOutsideTemplateLoaderPath expecting PugParserException).
- Expression handlers
  - Default: JEXL (JexlExpressionHandler). Behavior is JS-like but not identical. Non-existing values/properties evaluate to null/false; invalid method calls raise PugCompilerException. Reserved words (new, size, empty) require bracket access (obj["size"]). See README’s Expressions section.
  - Alternative: GraalJsExpressionHandler for pure JS expressions. Significantly slower; only enable when necessary. Graal JS artifacts are runtime dependencies already in pom.
- Output formatting
  - Pretty print can be enabled via PugConfiguration#setPrettyPrint(true). The deprecated knob in de.neuland.pug4j.compiler.Compiler#setPrettyPrint is scheduled for removal in 3.0.0 and should not be used in new code. Default output is minified.
- Modes and doctypes
  - If no doctype is present, output defaults to HTML. You can force modes via PugConfiguration#setMode(…): HTML/XHTML/XML. Tests cover XHTML/XML attribute rendering differences.
- Deprecated surfaces slated for 3.0.0 removal
  - Compiler(Node), Compiler#setPrettyPrint, Compiler#setTemplate are marked deprecated in comments. Prefer PugConfiguration/PugTemplate APIs.
- Test resource organization
  - src/test/resources is organized by concern (lexer, parser, compiler, etc.) and also contains ports of upstream pugjs tests (pug@2.0.4, pugjs@3.0.2). Use TestFileHelper constants and accessors to keep paths consistent and CI-friendly.
- Surefire/JPMS
  - Surefire is configured with useModulePath=false to avoid JPMS module path classification under JDK 9+. If you add tests using newer JDK features, keep them Java 8 compatible or add conditional profiles.
- Logging
  - slf4j-simple is used in tests only; runtime consumers are expected to bring their own SLF4J binding. Avoid committing tests that assume a specific logging backend unless guarded.

### Known pitfalls

- .jade templates are not supported anymore; attempting to render them should raise PugParserException (see Pug4JTest#testRenderJade). Keep legacy resources named .pug or update tests to expect failures for .jade.
- ReaderTemplateLoader only supports a single logical name and will throw if templates reference includes/extends. Use FileTemplateLoader for multi-file tests.
- Snapshot tests can be brittle across OS/platforms if they encode path separators or locale-dependent formats. Keep snapshots normalized or assert on structures/serializers with stable ordering.

### CI and release bits (for reference)

- The repo contains a GitHub Actions test workflow (badge in README). Local behavior should mirror CI: mvn -B -ntp -DskipTests=false test.
- Release flow uses maven-release-plugin; artifacts are signed when -DperformRelease=true activates profile release-sign-artifacts. Not required for local development.

### What was verified now and what you need to do

- Verified now:
  - mvn -Dtest=Pug4JTest#testRenderDefault test passed locally, confirming the test toolchain and resources are wired correctly.
- To reproduce locally:
  - Ensure JDK 8–21 (building with ≥11 is fine; bytecode targets 8).
  - Run mvn test for the full suite or use -Dtest to scope.
- To add the demonstration test:
  - Create src/test/java/de/neuland/pug4j/SimpleRenderTest.java with the in-memory example above.
  - Run mvn -Dtest=SimpleRenderTest test and ensure it passes.
  - If you temporarily added any templates under src/test/resources for experiments, clean them up before committing unless they are part of intended coverage.
