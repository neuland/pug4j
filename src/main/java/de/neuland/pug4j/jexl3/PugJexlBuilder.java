package de.neuland.pug4j.jexl3;

import de.neuland.pug4j.jexl3.internal.PugJexlEngine;
import org.apache.commons.jexl3.JexlBuilder;
import org.apache.commons.jexl3.JexlEngine;

public class PugJexlBuilder extends JexlBuilder {
  @Override
  public JexlEngine create() {
    return new PugJexlEngine(this);
  }
}
