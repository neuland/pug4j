package de.neuland.pug4j.exceptions;

import de.neuland.pug4j.template.TemplateLoader;
import java.io.BufferedReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reads template source lines for error reporting. Internal support API used at exception
 * construction time to capture a snapshot of the template source.
 */
public final class TemplateSource {

  private static final Logger logger = LoggerFactory.getLogger(TemplateSource.class);

  private TemplateSource() {}

  /**
   * Reads all lines of the given template. Never throws: a failure to read the source must not
   * mask the original template error this is being captured for.
   *
   * @param templateLoader the loader to read from, may be null
   * @param filename the template name, may be null
   * @return the template lines, or an empty list if the source cannot be read
   */
  public static List<String> readLines(TemplateLoader templateLoader, String filename) {
    if (templateLoader == null || filename == null) {
      return Collections.emptyList();
    }
    List<String> result = new ArrayList<>();
    try (Reader reader = templateLoader.getReader(filename);
        BufferedReader in = new BufferedReader(reader)) {
      String line;
      while ((line = in.readLine()) != null) {
        result.add(line);
      }
      return result;
    } catch (Exception e) {
      // also catches RuntimeException, e.g. from ReaderTemplateLoader name checks
      // single-line warn: this fires while an exception is already being constructed,
      // so a full stack trace here only buries the original error
      logger.warn("Failed to read template lines from file: {} ({})", filename, e.toString());
      logger.debug("Failed to read template lines from file: {}", filename, e);
      return result;
    }
  }
}
