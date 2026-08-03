package de.neuland.pug4j.template;

import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import de.neuland.pug4j.exceptions.PugTemplateLoaderException;
import java.io.IOException;
import org.junit.Test;

public class ClasspathTemplateLoaderTest {
  @Test
  public void shouldGetFileFromClassPath() throws IOException {
    ClasspathTemplateLoader templateLoader = new ClasspathTemplateLoader();
    templateLoader.getReader("loader/pages/subdir/test.pug");
  }

  @Test
  public void shouldGetFileFromClassPathWithBasePath() throws IOException {
    ClasspathTemplateLoader templateLoader = new ClasspathTemplateLoader("loader");
    templateLoader.getReader("pages/subdir/test.pug");
  }

  @Test
  public void shouldNormalizeLeadingSlashInTemplateLoaderPath() throws IOException {
    ClasspathTemplateLoader templateLoader =
        ClasspathTemplateLoader.builder().templateLoaderPath("/loader/").build();
    templateLoader.getReader("pages/subdir/test.pug");
  }

  @Test
  public void shouldFailWithDescriptiveMessageForMissingResourcePath() {
    PugTemplateLoaderException exception =
        assertThrows(
            PugTemplateLoaderException.class,
            () -> ClasspathTemplateLoader.builder().templateLoaderPath("/no/such/dir").build());
    assertTrue(exception.getMessage().contains("/no/such/dir"));
  }
}
