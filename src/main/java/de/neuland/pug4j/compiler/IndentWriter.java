package de.neuland.pug4j.compiler;

import java.io.IOException;
import java.io.Writer;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IndentWriter {
  private static final Logger logger = LoggerFactory.getLogger(IndentWriter.class);
  public static final String INDENT = "  ";
  private static final String[] CACHED_INDENTS = new String[33];

  static {
    for (int i = 0; i < CACHED_INDENTS.length; i++) {
      CACHED_INDENTS[i] = StringUtils.repeat(INDENT, i);
    }
  }

  private int indent = 0;
  private boolean useIndent = false;
  private final Writer writer;
  private boolean escape;
  private char lastChar = 0;

  public IndentWriter(Writer writer) {
    this.writer = writer;
  }

  public IndentWriter append(String string) {
    write(string);
    return this;
  }

  public void increment() {
    indent++;
  }

  public void decrement() {
    indent--;
  }

  private void write(String string) {
    try {
      writer.write(string);
      if (!string.isEmpty()) {
        lastChar = string.charAt(string.length() - 1);
      }
    } catch (IOException e) {
      logger.error("Failed to write to output: {}", string, e);
      throw new RuntimeException("Failed to write template output", e);
    }
  }

  /**
   * Whether the last written character was a newline. Needed for pretty-printing after filters,
   * whose output is only known at render time (unlike pug.js, where filters run at compile time and
   * their output is part of a text node).
   */
  public boolean isLastCharNewline() {
    return lastChar == '\n';
  }

  public String toString() {
    return writer.toString();
  }

  public void newline() {
    if (isPp()) {
      write("\n");
      write(indentString(indent));
    }
  }

  public void prettyIndent(int offset, boolean newline) {
    if (isPp()) {
      if (newline) {
        write("\n");
      }
      write(indentString(indent + offset - 1));
    }
  }

  private static String indentString(int level) {
    if (level <= 0) {
      return "";
    }
    if (level < CACHED_INDENTS.length) {
      return CACHED_INDENTS[level];
    }
    return StringUtils.repeat(INDENT, level);
  }

  public void setUseIndent(boolean useIndent) {
    this.useIndent = useIndent;
  }

  public void setEscape(boolean escape) {
    this.escape = escape;
  }

  public boolean isEscape() {
    return escape;
  }

  public boolean isPp() {
    return useIndent;
  }
}
