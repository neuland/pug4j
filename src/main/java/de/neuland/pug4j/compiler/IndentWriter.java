package de.neuland.pug4j.compiler;

import java.io.IOException;
import java.io.Writer;

import org.apache.commons.lang3.StringUtils;

public class IndentWriter {
    private int indent = 0;
    private boolean useIndent = false;
    private Writer writer;
    private String pp = "  ";
    private boolean escape;

    public IndentWriter(Writer writer) {
        this.writer = writer;
    }
    
    public IndentWriter add(String string) {
    	return append(string);
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
		} catch (IOException e) {
			e.printStackTrace();
		}    	
    }
    
    public String toString() {
        return writer.toString();
    }

	public void newline() {
		if (isPp()) {
			write("\n" + StringUtils.repeat("  ", indent));
		}
	}
    public void prettyIndent(int offset,boolean newline){
        if (isPp()) {
            String newlineChar = newline ? "\n" : "";
            write(newlineChar + StringUtils.repeat(this.pp, indent + offset -1));
        }
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

    public boolean isPp(){
        return this.pp.length()!=0 && useIndent;
    }
}
