package de.neuland.pug4j;

import de.neuland.pug4j.exceptions.PugException;
import de.neuland.pug4j.template.PugTemplate;
import de.neuland.pug4j.template.ReaderTemplateLoader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.text.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Renders a styled HTML error page for a {@link PugException} — typically used by web applications
 * to show a debug page when template processing fails:
 *
 * <pre>{@code
 * try {
 *     html = engine.render(template, model);
 * } catch (PugException e) {
 *     response.status(500).send(PugErrorRenderer.renderHtml(e));
 * }
 * }</pre>
 *
 * <p>The page shows the exception type, message, template location, and the template source around
 * the failing line (as captured in the exception). Rendering uses an internal engine that is
 * independent of any user configuration, so a broken expression handler or filter setup cannot
 * break the error page.
 *
 * @since 3.0.0
 */
public final class PugErrorRenderer {

  private static final Logger logger = LoggerFactory.getLogger(PugErrorRenderer.class);
  private static final String ERROR_TEMPLATE = "/error.jade";

  private PugErrorRenderer() {}

  /** Lazy holder for the parsed error page template and its engine. */
  private static final class ErrorTemplateHolder {
    static final PugEngine ENGINE;
    static final PugTemplate TEMPLATE;

    static {
      PugEngine engine = null;
      PugTemplate template = null;
      try (InputStream in = PugErrorRenderer.class.getResourceAsStream(ERROR_TEMPLATE)) {
        engine =
            PugEngine.builder()
                .templateLoader(
                    new ReaderTemplateLoader(
                        new InputStreamReader(in, StandardCharsets.UTF_8), ERROR_TEMPLATE))
                .caching(false)
                .build();
        template = engine.getTemplate(ERROR_TEMPLATE);
      } catch (IOException | RuntimeException e) {
        logger.error("Failed to load error page template {}", ERROR_TEMPLATE, e);
      }
      ENGINE = engine;
      TEMPLATE = template;
    }
  }

  /**
   * Renders an HTML error page for the given exception.
   *
   * @param exception the exception to render
   * @return the error page HTML; falls back to a minimal page if the error template cannot be
   *     rendered, never null
   */
  public static String renderHtml(PugException exception) {
    return renderHtml(exception, null);
  }

  /**
   * Renders an HTML error page for the given exception, including the HTML generated before the
   * error occurred.
   *
   * @param exception the exception to render
   * @param generatedHtml the partial output generated before the error, may be null
   * @return the error page HTML; falls back to a minimal page if the error template cannot be
   *     rendered, never null
   */
  public static String renderHtml(PugException exception, String generatedHtml) {
    Map<String, Object> model = new HashMap<>();
    model.put("filename", exception.getFilename());
    model.put("linenumber", exception.getLineNumber());
    model.put("column", exception.getColNumber());
    model.put("message", exception.getMessage());
    model.put("lines", exception.getTemplateLines());
    model.put("exception", prettyName(exception));
    if (generatedHtml != null) {
      model.put("html", generatedHtml);
    }

    try {
      if (ErrorTemplateHolder.TEMPLATE == null) {
        return fallbackPage(exception);
      }
      RenderContext context = RenderContext.builder().prettyPrint(true).build();
      return ErrorTemplateHolder.ENGINE.render(ErrorTemplateHolder.TEMPLATE, model, context);
    } catch (RuntimeException e) {
      logger.error("Failed to render error template for exception: {}", prettyName(exception), e);
      return fallbackPage(exception);
    }
  }

  private static String prettyName(PugException exception) {
    return exception.getClass().getSimpleName().replaceAll("([A-Z])", " $1").trim();
  }

  private static String fallbackPage(PugException exception) {
    return "<!DOCTYPE html><html><head><title>"
        + StringEscapeUtils.escapeHtml4(prettyName(exception))
        + "</title></head><body><h1>"
        + StringEscapeUtils.escapeHtml4(prettyName(exception))
        + "</h1><p>"
        + StringEscapeUtils.escapeHtml4(
            exception.getFilename()
                + ":"
                + exception.getLineNumber()
                + ":"
                + exception.getColNumber())
        + "</p><pre>"
        + StringEscapeUtils.escapeHtml4(exception.getMessage())
        + "</pre></body></html>";
  }
}
