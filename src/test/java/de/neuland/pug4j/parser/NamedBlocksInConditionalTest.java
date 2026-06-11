package de.neuland.pug4j.parser;

import static org.junit.Assert.assertEquals;

import de.neuland.pug4j.PugEngine;
import de.neuland.pug4j.UnitTestSetup;
import de.neuland.pug4j.expression.ExpressionHandler;
import de.neuland.pug4j.expression.GraalJsExpressionHandler;
import de.neuland.pug4j.expression.JexlExpressionHandler;
import de.neuland.pug4j.template.PugTemplate;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

/**
 * Same-named blocks within a single template (no extends) are independent and each renders its own
 * default content — e.g. a named block in both branches of a conditional (pug#1589). The parse-time
 * block merge in {@link Parser#parseBlock} must only fire as an extends override.
 */
public class NamedBlocksInConditionalTest {

  private static final String TEMPLATE =
      """
      -if( ajax )
        block contents
          p ajax contents
      -else
        div
          block contents
            p all contents
      """;

  private String render(ExpressionHandler handler, boolean ajax) throws Exception {
    PugEngine engine =
        UnitTestSetup.createEngineFromReader(new StringReader(TEMPLATE), "inline", handler);
    PugTemplate template = engine.getTemplate("inline");
    Map<String, Object> model = new HashMap<>();
    model.put("ajax", ajax);
    return engine.render(template, model);
  }

  @Test
  public void eachBranchRendersItsOwnBlockContentWithJexl() throws Exception {
    assertEquals("<p>ajax contents</p>", render(new JexlExpressionHandler(), true));
    assertEquals("<div><p>all contents</p></div>", render(new JexlExpressionHandler(), false));
  }

  @Test
  public void eachBranchRendersItsOwnBlockContentWithGraalJs() throws Exception {
    assertEquals("<p>ajax contents</p>", render(new GraalJsExpressionHandler(), true));
    assertEquals("<div><p>all contents</p></div>", render(new GraalJsExpressionHandler(), false));
  }
}
