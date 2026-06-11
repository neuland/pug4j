package de.neuland.pug4j.exceptions;

import de.neuland.pug4j.template.TemplateLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang3.StringUtils;

public abstract class PugException extends RuntimeException {

  private static final long serialVersionUID = -8189536050437574552L;
  private String filename;
  private int lineNumber;
  private int colNumber;
  private final List<String> templateLines;

  /**
   * Creates an exception carrying a snapshot of the template source for error reporting.
   *
   * @param message Description message of exception
   * @param filename Filename where exception was thrown
   * @param lineNumber Linenumber where exception was thrown
   * @param colNumber Column where exception was thrown
   * @param templateLines Snapshot of the template source lines, may be null
   * @param cause Thrown exception
   */
  protected PugException(
      String message,
      String filename,
      int lineNumber,
      int colNumber,
      List<String> templateLines,
      Throwable cause) {
    super(message, cause);
    this.filename = filename;
    this.lineNumber = lineNumber;
    this.colNumber = colNumber;
    this.templateLines =
        templateLines == null
            ? Collections.emptyList()
            : Collections.unmodifiableList(new ArrayList<>(templateLines));
  }

  /**
   * @deprecated As of 3.0.0, replaced by {@link #PugException(String, String, int, int, List,
   *     Throwable)}. Exceptions no longer hold a TemplateLoader; the template source is captured
   *     at construction time instead.
   */
  @Deprecated(since = "3.0.0", forRemoval = true)
  protected PugException(
      String message, String filename, int lineNumber, TemplateLoader templateLoader, Throwable e) {
    this(message, filename, lineNumber, 0, TemplateSource.readLines(templateLoader, filename), e);
  }

  /**
   * @deprecated As of 3.0.0, replaced by {@link #PugException(String, String, int, int, List,
   *     Throwable)}. Exceptions no longer hold a TemplateLoader; the template source is captured
   *     at construction time instead.
   */
  @Deprecated(since = "3.0.0", forRemoval = true)
  protected PugException(
      String message,
      String filename,
      int lineNumber,
      int column,
      TemplateLoader templateLoader,
      Throwable e) {
    this(
        message, filename, lineNumber, column, TemplateSource.readLines(templateLoader, filename), e);
  }

  protected PugException(String message) {
    super(message);
    this.templateLines = Collections.emptyList();
  }

  public String getFilename() {
    return filename;
  }

  public int getLineNumber() {
    return lineNumber;
  }

  public int getColNumber() {
    return colNumber;
  }

  private String createErrorMessage(String message, int line, int column, String filename) {
    String fullMessage;
    String location = line + (column != 0 ? ":" + column : "");
    List<String> lines = getTemplateLines();
    if (lines != null && lines.size() > 0 && line >= 1 && line <= lines.size()) {

      int start = Math.max(line - 3, 0);
      int end = Math.min(lines.size(), line + 3);
      // Error context
      StringBuffer context = new StringBuffer();
      for (int i = start; i < end; i++) {
        String text = lines.get(i);
        int curr = i + 1;
        String preamble = (curr == line ? "  > " : "    ") + curr + "| ";
        String out = preamble + text;
        if (curr == line && column > 0) {
          out += "\n";
          out += StringUtils.repeat("-", preamble.length() + column - 1) + "^";
        }
        context.append(out);
        if (i != end - 1) {
          context.append("\n");
        }
      }

      fullMessage = filename + ":" + location + "\n" + context.toString() + "\n\n" + message;
    } else {
      fullMessage = filename + ":" + location + "\n\n" + message;
    }
    return fullMessage;
  }

  /**
   * Returns the template source lines captured when this exception was constructed. Unlike in
   * 2.x, the lines are a snapshot taken at construction time and are not re-read from the
   * template loader.
   *
   * @return the template lines, or an empty list if the source could not be read
   */
  public List<String> getTemplateLines() {
    return templateLines;
  }

  @Override
  public String toString() {
    return getClass().getName()
        + ": "
        + createErrorMessage(getMessage(), lineNumber, colNumber, filename);
  }

  /**
   * @deprecated As of 3.0.0, use {@link de.neuland.pug4j.PugErrorRenderer#renderHtml(PugException)}
   *     instead.
   */
  @Deprecated(since = "3.0.0", forRemoval = true)
  public String toHtmlString() {
    return toHtmlString(null);
  }

  /**
   * @deprecated As of 3.0.0, use {@link de.neuland.pug4j.PugErrorRenderer#renderHtml(PugException,
   *     String)} instead.
   */
  @Deprecated(since = "3.0.0", forRemoval = true)
  public String toHtmlString(String generatedHtml) {
    return de.neuland.pug4j.PugErrorRenderer.renderHtml(this, generatedHtml);
  }
}
