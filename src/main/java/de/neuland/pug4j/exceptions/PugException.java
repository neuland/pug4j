package de.neuland.pug4j.exceptions;

import java.io.BufferedReader;
import java.io.Reader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.neuland.pug4j.Pug4J;
import de.neuland.pug4j.template.TemplateLoader;
import org.apache.commons.lang3.StringUtils;

public abstract class PugException extends RuntimeException {

    private static final long serialVersionUID = -8189536050437574552L;
    private String filename;
    private int lineNumber;
    private int colNumber;
    private TemplateLoader templateLoader;

    /**
     * Just use protected for constructor of abstract class
     * See more at <a href="https://rules.sonarsource.com/java/type/Code%20Smell/RSPEC-5993">https://rules.sonarsource.com/java/type/Code%20Smell/RSPEC-5993</a>
     *
     * @param message        Description message of exception
     * @param filename       Filename where exception was thrown
     * @param lineNumber     Linenumber where exception was thrown
     * @param templateLoader TemplateLoader to load templates
     * @param e              Thrown exception
     */
    protected PugException(String message, String filename, int lineNumber, TemplateLoader templateLoader, Throwable e) {
        super(message, e);
        this.filename = filename;
        this.lineNumber = lineNumber;
        this.templateLoader = templateLoader;
    }

    protected PugException(String message, String filename, int lineNumber, int column, TemplateLoader templateLoader, Throwable e) {
        super(message, e);
        this.filename = filename;
        this.lineNumber = lineNumber;
        this.colNumber = column;
        this.templateLoader = templateLoader;
    }

    protected PugException(String message) {
        super(message);
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
                String preamble = (curr == line ? "  > " : "    ")
                        + curr
                        + "| ";
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

    public List<String> getTemplateLines() {
        List<String> result = new ArrayList<String>();
        try (Reader reader = templateLoader.getReader(filename);
             BufferedReader in = new BufferedReader(reader)) {
            String line;
            while ((line = in.readLine()) != null) {
                result.add(line);
            }
            return result;
        } catch (Exception e) {
            return result;
        }
    }

    @Override
    public String toString() {
        return getClass() + ": " + createErrorMessage(getMessage(), lineNumber, colNumber, filename);
    }

    public String toHtmlString() {
        return toHtmlString(null);
    }

    public String toHtmlString(String generatedHtml) {
        Map<String, Object> model = new HashMap<String, Object>();
        model.put("filename", filename);
        model.put("linenumber", lineNumber);
        model.put("column", colNumber);
        model.put("message", getMessage());
        model.put("lines", getTemplateLines());
        model.put("exception", getName());
        if (generatedHtml != null) {
            model.put("html", generatedHtml);
        }

        try {
            URL url = PugException.class.getResource("/error.jade");
            return Pug4J.render(url, model, true);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private String getName() {
        return this.getClass().getSimpleName().replaceAll("([A-Z])", " $1").trim();
    }
}
