package de.neuland.pug4j.exceptions;

import de.neuland.pug4j.template.TemplateLoader;
import java.util.List;

public class PugParserException extends PugException {

  private static final long serialVersionUID = 2022663314591205451L;
  String code = "";

  public PugParserException(
      String filename, int lineNumber, int column, String message, List<String> templateLines) {
    super(message, filename, lineNumber, column, templateLines, null);
  }

  public PugParserException(
      String filename,
      int lineNumber,
      int column,
      String message,
      String code,
      List<String> templateLines) {
    super(message, filename, lineNumber, column, templateLines, null);
    this.code = code;
  }

  /**
   * @deprecated As of 3.0.0, use {@link #PugParserException(String, int, int, String, List)}
   *     instead.
   */
  @Deprecated(since = "3.0.0", forRemoval = true)
  public PugParserException(
      String filename, int lineNumber, int column, TemplateLoader templateLoader, String message) {
    this(filename, lineNumber, column, message, TemplateSource.readLines(templateLoader, filename));
  }

  /**
   * @deprecated As of 3.0.0, use {@link #PugParserException(String, int, int, String, String,
   *     List)} instead.
   */
  @Deprecated(since = "3.0.0", forRemoval = true)
  public PugParserException(
      String filename,
      int lineNumber,
      int column,
      TemplateLoader templateLoader,
      String message,
      String code) {
    this(
        filename,
        lineNumber,
        column,
        message,
        code,
        TemplateSource.readLines(templateLoader, filename));
  }
}
