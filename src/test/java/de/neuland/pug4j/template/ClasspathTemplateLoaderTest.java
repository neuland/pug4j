package de.neuland.pug4j.template;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import de.neuland.pug4j.exceptions.PugTemplateLoaderException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import org.apache.commons.io.IOUtils;
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

  @Test
  public void shouldFailWithFileNotFoundForMissingTemplate() {
    ClasspathTemplateLoader templateLoader = new ClasspathTemplateLoader("loader");
    FileNotFoundException exception =
        assertThrows(FileNotFoundException.class, () -> templateLoader.getReader("no-such.pug"));
    assertTrue(exception.getMessage().contains("no-such.pug"));
  }

  @Test
  public void shouldRejectTemplatePathEscapingTheLoaderPath() {
    ClasspathTemplateLoader templateLoader = new ClasspathTemplateLoader("loader");
    assertThrows(
        PugTemplateLoaderException.class, () -> templateLoader.getReader("../../secret.pug"));
  }

  @Test
  public void shouldResolveExtendsAndIncludesInsideJar() throws Exception {
    Path jar = Files.createTempFile("pug4j-templates-inheritance", ".jar");
    try (JarOutputStream out = new JarOutputStream(Files.newOutputStream(jar))) {
      // directory entries as written by standard build tooling - constructor validation needs them
      writeDir(out, "jar-inheritance/");
      writeDir(out, "jar-inheritance/pages/");
      writeDir(out, "jar-inheritance/partials/");
      writeEntry(out, "jar-inheritance/layout.pug", "html\n  body\n    block content\n");
      writeEntry(
          out,
          "jar-inheritance/pages/index.pug",
          "extends ../layout\n\nblock content\n  include ../partials/greeting\n");
      writeEntry(out, "jar-inheritance/partials/greeting.pug", "p hello from include\n");
    }

    ClassLoader original = Thread.currentThread().getContextClassLoader();
    try (URLClassLoader jarClassLoader =
        new URLClassLoader(new URL[] {jar.toUri().toURL()}, original)) {
      Thread.currentThread().setContextClassLoader(jarClassLoader);

      de.neuland.pug4j.PugConfiguration config = new de.neuland.pug4j.PugConfiguration();
      config.setTemplateLoader(
          ClasspathTemplateLoader.builder().templateLoaderPath("jar-inheritance").build());
      String html =
          config.renderTemplate(config.getTemplate("pages/index"), new java.util.HashMap<>());
      assertEquals("<html><body><p>hello from include</p></body></html>", html);
    } finally {
      Thread.currentThread().setContextClassLoader(original);
      Files.deleteIfExists(jar);
    }
  }

  private static void writeEntry(JarOutputStream out, String name, String content)
      throws IOException {
    out.putNextEntry(new JarEntry(name));
    out.write(content.getBytes(StandardCharsets.UTF_8));
    out.closeEntry();
  }

  private static void writeDir(JarOutputStream out, String name) throws IOException {
    out.putNextEntry(new JarEntry(name));
    out.closeEntry();
  }

  @Test
  public void shouldLoadTemplatesFromInsideJar() throws IOException {
    Path jar = Files.createTempFile("pug4j-templates", ".jar");
    try (JarOutputStream out = new JarOutputStream(Files.newOutputStream(jar))) {
      out.putNextEntry(new JarEntry("jar-templates/"));
      out.closeEntry();
      out.putNextEntry(new JarEntry("jar-templates/hello.pug"));
      out.write("p hello from jar".getBytes(StandardCharsets.UTF_8));
      out.closeEntry();
      out.putNextEntry(new JarEntry("jar-templates/sub/"));
      out.closeEntry();
      out.putNextEntry(new JarEntry("jar-templates/sub/nested.pug"));
      out.write("p nested".getBytes(StandardCharsets.UTF_8));
      out.closeEntry();
    }

    ClassLoader original = Thread.currentThread().getContextClassLoader();
    try (URLClassLoader jarClassLoader =
        new URLClassLoader(new URL[] {jar.toUri().toURL()}, original)) {
      Thread.currentThread().setContextClassLoader(jarClassLoader);

      ClasspathTemplateLoader templateLoader =
          ClasspathTemplateLoader.builder().templateLoaderPath("jar-templates").build();
      try (Reader reader = templateLoader.getReader("hello")) {
        assertEquals("p hello from jar", IOUtils.toString(reader));
      }
      try (Reader reader = templateLoader.getReader("sub/nested.pug")) {
        assertEquals("p nested", IOUtils.toString(reader));
      }
    } finally {
      Thread.currentThread().setContextClassLoader(original);
      Files.deleteIfExists(jar);
    }
  }
}
