package de.neuland.pug4j.exceptions;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.neuland.pug4j.Pug4J;
import de.neuland.pug4j.template.TemplateLoader;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

public abstract class PugException extends RuntimeException {

	private static final long serialVersionUID = -8189536050437574552L;
	private String filename;
	private int lineNumber;
	private int colNumber;
	private TemplateLoader templateLoader;

	public PugException(String message, String filename, int lineNumber, TemplateLoader templateLoader, Throwable e) {
		super(message, e);
		this.filename = filename;
		this.lineNumber = lineNumber;
		this.templateLoader = templateLoader;
	}
	public PugException(String message, String filename, int lineNumber, int column, TemplateLoader templateLoader, Throwable e) {
		super(message, e);
		this.filename = filename;
		this.lineNumber = lineNumber;
		this.colNumber = column;
		this.templateLoader = templateLoader;
	}

	public PugException(String message) {
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
	@NotNull
	private String createErrorMessage(String message, int line, int column, String filename) {
		String fullMessage;
		String location = line + (column !=0 ? ":" + column : "");
		List<String> lines = getTemplateLines();
		if (lines != null && lines.size()>0 && line >= 1 && line <= lines.size()) {

			int start = Math.max(line - 3, 0);
			int end = Math.min(lines.size(), line + 3);
			// Error context
			StringBuffer context = new StringBuffer();
			for (int i = start;i<end;i++){
				String text = lines.get(i);
				int curr = i + 1;
				String preamble = (curr == line ? "  > " : "    ")
						+ curr
						+ "| ";
				String out = preamble + text;
				if (curr == line && column > 0) {
					out += "\n";
					out += StringUtils.repeat("-", preamble.length() + column -1) + "^";
				}
				context.append(out);
				if(i!=end-1) {
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
		try {
			List<String> result = new ArrayList<String>();
			Reader reader = templateLoader.getReader(filename);
			BufferedReader in = new BufferedReader(reader);
			String line;
			while ((line = in.readLine()) != null) {
				result.add(line);
			}
			return result;
		} catch (IOException e) {
			return null;
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
		model.put("column",colNumber);
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
