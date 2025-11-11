package de.neuland.pug4j.integration;

import static org.junit.Assert.assertEquals;

import de.neuland.pug4j.Pug4J;
import de.neuland.pug4j.PugEngine;
import de.neuland.pug4j.RenderContext;
import de.neuland.pug4j.TestFileHelper;
import de.neuland.pug4j.filter.CDATAFilter;
import de.neuland.pug4j.filter.CssFilter;
import de.neuland.pug4j.filter.CustomTestFilter;
import de.neuland.pug4j.filter.JsFilter;
import de.neuland.pug4j.filter.MarkdownFilter;
import de.neuland.pug4j.filter.PlainFilter;
import de.neuland.pug4j.filter.VerbatimFilter;
import de.neuland.pug4j.helper.FormatHelper;
import de.neuland.pug4j.template.ClasspathTemplateLoader;
import de.neuland.pug4j.template.FileTemplateLoader;
import de.neuland.pug4j.template.PugTemplate;
import de.neuland.pug4j.template.ReaderTemplateLoader;
import de.neuland.pug4j.template.TemplateLoader;
import java.io.*;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class IssuesTest {
  private static final Charset FILES_ENCODING = Charset.forName("UTF-8");
  private static String[] ignoredCases = new String[] {"131"};

  private String file;

  public IssuesTest(String file) {
    this.file = file;
  }

  @Test
  public void shouldCompileJadeToHtml() throws Exception {
    FileTemplateLoader templateLoader =
        new FileTemplateLoader(TestFileHelper.getIssuesResourcePath(""), "jade");
    String templateName = file;

    compareJade(templateLoader, templateName);
  }

  @Test
  public void shouldCompileJadeToHtmlWithClasspathTemplateLoader() throws Exception {
    ClasspathTemplateLoader templateLoader =
        new ClasspathTemplateLoader(Charset.forName("UTF-8"), "jade");
    String templateName = "issues/" + file;

    compareJade(templateLoader, templateName);
  }

  @Test
  public void shouldCompileJadeToHtmlWithReaderTemplateLoader() throws Exception {
    List<String> additionalIgnoredCases =
        Arrays.asList("52", "74", "100", "104a", "104b", "123", "135", "pug015");
    if (additionalIgnoredCases.contains(file.replace(".jade", ""))) {
      return;
    }
    String issuesResourcePath = TestFileHelper.getIssuesResourcePath("");
    String pathToFile = issuesResourcePath + File.separator + file;
    InputStreamReader reader =
        new InputStreamReader(new FileInputStream(pathToFile), FILES_ENCODING);
    String templateName = file;
    ReaderTemplateLoader templateLoader = new ReaderTemplateLoader(reader, templateName);

    compareJade(templateLoader, templateName);
  }

  private void compareJade(TemplateLoader templateLoader, String templateName)
      throws IOException, URISyntaxException {
    PugEngine engine =
        PugEngine.builder()
            .templateLoader(templateLoader)
            .filter("plain", new PlainFilter())
            .filter("cdata", new CDATAFilter())
            .filter("custom-filter", new CustomTestFilter())
            .filter("marked", new MarkdownFilter())
            .filter("markdown", new MarkdownFilter())
            .filter("verbatim", new VerbatimFilter())
            .filter("js", new JsFilter())
            .filter("css", new CssFilter())
            .build();

    RenderContext context =
        RenderContext.builder()
            .prettyPrint(true)
            .defaultMode(Pug4J.Mode.XHTML) // original jade uses xhtml by default
            .build();

    PugTemplate template = engine.getTemplate(templateName);
    HashMap<String, Object> model = new HashMap<String, Object>();
    model.put("title", "Jade");
    model.put("format", new FormatHelper());
    String html = engine.render(template, model, context);

    String expected = readFile(file.replace(".jade", ".html")).trim().replaceAll("\r", "");

    assertEquals(file, expected, html.trim().replaceAll("\r", ""));
  }

  private String readFile(String fileName) throws IOException, URISyntaxException {
    return FileUtils.readFileToString(
        new File(TestFileHelper.getIssuesResourcePath(fileName)), "UTF-8");
  }

  @Parameterized.Parameters(name = "{0}")
  public static Collection<String[]> data() throws FileNotFoundException, URISyntaxException {
    File folder = new File(TestFileHelper.getIssuesResourcePath(""));
    Collection<File> files = FileUtils.listFiles(folder, new String[] {"jade"}, false);

    Collection<String[]> data = new ArrayList<String[]>();
    for (File file : files) {
      if (!ArrayUtils.contains(ignoredCases, file.getName().replace(".jade", ""))
          && !file.getName().startsWith("_")) {
        data.add(new String[] {file.getName()});
      }
    }
    return data;
  }
}
