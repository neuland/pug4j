package de.neuland.pug4j.jexl3.internal;

import org.apache.commons.jexl3.*;
import org.apache.commons.jexl3.internal.Engine;
import org.apache.commons.jexl3.internal.Frame;
import org.apache.commons.jexl3.internal.Interpreter;

public class PugJexlEngine extends Engine {
  public PugJexlEngine(final JexlBuilder conf) {
    super(conf);
  }

  /*
   * using a semi strict interpreter and non strict arithmetic
   */
  protected Interpreter createInterpreter(
      final JexlContext context, final Frame frame, final JexlOptions opts) {
    return new PugJexlInterpreter(this, opts, context == null ? EMPTY_CONTEXT : context, frame);
  }
}
