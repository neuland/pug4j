package de.neuland.pug4j.exceptions;

import de.neuland.pug4j.template.TemplateLoader;
import java.util.List;

public class PugLexerException extends PugException {

  private static final long serialVersionUID = -4390591022593362563L;

  public PugLexerException(
      String message, String filename, int lineNumber, int column, List<String> templateLines) {
    super(message, filename, lineNumber, column, templateLines, null);
  }

  public PugLexerException(
      String code,
      String message,
      String filename,
      int lineNumber,
      int column,
      List<String> templateLines) {
    this(message, filename, lineNumber, column, templateLines);
  }

  /**
   * @deprecated As of 3.0.0, use {@link #PugLexerException(String, String, int, int, List)}
   *     instead.
   */
  @Deprecated(since = "3.0.0", forRemoval = true)
  public PugLexerException(
      String message, String filename, int lineNumber, int column, TemplateLoader templateLoader) {
    this(
        message,
        filename,
        lineNumber,
        column,
        TemplateSource.readLines(templateLoader, filename));
  }

  /**
   * @deprecated As of 3.0.0, use {@link #PugLexerException(String, String, String, int, int, List)}
   *     instead.
   */
  @Deprecated(since = "3.0.0", forRemoval = true)
  public PugLexerException(
      String code,
      String message,
      String filename,
      int lineNumber,
      int column,
      TemplateLoader templateLoader) {
    this(message, filename, lineNumber, column, templateLoader);
  }
}
