package de.neuland.pug4j.exceptions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import de.neuland.pug4j.template.FileTemplateLoader;
import de.neuland.pug4j.template.ReaderTemplateLoader;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class TemplateSourceTest {

  @Rule public TemporaryFolder folder = new TemporaryFolder();

  @Test
  public void readsAllLines() throws Exception {
    Path dir = folder.getRoot().toPath();
    Files.writeString(dir.resolve("a.pug"), "div\n  p hello\n");

    List<String> lines = TemplateSource.readLines(new FileTemplateLoader(dir), "a.pug");

    assertEquals(Arrays.asList("div", "  p hello"), lines);
  }

  @Test
  public void nonexistentFileYieldsEmptyList() {
    List<String> lines =
        TemplateSource.readLines(new FileTemplateLoader(folder.getRoot().toPath()), "missing.pug");

    assertTrue(lines.isEmpty());
  }

  @Test
  public void nullLoaderOrFilenameYieldsEmptyList() {
    assertTrue(TemplateSource.readLines(null, "a.pug").isEmpty());
    assertTrue(
        TemplateSource.readLines(new ReaderTemplateLoader(new StringReader("x"), "a.pug"), null)
            .isEmpty());
  }

  @Test
  public void foreignNameOnReaderLoaderYieldsEmptyListInsteadOfThrowing() {
    ReaderTemplateLoader loader = new ReaderTemplateLoader(new StringReader("div"), "known.pug");

    // checkName throws a raw RuntimeException; it must not escape and mask the original error
    assertTrue(TemplateSource.readLines(loader, "other.pug").isEmpty());
  }

  @Test
  public void consumedReaderYieldsEmptyListInsteadOfThrowing() throws Exception {
    StringReader reader = new StringReader("div");
    reader.close();
    ReaderTemplateLoader loader = new ReaderTemplateLoader(reader, "known.pug");

    assertTrue(TemplateSource.readLines(loader, "known.pug").isEmpty());
  }
}
