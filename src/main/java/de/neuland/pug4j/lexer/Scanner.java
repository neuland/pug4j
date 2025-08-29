package de.neuland.pug4j.lexer;

import java.io.BufferedReader;
import java.io.Reader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

public class Scanner {

    private String input;
    private String originalInput;
    public static final String UTF8_BOM = "\uFEFF";

    public Scanner(Reader reader) {
        initFromReader(reader);
    }

    public Scanner(String input) {
        this.input = input;
    }

    public void consume(int length) {
        input = input.substring(length);
    }

    private void initFromReader(Reader reader) {
        try {
            BufferedReader in = new BufferedReader(reader);
            StringBuilder sb = new StringBuilder();
            String s = "";
            int data = in.read();
            while (data != -1) {
                char theChar = (char) data;
                sb.append(theChar);
                data = in.read();
            }
            input = sb.toString();
            if (StringUtils.isNotBlank(input)) {
                input = removeUTF8BOM(input);
                input = input.replaceAll("\\r\\n|\\r", "\n");
            }
            originalInput = input;
            in.close();
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public char charAt(int i) {
        return input.charAt(i);
    }

    public String getPipelessText() {
        int i = input.indexOf('\n');
        if (-1 == i)
            i = input.length();
        String str = input.substring(0, i);
        consume(str.length());
        return str.trim();
    }

    public String getInput() {
        return input;
    }

    public String getOriginalInput() {
        return originalInput;
    }

    public Matcher getMatcherForPattern(Pattern pattern) {
        return pattern.matcher(input);
    }

    public boolean isBlankLine() {
        return input != null && input.length() > 0 && '\n' == input.charAt(0);
    }

    public void setInput(String input) {
        this.input = input;
    }

    private String removeUTF8BOM(String s) {
        if (s.startsWith(UTF8_BOM)) {
            s = s.substring(1);
        }
        return s;
    }
}
