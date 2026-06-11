package de.neuland.pug4j.exceptions;

import de.neuland.pug4j.parser.node.Node;
import de.neuland.pug4j.template.TemplateLoader;
import java.util.List;

public class PugCompilerException extends PugException {

  private static final long serialVersionUID = -126617495230190225L;

  public PugCompilerException(Node node, List<String> templateLines, Throwable e) {
    super(
        e.getMessage(), node.getFileName(), node.getLineNumber(), node.getColumn(), templateLines, e);
  }

  public PugCompilerException(Node node, List<String> templateLines, String message) {
    super(message, node.getFileName(), node.getLineNumber(), node.getColumn(), templateLines, null);
  }

  /**
   * @deprecated As of 3.0.0, use {@link #PugCompilerException(Node, List, Throwable)} instead.
   */
  @Deprecated(since = "3.0.0", forRemoval = true)
  public PugCompilerException(Node node, TemplateLoader templateLoader, Throwable e) {
    this(node, TemplateSource.readLines(templateLoader, node.getFileName()), e);
  }

  /**
   * @deprecated As of 3.0.0, use {@link #PugCompilerException(Node, List, String)} instead.
   */
  @Deprecated(since = "3.0.0", forRemoval = true)
  public PugCompilerException(Node node, TemplateLoader templateLoader, String message) {
    this(node, TemplateSource.readLines(templateLoader, node.getFileName()), message);
  }
}
