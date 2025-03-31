package de.neuland.pug4j.lexer;

import de.neuland.pug4j.exceptions.ExpressionException;
import de.neuland.pug4j.exceptions.PugLexerException;
import de.neuland.pug4j.expression.ExpressionHandler;
import de.neuland.pug4j.lexer.token.*;
import de.neuland.pug4j.parser.node.ExpressionString;
import de.neuland.pug4j.template.TemplateLoader;
import de.neuland.pug4j.parser.CharacterParserOptions;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;

import java.io.IOException;
import java.io.Reader;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Lexer {
    private static final Pattern cleanRe = Pattern.compile("^['\"]|['\"]$");
    private static final Pattern doubleQuotedRe = Pattern.compile("^\"[^\"]*\"$");
    private static final Pattern quotedRe = Pattern.compile("^'[^']*'$");
    private static final Pattern PATTERN_MIXIN_BLOCK = Pattern.compile("^block");
    private static final Pattern PATTERN_YIELD = Pattern.compile("^yield");
    private static final Pattern PATTERN_DOT = Pattern.compile("^\\.");
    private static final Pattern PATTERN_DEFAULT = Pattern.compile("^default");
    private static final Pattern PATTERN_CASE = Pattern.compile("^case +([^\\n]+)");
    private static final Pattern PATTERN_WHEN = Pattern.compile("^when +([^:\\n]+)");
    private static final Pattern PATTERN_PATH = Pattern.compile("^ ([^\\n]+)");
    private static final Pattern PATTERN_TEXT_1 = Pattern.compile("^(?:\\| ?| )([^\\n]+)");
    private static final Pattern PATTERN_TEXT_2 = Pattern.compile("^( )");
    private static final Pattern PATTERN_TEXT_3 = Pattern.compile("^\\|( ?)");
    private static final Pattern PATTERN_FILTER = Pattern.compile("^:([\\w\\-]+)");
    private static final Pattern PATTERN_COLON = Pattern.compile("^: +");
    private static final Pattern PATTERN_SLASH = Pattern.compile("^/");
    private static final Pattern PATTERN_TAG = Pattern.compile("^(\\w(?:[-:\\w]*\\w)?)");
    private static final Pattern PATTERN_INTERPOLATION = Pattern.compile("^#\\{");
    private static final Pattern PATTERN_BLANK = Pattern.compile("^\\n[ \\t]*\\n");
    private static final Pattern PATTERN_INCLUDE = Pattern.compile("^include(?=:| |$|\\n)");
    private static final Pattern PATTERN_CONDITIONAL = Pattern.compile("^(if|unless|else if|else)\\b([^\\n]*)");
    private static final Pattern PATTERN_EACH = Pattern.compile("^(?:each|for) +([a-zA-Z_$][\\w$]*)(?: *, *([a-zA-Z_$][\\w$]*))? * in *([^\\n]+)");
    private static final Pattern PATTERN_MALFORMED_EACH = Pattern.compile("^(?:each|for)\\b");
    private static final Pattern PATTERN_MALFORMED_EACH2 = Pattern.compile("^- *(?:each|for) +([a-zA-Z_$][\\w$]*)(?: *, *([a-zA-Z_$][\\w$]*))? +in +([^\\n]+)");
    private static final Pattern PATTERN_WHILE = Pattern.compile("^while +([^\\n]+)");
    private static final Pattern PATTERN_NO_WHILE_EXPRESSION = Pattern.compile("^while\\b");
    private static final Pattern PATTERN_CODE = Pattern.compile("^(!?=|-)[ \\t]*([^\\n]+)");
    private static final Pattern PATTERN_ATTRIBUTES_BLOCK = Pattern.compile("^&attributes\\b");
    private static final Pattern PATTERN_WHITESPACE = Pattern.compile("[ \\n\\t]");
    private static final Pattern PATTERN_QUOTE = Pattern.compile("['\"]");
    private static final Pattern PATTERN_ID = Pattern.compile("^#([\\w-]+)");
    private static final Pattern PATTERN_INVALID_ID = Pattern.compile("^#");
    private static final Pattern PATTERN_CLASS_NAME = Pattern.compile("^\\.([_a-z0-9\\-]*[_a-z][_a-z0-9\\-]*)", Pattern.CASE_INSENSITIVE);
    private static final Pattern PATTERN_EXTENDS = Pattern.compile("^extends?(?= |$|\\n)");
    private static final Pattern PATTERN_PREPEND = Pattern.compile("^(?:block +)?prepend +([^\\n]+)");
    private static final Pattern PATTERN_APPEND = Pattern.compile("^(?:block +)?append +([^\\n]+)");
    private static final Pattern PATTERN_BLOCK = Pattern.compile("^block +([^\\n]+)");
    private static final Pattern PATTERN_MIXIN = Pattern.compile("^mixin +([-\\w]+)(?: *\\((.*)\\))? *");
    private static final Pattern PATTERN_CALL = Pattern.compile("^\\+(\\s*)(([-\\w]+)|(#\\{))");
    private static final Pattern PATTERN_BLOCK_CODE = Pattern.compile("^-");
    private static final Pattern PATTERN_TABS = Pattern.compile("^\\n(\\t*) *");
    private static final Pattern PATTERN_SPACES = Pattern.compile("^\\n( *)");
    private static final Pattern PATTERN_COMMENT = Pattern.compile("^\\/\\/(-)?([^\\n]*)");
    private static final int INFINITY = Integer.MAX_VALUE;
    public static final Pattern PATTERN_NO_CASE_EXPRESSION = Pattern.compile("^case\\b");
    public static final Pattern PATTERN_MALFORMED_INCLUDE = Pattern.compile("^include\\b");
    public static final Pattern PATTERN_MALFORMED_EXTENDS = Pattern.compile("^extends?\\b");
    public static final Pattern PATTERN_STRING_INTERPOLATION = Pattern.compile("(\\\\)?([#!])\\{((?:.|\\n)*)$");
    public static final Pattern PATTERN_INVALID_CLASSNAME = Pattern.compile("^\\.[_a-z0-9\\-]+", Pattern.CASE_INSENSITIVE);
    public static final Pattern PATTERN_CLASSNAME_STARTS_WITH_DOT = Pattern.compile("^\\.");
    public static final Pattern PATTERN_EXTRACT_INVALID_CLASSNAME = Pattern.compile(".[^ \\t\\(\\#\\.\\:]*");
    public static final Pattern PATTERN_EXTRACT_INVALID_ID = Pattern.compile(".[^ \\t\\(\\#\\.\\:]*");
    public static final Pattern PATTERN_DOCTYPE = Pattern.compile("^doctype *([^\\n]*)");
    private final Scanner scanner;
    private int lineno;
    private int colno;
    private final LinkedList<Token> tokens;
    private final LinkedList<Integer> indentStack;
    private Pattern indentRe = null;
    private boolean pipeless = false;
    private boolean interpolationAllowed = true;
    private final String filename;
    private final TemplateLoader templateLoader;
    private final CharacterParser characterParser;
    private final ExpressionHandler expressionHandler;
    private boolean ended=false;
    private boolean interpolated=false;

    public Lexer(String filename, TemplateLoader templateLoader,ExpressionHandler expressionHandler) throws IOException {
        this.expressionHandler = expressionHandler;
        this.templateLoader = templateLoader;
        this.filename = filename;
        Reader reader = templateLoader.getReader(this.filename);
        scanner = new Scanner(reader);
        tokens = new LinkedList<>();
        indentStack = new LinkedList<>();
        indentStack.add(0);
        lineno = 1;
        colno = 1;
        characterParser = new CharacterParser();
    }

    public Lexer(String input,String filename, TemplateLoader templateLoader,ExpressionHandler expressionHandler,int lineno,int colno, boolean interpolated) throws IOException {
        this(input, filename,templateLoader,expressionHandler);
        this.lineno = lineno;
        this.colno = colno;
        this.interpolated = interpolated;
    }

    public Lexer(String input,String filename, TemplateLoader templateLoader,ExpressionHandler expressionHandler) throws IOException {
        this.expressionHandler = expressionHandler;
        this.templateLoader = templateLoader;
        this.filename = filename;
        scanner = new Scanner(input);
        tokens = new LinkedList<>();
        indentStack = new LinkedList<>();
        indentStack.add(0);
        lineno = 1;
        colno = 1;
        characterParser = new CharacterParser();
    }

    private PugLexerException error(String code, String message){
        return new PugLexerException("PUG:" + code,message,this.filename, this.lineno, this.colno,templateLoader);
    }

    public boolean next() {
        if (blank()) {
            return true;
        }
        if (eos()) {
            return true;
        }
        if (endInterpolation()) {
            return true;
        }
        if (yieldToken()) {
            return true;
        }
        if (doctype()) {
            return true;
        }
        if (interpolation()) {
            return true;
        }
        if (caseToken()) {
            return true;
        }
        if (when()) {
            return true;
        }
        if (defaultToken()) {
            return true;
        }
        if (extendsToken()) {
            return true;
        }
        if (append()) {
            return true;
        }
        if (prepend()) {
            return true;
        }
        if (block()) {
            return true;
        }
        if (mixinBlock()) {
            return true;
        }
        if (include()) {
            return true;
        }
        if (mixin()) {
            return true;
        }
        if (call()) {
            return true;
        }
        if (conditional()) {
            return true;
        }
        //Todo: eachOf
        if (each()) {
            return true;
        }
        if (whileToken()) {
            return true;
        }
        if (tag()) {
            return true;
        }
        if (filter()) {
            return true;
        }
        if (blockCode()) {
            return true;
        }
        if (code()) {
            return true;
        }
        if (id()) {
            return true;
        }
        if (dot()) {
            return true;
        }
        if (className()) {
            return true;
        }
        if (attrs()) {
            return true;
        }
        if (attributesBlock()) {
            return true;
        }
        if (indent()) {
            return true;
        }
        if (text()) {
            return true;
        }
        if (textHtml()) {
            return true;
        }
        if (comment()) {
            return true;
        }
        if (slash()) {
            return true;
        }
        if (colon()) {
            return true;
        }
        return fail();
    }
    
    public void consume(int len) {
        scanner.consume(len);
    }

    public void defer(Token tok) {
        tokens.push(tok);
    }

    public Token lookahead(int index) {
        boolean found = true;
        while (tokens.size() <= index && found) {
            found = next();
        }

        if(this.tokens.size() <= index){
            throw new PugLexerException("Cannot read past the end of a stream",this.filename,this.lineno,this.colno,templateLoader);
        }
        return this.tokens.get(index);
    }

    private CharacterParser.Match bracketExpression(){
        return bracketExpression(0);
    }

    private CharacterParser.Match bracketExpression(int skip){
        char start = scanner.getInput().charAt(skip);
        assertIf(start == '(' || start == '{' || start == '[',"The start character should be \"(\", \"{\" or \"[\"");
        Map<Character,Character> closingBrackets =  new HashMap<>();
        closingBrackets.put('(',')');
        closingBrackets.put('{','}');
        closingBrackets.put('[',']');
        char end = closingBrackets.get(start);
        CharacterParserOptions options = new CharacterParserOptions();
        options.setStart(skip+1);
        CharacterParser.Match range;
        try {
            range = characterParser.parseUntil(scanner.getInput(), String.valueOf(end), options);
        }catch(CharacterParserException exception){
            if(exception.getIndex()!=null){
               int index = exception.getIndex();
               int tmp = scanner.getInput().substring(skip).indexOf("\n");
               int nextNewline = tmp + skip;
               int ptr = 0;
               while(index > nextNewline && tmp != -1){
                   this.incrementLine(1);
                   index += nextNewline + 1;
                   ptr += nextNewline + 1;
                   tmp = nextNewline = scanner.getInput().substring(ptr).indexOf("\n");
               }
               this.incrementColumn(index);
            }
            if("CHARACTER_PARSER:END_OF_STRING_REACHED".equals(exception.getCode())){
                throw error("NO_END_BRACKET","The end of the string reached with no closing bracket " + end + " found.");
            }else if("CHARACTER_PARSER:MISMATCHED_BRACKET".equals(exception.getCode())){
                throw error("BRACKET_MISMATCH",exception.getMessage());
            }
            throw exception;
        }
        return range;
    }

    private void assertIf(boolean assertion,String message) {
        if(!assertion) {
            throw new PugLexerException(message, filename, getLineno(),this.colno, templateLoader);
        }
    }

    public int getLineno() {
        return lineno;
    }

    public int getColno() {
        return colno;
    }

    public void setPipeless(boolean pipeless) {
        this.pipeless = pipeless;
    }

    public Token advance() {
        boolean found = true;
        while (tokens.size() <= 0 && found && !ended) {
            found = next();
        }

        return this.tokens.pollFirst();
    }

    private Token tokEnd(Token token){
        token.setEndLineNumber(this.lineno);
        token.setEndColumn(this.colno);
        return token;
    }

    private void incrementLine(int increment) {
        lineno+=increment;
        if(increment>0)
            colno = 1;
    }

    private void incrementColumn(int increment){
        this.colno += increment;
    }

    private Token scan(Pattern pattern,Token token) {
        Matcher matcher = scanner.getMatcherForPattern(pattern);
        if (matcher.find(0)) {
            int end = matcher.end();
            String val = null;
            if(matcher.groupCount()>0)
                val = matcher.group(1);
            int diff = end - (val!=null ? val.length() : 0);
            token = tok(token);
            token.setValue(val);
            consume(end);
            incrementColumn(diff);
            return token;
        }
        return null;
    }
    private boolean scan(Pattern pattern) {
        Matcher matcher = scanner.getMatcherForPattern(pattern);
        return matcher.find(0);
    }

    private Token scanEndOfLine(Pattern pattern, Token token) {
        Matcher matcher = scanner.getMatcherForPattern(pattern);
        if (matcher.find(0)) {
            int whitespaceLength = 0;
            Pattern pattern1 = Pattern.compile("^([ ]+)([^ ]*)");
            Matcher whitespace = pattern1.matcher(matcher.group(0));
            if(whitespace.find(0)){
                whitespaceLength = whitespace.group(1).length();
                incrementColumn(whitespaceLength);
            }

            String newInput = scanner.getInput().substring(matcher.group(0).length());
            if(!newInput.isEmpty() && newInput.charAt(0) == ':'){
                scanner.consume(matcher.group(0).length());
                token = tok(token);
                if(matcher.groupCount()>0) {
                    token.setValue(matcher.group(1));
                }
                incrementColumn(matcher.group(0).length() - whitespaceLength);
                return token;
            }

            Pattern pattern2 = Pattern.compile("^[ \\t]*(\\n|$)");
            Matcher matcher1 = pattern2.matcher(newInput);
            if(matcher1.find(0)){
                Pattern pattern3 = Pattern.compile("^[ \\t]*");
                int length = matcher.group(0).length();
                Matcher matcher2 = pattern3.matcher(newInput);
                if(matcher2.find(0)) {
                    length = length + matcher2.end();
                }
                scanner.consume(length);
                token = tok(token);
                if(matcher.groupCount()>0) {
                    token.setValue(matcher.group(1));
                }
                incrementColumn(matcher.group(0).length() - whitespaceLength);
                return token;
            }
        }
        return null;
    }

    private boolean blank(){
        Matcher matcher = scanner.getMatcherForPattern(PATTERN_BLANK);
        if (matcher.find(0)) {
            consume(matcher.end()-1);
            incrementLine(1);
            return true;
        }
        return false;
    }

    private boolean eos() {
        if (!scanner.getInput().isEmpty()) {
            return false;
        }
        if(this.interpolated){
            throw error("NO_END_BRACKET","End of line was reached with no closing bracket for interpolation.");
        }
        for (int i = 0;!indentStack.get(i).equals(0);i++) {
            pushToken(tokEnd(tok(new Outdent())));
        }
        pushToken(tokEnd(tok(new Eos(null, lineno))));
        this.ended = true;
        return true;
    }

    private boolean comment() {
        Matcher matcher = scanner.getMatcherForPattern(PATTERN_COMMENT);
        if (matcher.find(0) && matcher.groupCount() > 1) {
            consume(matcher.end());
            boolean buffer = !"-".equals(matcher.group(1));
            this.interpolationAllowed = buffer;
            Token comment = tok(new Comment(matcher.group(2), lineno, buffer));
            incrementColumn(matcher.end());
            pushToken(tokEnd(comment));
            pipelessText();
            return true;
        }
        return false;
    }

    private boolean code() {
        Matcher matcher = scanner.getMatcherForPattern(PATTERN_CODE);
        if (matcher.find(0) && matcher.groupCount() > 1) {
            String flags = matcher.group(1);
            String code = matcher.group(2);
            int shortend = 0;
            if(this.interpolated){
                CharacterParser.Match parsed = characterParser.parseUntil(code, "]");
                shortend = code.length() - parsed.getEnd();
                code = parsed.getSrc();
            }
            int consumed = matcher.end() - shortend;
            consume(consumed);
            Expression expression = (Expression) tok(new Expression(code, lineno));
            expression.setEscape(flags.charAt(0) == '=');
            expression.setBuffer(flags.charAt(0) == '=' || flags.length()>1 && flags.charAt(1) == '=');
            incrementColumn(matcher.end()-matcher.group(2).length());
            if(expression.isBuffer()) {
                assertExpression(code);
            }
            incrementColumn(code.length());
            pushToken(tokEnd(expression));
            return true;
        }
        return false;
    }

    private boolean interpolation(){
        Matcher matcher = scanner.getMatcherForPattern(PATTERN_INTERPOLATION);
        if (matcher.find(0)) {
            try {
                CharacterParser.Match match = this.bracketExpression(1);
                this.scanner.consume(match.getEnd()+1);
                Token tok = tok(new Interpolation(match.getSrc(), lineno));
                incrementColumn(2); // '#{'
                assertExpression(match.getSrc());
                String[] splitted = StringUtils.split(match.getSrc(), '\n');
                int lines = splitted.length-1;
                incrementLine(lines);
                incrementColumn(splitted[lines].length()+1); // + 1 → '}'
                pushToken(tokEnd(tok));
                return true;
            } catch(Exception ex){
                return false; //not an interpolation expression, just an unmatched open interpolation
            }
        }
        return false;
    }

    private boolean tag() {
        Matcher matcher = scanner.getMatcherForPattern(PATTERN_TAG);
        if(matcher.find(0) && matcher.groupCount() > 0){
            String name = matcher.group(1);
            int length = matcher.group(0).length();
            consume(length);
            Token token = tok(new Tag(name));
            incrementColumn(length);
            pushToken(tokEnd(token));
            return true;
        }
        return false;
    }

    private boolean filter(){
        return filter(false);
    }

    private boolean filter(boolean inInclude) {
        Token token = scan(PATTERN_FILTER,new Filter());
        if (token!=null) {
            incrementColumn(token.getValue().length());
            pushToken(tokEnd(token));
            attrs();
            if(!inInclude){
                this.interpolationAllowed = false;
                pipelessText();
            }
            return true;
        }
        return false;
    }

    private boolean each() {
        Matcher matcher = scanner.getMatcherForPattern(PATTERN_EACH);
        if (matcher.find(0) && matcher.groupCount() > 2) {
            consume(matcher.end());
            String value = matcher.group(1);
            Each each = (Each) tok(new Each(value, lineno));
            String key = matcher.group(2);
            each.setKey(key);
            String code = matcher.group(3);
            this.incrementColumn(matcher.end() - code.length());
            assertExpression(code);
            each.setCode(code);
            this.incrementColumn(code.length());
            pushToken(tokEnd(each));

            return true;
        }

        if(scan(PATTERN_MALFORMED_EACH)){
            throw error("MALFORMED_EACH", "malformed each");
        }
        if(scan(PATTERN_MALFORMED_EACH2)){
            throw error("MALFORMED_EACH", "Pug each and for should no longer be prefixed with a dash (\"-\"). They are pug keywords and not part of JavaScript.");
        }
        return false;
    }

    private boolean whileToken() {
        Matcher matcher = scanner.getMatcherForPattern(PATTERN_WHILE);
        if (matcher.find(0) && matcher.groupCount()>0) {
            consume(matcher.end());
            assertExpression(matcher.group(1));
            Token token = tok(new While(matcher.group(1)));
            incrementColumn(matcher.end());
            pushToken(tokEnd(token));
            return true;
        }
        if (this.scan(PATTERN_NO_WHILE_EXPRESSION)) {
            throw error("NO_WHILE_EXPRESSION", "missing expression for while");
        }
        return false;
    }

    private boolean conditional() {
        Matcher matcher = scanner.getMatcherForPattern(PATTERN_CONDITIONAL);
        if (matcher.find(0) && matcher.groupCount() > 1) {
            consume(matcher.end());
            String type = matcher.group(1).replace(' ','-');
            String js = matcher.group(2);
            if(js!=null)
                js = js.trim();

            Token token = null;

            switch (type){
                case "if":
                    assertExpression(js);
                    If ifToken = new If(js, lineno);
                    token = tok(ifToken);
                    break;
                case "else-if":
                    assertExpression(js);
                    token = tok(new ElseIf(js, lineno));
                    break;
                case "unless":
                    assertExpression(js);
                    If unlessToken = new If("!("+js+")", lineno);
                    token = tok(unlessToken);
                    break;
                case "else":
                    if(js!=null && !js.isEmpty()){
                        throw error("ELSE_CONDITION","`else` cannot have a condition, perhaps you meant `else if`");
                    }
                    token = tok(new Else(null, lineno));
                    break;
            }
            assert js != null;
            this.incrementColumn(matcher.end()-js.length());
            this.incrementColumn(js.length());
            if(token!=null) {
                pushToken(tokEnd(token));
            }else{
                throw error("WRONG_CONDITION","type "+type+" no allowed here");
            }
            return true;
        }
        return false;
    }

    private boolean doctype(){
        Token token = scanEndOfLine(PATTERN_DOCTYPE,new Doctype());
        if(token!=null){
            pushToken(tokEnd(token));
            return true;
        }
        return false;
    }

    private boolean id() {
        Token token = scan(PATTERN_ID, new CssId());
        if (token != null) {
            incrementColumn(token.getValue().length());
            pushToken(tokEnd(token));
            return true;
        }
        if (scan(PATTERN_INVALID_ID)){
            Matcher matcher = PATTERN_EXTRACT_INVALID_ID.matcher(scanner.getInput());
            if(matcher.find()) {
                throw error("INVALID_ID", "\"" + matcher.group(0) + "\" is not a valid ID.");
            }
        }
        return false;
    }

    private boolean className() {
        Token token = scan(PATTERN_CLASS_NAME,new CssClass());
        if (token!=null) {
            incrementColumn(token.getValue().length());
            pushToken(tokEnd(token));
            return true;
        }
        if(PATTERN_INVALID_CLASSNAME.matcher(scanner.getInput()).find(0)){
            throw error("INVALID_CLASS_NAME","Class names must contain at least one letter or underscore.");
        }
        if(PATTERN_CLASSNAME_STARTS_WITH_DOT.matcher(scanner.getInput()).find(0)){
            Matcher matcher = PATTERN_EXTRACT_INVALID_CLASSNAME.matcher(scanner.getInput().substring(1));
            if(matcher.find(0))
                throw error("INVALID_CLASS_NAME","\"" + matcher.group(0) + "\" is not a valid class name.  Class names can only contain \"_\", \"-\", a-z and 0-9, and must contain at least one of \"_\", or a-z");
        }
        return false;
    }

    private boolean endInterpolation(){
        if(interpolated && this.scanner.getInput().charAt(0) == ']'){
            this.consume(1);
            this.ended=true;
            return true;
        }
        return false;
    }

    private void addText(Token token, String value){
        addText(token,value,null);
    }

    private void addText(Token token, String value, String prefix) {
        addText(token,value,prefix,0);
    }

    private void addText(Token token, String value, String prefix,int escaped) {
        if (prefix != null && (value + prefix).isEmpty())
            return;
        int indexOfEnd = this.interpolated ? value.indexOf(']') : -1;
        int indexOfStart = this.interpolationAllowed ? value.indexOf("#[") : -1;
        int indexOfEscaped = this.interpolationAllowed ? value.indexOf("\\#[") : -1;
        Matcher matchOfStringInterp = PATTERN_STRING_INTERPOLATION.matcher(value);
        int indexOfStringInterp = this.interpolationAllowed && matchOfStringInterp.find(0) ? matchOfStringInterp.start() : INFINITY;

        if (indexOfEnd == -1) indexOfEnd = INFINITY;
        if (indexOfStart == -1) indexOfStart = INFINITY;
        if (indexOfEscaped == -1) indexOfEscaped = INFINITY;

        if (indexOfEscaped < indexOfEnd && indexOfEscaped < indexOfStart && indexOfEscaped < indexOfStringInterp) {
            if(prefix!=null) {
                prefix = prefix + value.substring(0, indexOfEscaped) + "#[";
            }else {
                prefix = value.substring(0, indexOfEscaped) + "#[";
            }
            this.addText(token, StringUtils.substring(value, indexOfEscaped + 3), prefix, escaped + 1);
            return;
        }

        if (indexOfStart < indexOfEnd && indexOfStart < indexOfEscaped && indexOfStart < indexOfStringInterp) {
            Token newToken = tok(token);
            if(prefix == null) {
                newToken.setValue(StringUtils.substring(value, 0, indexOfStart));
                incrementColumn(indexOfStart + escaped);
            }else {
                newToken.setValue(prefix + StringUtils.substring(value, 0, indexOfStart));
                incrementColumn(prefix.length() + indexOfStart + escaped);
            }
            pushToken(tokEnd(newToken));
            StartPugInterpolation startPugInterpolation = (StartPugInterpolation) this.tok(new StartPugInterpolation());
            this.incrementColumn(2);
            pushToken(this.tokEnd(startPugInterpolation));
            Lexer child = null;
            try {
                child = new Lexer(value.substring(indexOfStart + 2),this.filename, templateLoader, expressionHandler,this.lineno,this.colno,true);
            } catch (IOException e) {
                throw new PugLexerException(e.getMessage(),this.filename,this.lineno,this.colno,templateLoader);
            }
            LinkedList<Token> interpolated = child.getTokens();  //TODO: try catch

            this.colno = child.getColno();
            this.tokens.addAll(interpolated);
            Token endInterpolationToken = tok(new EndPugInterpolation());
            this.incrementColumn(1);
            pushToken(this.tokEnd(endInterpolationToken));
            this.addText(token, child.getInput());
            return;
        }

        if (indexOfEnd < indexOfStart && indexOfEnd < indexOfEscaped && indexOfEnd < indexOfStringInterp) {
            if(prefix == null){
                if (!(StringUtils.substring(value, 0, indexOfEnd)).isEmpty()) {
                    this.addText(token, value.substring(0, indexOfEnd), prefix);
                }
            }else {
                if (!(prefix + StringUtils.substring(value, 0, indexOfEnd)).isEmpty()) {
                    this.addText(token, value.substring(0, indexOfEnd), prefix);
                }
            }
            this.ended = true;
            scanner.setInput(value.substring(value.indexOf(']') + 1) + scanner.getInput());
            return;
        }

        if (indexOfStringInterp != INFINITY) {
            if (matchOfStringInterp.group(1)!=null) {
                if(prefix==null) {
                    prefix = StringUtils.substring(value, 0, indexOfStringInterp) + "#{";
                }else{
                    prefix = prefix + StringUtils.substring(value, 0, indexOfStringInterp) + "#{";
                }
                this.addText(token, value.substring(indexOfStringInterp + 3), prefix, escaped + 1);
                return;
            }

            String before = StringUtils.substring(value, 0, indexOfStringInterp);
            if (prefix != null)
                before = prefix + before;
            Token tok = this.tok(token);
            tok.setValue(before);
            this.incrementColumn(before.length() + escaped);
            pushToken(this.tokEnd(tok));

            String rest = matchOfStringInterp.group(3);
            InterpolatedCode interpolatedCodeToken = (InterpolatedCode) this.tok(new InterpolatedCode());
            this.incrementColumn(2);
            CharacterParser.Match range;
            try {
                range = characterParser.parseUntil(rest, "}");
            }catch (CharacterParserException exception){
                if(exception.getIndex()!=null){
                    this.incrementColumn(exception.getIndex());
                }
                if ("CHARACTER_PARSER:END_OF_STRING_REACHED".equals(exception.getCode())) {
                    throw this.error("NO_END_BRACKET", "End of line was reached with no closing bracket for interpolation.");
                } else if ("CHARACTER_PARSER:MISMATCHED_BRACKET".equals(exception.getCode())) {
                    throw this.error("BRACKET_MISMATCH", exception.getMessage());
                } else {
                    throw exception;
                }
            }

            interpolatedCodeToken.setMustEscape("#".equals(matchOfStringInterp.group(2)));
            interpolatedCodeToken.setBuffer(true);
            interpolatedCodeToken.setValue(range.getSrc());
            assertExpression(range.getSrc());

            if (range.getEnd() + 1 < rest.length()) {
                rest = rest.substring(range.getEnd() + 1);
                this.incrementColumn(range.getEnd() + 1);
                pushToken(this.tokEnd(interpolatedCodeToken));
                this.addText(token, rest);
            } else {
                this.incrementColumn(rest.length());
                pushToken(this.tokEnd(interpolatedCodeToken));
            }
            return;

        }
        if(prefix!=null)
            value = prefix + value;
        Token tok = this.tok(token);
        tok.setValue(value);
        this.incrementColumn(value.length() + escaped);
        pushToken(this.tokEnd(tok));
    }

    private boolean text() {
        Text textToken = new Text();
        Token token = scan(PATTERN_TEXT_1, textToken);
        if (token==null) {
            token = scan(PATTERN_TEXT_2,textToken);
        }
        if (token==null) {
            token = scan(PATTERN_TEXT_3,textToken);
        }
        if (token!=null) {
            addText(new Text(),token.getValue());
            return true;
        }
        return false;
    }

    private boolean textHtml() {
        Token token = scan(Pattern.compile("^(<[^\\n]*)"), new TextHtml());
        if (token!=null) {
            addText(new TextHtml(),token.getValue());
            return true;
        }
        return false;
    }

    private boolean dot() {
        Token token = scanEndOfLine(PATTERN_DOT, new Dot());
        if (token!=null) {
            pushToken(tokEnd(token));
            pipelessText();
            return true;
        }
        return false;
    }

    private boolean extendsToken() {
        Token token = scan(PATTERN_EXTENDS, new ExtendsToken());
        if (token != null) {
            pushToken(tokEnd(token));
            if(!path()){
                throw error("NO_EXTENDS_PATH","missing path for extends");
            }
            return true;
        }

        if (this.scan(PATTERN_MALFORMED_EXTENDS)) {
            throw error("MALFORMED_EXTENDS", "malformed extends");
        }
        return false;
    }

    private boolean prepend() {
        Matcher matcher = scanner.getMatcherForPattern(PATTERN_PREPEND);
        if (matcher.find(0)) {
            String name = matcher.group(1).trim();
            String comment = "";

            if(name.contains("//")){
                String[] split = StringUtils.split(name, "//");
                comment = "//" + StringUtils.join(Arrays.copyOfRange(split,1,split.length),"//");
                name = StringUtils.split(name,"//")[0].trim();
            }

            if(StringUtils.isNotBlank(name)) {
                Token token = tok(new Block(name));
                int len = matcher.group(0).length() - comment.length();
                while(PATTERN_WHITESPACE.matcher(String.valueOf(scanner.getInput().charAt(len-1))).find(0)) {
                    len--;
                }
                incrementColumn(len);
                token.setMode("prepend");
                pushToken(tokEnd(token));
                consume(matcher.end() - comment.length());
                incrementColumn(matcher.end() - comment.length() - len);
                return true;
            }
        }
        return false;
    }

    private boolean append() {
        Matcher matcher = scanner.getMatcherForPattern(PATTERN_APPEND);
        if (matcher.find(0)) {
            String name = matcher.group(1).trim();
            String comment = "";

            if(name.contains("//")){
                String[] split = StringUtils.split(name, "//");
                comment = "//" + StringUtils.join(Arrays.copyOfRange(split,1,split.length),"//");
                name = StringUtils.split(name,"//")[0].trim();
            }

            if(StringUtils.isNotBlank(name)) {
                Token token = tok(new Block(name));
                int len = matcher.group(0).length() - comment.length();
                while(PATTERN_WHITESPACE.matcher(String.valueOf(scanner.getInput().charAt(len-1))).find(0)) {
                    len--;
                }
                incrementColumn(len);
                token.setMode("append");
                pushToken(tokEnd(token));
                consume(matcher.end() - comment.length());
                incrementColumn(matcher.end() - comment.length() - len);
                return true;
            }
        }
        return false;
    }

    private boolean block() {
        Matcher matcher = scanner.getMatcherForPattern(PATTERN_BLOCK);
        if (matcher.find(0)) {
            String name = matcher.group(1).trim();
            String comment = "";

            if(name.contains("//")){
                String[] split = StringUtils.split(name, "//");
                comment = "//" + StringUtils.join(Arrays.copyOfRange(split,1,split.length),"//");
                name = StringUtils.split(name,"//")[0].trim();
            }

            if(StringUtils.isNotBlank(name)) {
                Token token = tok(new Block(name));
                int len = matcher.group(0).length() - comment.length();
                while(PATTERN_WHITESPACE.matcher(String.valueOf(scanner.getInput().charAt(len-1))).find(0)) {
                    len--;
                }
                incrementColumn(len);
                token.setMode("replace");
                pushToken(tokEnd(token));
                consume(matcher.end() - comment.length());
                incrementColumn(matcher.end() - comment.length() - len);
                return true;
            }
        }
        return false;
    }

    private boolean mixinBlock() {
        Token token = scanEndOfLine(PATTERN_MIXIN_BLOCK, new MixinBlock());
        if (token!=null) {
            pushToken(tokEnd(token));
            return true;
        }
        return false;
    }

    private boolean yieldToken() {
        Token token = scanEndOfLine(PATTERN_YIELD, new Yield());
        if (token!=null) {
            pushToken(tokEnd(token));
            return true;
        }
        return false;
    }

    private boolean include() {
        Token token = scan(PATTERN_INCLUDE,new Include());
        if (token!=null) {
            pushToken(tokEnd(token));
            while (filter(true));
            if(!path()){
                if(Pattern.compile("^[^ \\n]+").matcher(scanner.getInput()).find(0)){
                    fail();
                } else {
                    throw error("NO_INCLUDE_PATH","missing path for include");
                }
            }
            return true;
        }

        if (this.scan(PATTERN_MALFORMED_INCLUDE)) {
             throw error("MALFORMED_INCLUDE", "malformed include");
        }
        return false;
    }

    private boolean path(){
        Token token = scanEndOfLine(PATTERN_PATH,new Path());
        if (token != null) {
            token.setValue(token.getValue().trim());
            pushToken(tokEnd(token));
            return true;
        }
        return false;
    }

    private boolean caseToken() {
        Token token = scanEndOfLine(PATTERN_CASE,new CaseToken());
        if (token!=null) {
            incrementColumn(-token.getValue().length());
            assertExpression(token.getValue());
            incrementColumn(token.getValue().length());
            pushToken(tokEnd(token));
            return true;
        }
        if (this.scan(PATTERN_NO_CASE_EXPRESSION)) {
            throw error("NO_CASE_EXPRESSION", "missing expression for case");
        }

        return false;
    }

    private boolean when() {
        Token token = scanEndOfLine(PATTERN_WHEN,new When());
        if (token!=null) {
            String val = token.getValue();
            CharacterParser.State parse = characterParser.parse(val);
            while(parse.isNesting() || parse.isString()){
                Matcher matcher = scanner.getMatcherForPattern(Pattern.compile(":([^:\\n]+)"));
                if(!matcher.find(0))
                    break;

                val += matcher.group(0);
                int increment = matcher.group(0).length();
                consume(increment);
                incrementColumn(increment);
                parse = characterParser.parse(val);
            }

            incrementColumn(-val.length());
            assertExpression(val);
            incrementColumn(val.length());
            token.setValue(val);
            pushToken(tokEnd(token));
            return true;
        }
        if (this.scan(Pattern.compile("^when\\b"))) {
            throw error("NO_WHEN_EXPRESSION", "missing expression for when");
        }
        return false;
    }

    private boolean defaultToken() {
        Token token = scanEndOfLine(PATTERN_DEFAULT,new Default());
        if (token!=null) {
            pushToken(tokEnd(token));
            return true;
        }
        if (this.scan(Pattern.compile("^default\\b"))) {
            throw error("DEFAULT_WITH_EXPRESSION", "default should not have an expression");
        }
        return false;
    }

    private boolean mixin() {
        Matcher matcher = scanner.getMatcherForPattern(PATTERN_MIXIN);
        if (matcher.find(0) && matcher.groupCount() > 1) {
            consume(matcher.end());
            Mixin tok = (Mixin) tok(new Mixin(matcher.group(1), lineno));
            tok.setArguments(matcher.group(2));
            incrementColumn(matcher.group(0).length());
            pushToken(tokEnd(tok));
            return true;
        }
        return false;
    }

    private boolean call() {
        Call tok;
        int increment;
        Matcher matcher = scanner.getMatcherForPattern(PATTERN_CALL);
        if (matcher.find(0) && matcher.groupCount() > 3) {
            // try to consume simple or interpolated call
            if(matcher.group(3)!=null) {
                // simple call
                increment = matcher.end();
                consume(increment);
                tok = (Call) tok(new Call(matcher.group(3), lineno));
            }else{
                // interpolated call
                CharacterParser.Match match = this.bracketExpression(2 + matcher.group(1).length());
                increment = match.getEnd() + 1;
                this.consume(increment);
                assertExpression(match.getSrc());
                tok = (Call) tok(new Call("#{"+match.getSrc()+"}", lineno));
            }

            incrementColumn(increment);

            matcher = scanner.getMatcherForPattern(Pattern.compile("^ *\\("));
            if (matcher.find(0)) {
                CharacterParser.Match range = this.bracketExpression(matcher.group(0).length() - 1);
                matcher = Pattern.compile("^\\s*[-\\w]+ *=").matcher(range.getSrc());

                if (!matcher.find(0)) { // not attributes
                    incrementColumn(1);
                    this.consume(range.getEnd() + 1);
                    tok.setArguments(range.getSrc());
                }

                if (tok.getArguments()!=null) {
                    assertExpression("[" + tok.getArguments() + "]");
                    for (int i = 0; i< tok.getArguments().length();i++) {
                        if(tok.getArguments().charAt(i) == '\n'){
                            incrementLine(1);
                        }else{
                            incrementColumn(1);
                        }
                    }
                }
            }
            pushToken(tokEnd(tok));
            return true;
        }
        return false;
    }

    private void assertNestingCorrect(String exp) {
        //this verifies that code is properly nested, but allows
        //invalid JavaScript such as the contents of `attributes`
        CharacterParser.State res = characterParser.parse(exp);
        if (res.isNesting()) {
            throw error("INCORRECT_NESTING","Nesting must match on expression `" + exp + "`");
        }
    }

    private boolean attrs() {
        if (scanner.getInput().length()>1 && '(' == scanner.getInput().charAt(0)) {
            Token startAttributesToken = tok(new StartAttributes());
            int index = this.bracketExpression().getEnd();
            String str = scanner.getInput().substring(1, index);

            incrementColumn(1);
            pushToken(tokEnd(startAttributesToken));
            assertNestingCorrect(str);
            scanner.consume(index + 1);

            while (!str.isEmpty()) {
                str = attribute(str);
            }

            Token endAttributesToken = tok(new EndAttributes());
            incrementColumn(1);
            pushToken(tokEnd(endAttributesToken));
            return true;
        }
        return false;
    }

    private boolean blockCode() {
        Token token = scanEndOfLine(PATTERN_BLOCK_CODE,new BlockCode());
        if(token != null){
            pushToken(tokEnd(token));
            this.interpolationAllowed=false;
            pipelessText();
            return true;
        }
        return false;
    }

    private String attribute(String str) {
        Character quote = null;
        StringBuilder key = new StringBuilder();
        int i;

        // consume all whitespace before the key
        for(i = 0; i < str.length(); i++){

            if(!PATTERN_WHITESPACE.matcher(String.valueOf(str.charAt(i))).find(0)) break;
            if(str.charAt(i) == '\n'){
                this.incrementLine(1);
            } else {
                this.incrementColumn(1);
            }
        }

        if(i == str.length()){
            return "";
        }

        Attribute tok = (Attribute) this.tok(new Attribute());

        // quote?
        if(PATTERN_QUOTE.matcher(String.valueOf(str.charAt(i))).find(0)){
            quote = str.charAt(i);
            this.incrementColumn(1);
            i++;
        }

        // start looping through the key
        for (; i < str.length(); i++) {

            if(quote != null){
                if (str.charAt(i) == quote) {
                    this.incrementColumn(1);
                    i++;
                    break;
                }
            } else {
                if(PATTERN_WHITESPACE.matcher(String.valueOf(str.charAt(i))).find(0) || str.charAt(i) == '!' || str.charAt(i) == '=' || str.charAt(i) == ',') {
                    break;
                }
            }

            key.append(str.charAt(i));

            if (str.charAt(i) == '\n') {
                this.incrementLine(1);
            } else {
                this.incrementColumn(1);
            }
        }

        tok.setName(key.toString());

        AttributeValueResponse valueResponse = this.attributeValue(str.substring(i));

        if (valueResponse.getValue()!=null) {
            if ("".equals(valueResponse.getValue())) {
                tok.setAttributeValue(true);
                tok.setMustEscape(false);
            } else if (doubleQuotedRe.matcher(valueResponse.getValue()).matches()
                    || quotedRe.matcher(valueResponse.getValue()).matches()) {
                //toConstant
                String val = valueResponse.getValue();
                val = val.trim();
                val = val.replaceAll("\\n","");
                val = StringEscapeUtils.unescapeEcmaScript(val);
                String cleanValue = cleanRe.matcher(val).replaceAll("");

                tok.setAttributeValue(cleanValue);
                tok.setMustEscape(valueResponse.isMustEscape());
            } else {
                ExpressionString expressionString = new ExpressionString(valueResponse.getValue());
                assertExpression(valueResponse.getValue());
                tok.setAttributeValue(expressionString);
                tok.setMustEscape(valueResponse.isMustEscape());
            }
        } else {
            // was a boolean attribute (ex: `input(disabled)`)
            tok.setAttributeValue(true);
            tok.setMustEscape(true);
        }

        str = valueResponse.getRemainingSource();

        pushToken(this.tokEnd(tok));

        for(i = 0; i < str.length(); i++){
            if(!PATTERN_WHITESPACE.matcher(String.valueOf(str.charAt(i))).find(0)) {
                break;
            }
            if(str.charAt(i) == '\n'){
                this.incrementLine(1);
            } else {
                this.incrementColumn(1);
            }
        }

        if(str.length()>i && str.charAt(i) == ','){
            this.incrementColumn(1);
            i++;
        }

        return str.substring(i);

    }

    private AttributeValueResponse attributeValue(String str){
        Pattern quoteRe = PATTERN_QUOTE;
        String val = "";
        int i;
        int x;
        boolean done;
        boolean escapeAttr = true;
        CharacterParser.State state = characterParser.defaultState();
        int col = this.colno;
        int line = this.lineno;

        // consume all whitespace before the equals sign
        for(i = 0; i < str.length(); i++){
            if(!PATTERN_WHITESPACE.matcher(String.valueOf(str.charAt(i))).find(0)) break;
            if(str.charAt(i) == '\n'){
                line++;
                col = 1;
            } else {
                col++;
            }
        }

        if(i == str.length()){
            return new AttributeValueResponse(null,false,str);
        }

        if(str.charAt(i) == '!'){
            escapeAttr = false;
            col++;
            i++;
            if (str.charAt(i) != '=')
                throw error("INVALID_KEY_CHARACTER","Unexpected character " + str.charAt(i) + " expected `=`");
        }

        if(str.charAt(i) != '='){
            // check for anti-pattern `div("foo"bar)`
            if (i == 0 && !PATTERN_WHITESPACE.matcher(String.valueOf(str.charAt(0))).find(0) && str.charAt(0) != ','){
                throw error("INVALID_KEY_CHARACTER","Unexpected character " + str.charAt(i) + " expected `=`");
            } else {
                return new AttributeValueResponse(null,false,str);
            }
        }

        this.lineno = line;
        this.colno = col + 1;
        i++;

        // consume all whitespace before the value
        for(; i < str.length(); i++){
            if(!PATTERN_WHITESPACE.matcher(String.valueOf(str.charAt(i))).find(0)) break;
            if(str.charAt(i) == '\n'){
                this.incrementLine(1);
            } else {
                this.incrementColumn(1);
            }
        }

        line = this.lineno;
        col = this.colno;

        // start looping through the value
        for (; i < str.length(); i++) {
            // if the character is in a string or in parentheses/brackets/braces
            if (!(state.isNesting() || state.isString())){

                if (PATTERN_WHITESPACE.matcher(String.valueOf(str.charAt(i))).find(0)) {
                    done = false;

                    // find the first non-whitespace character
                    for (x = i; x < str.length(); x++) {
                        if (!PATTERN_WHITESPACE.matcher(String.valueOf(str.charAt(x))).find(0)) {
                            // if it is a JavaScript punctuator, then assume that it is
                            // a part of the value
                            boolean isNotPunctuator = !characterParser.isPunctuator(str.charAt(x));
                            boolean isQuote = PATTERN_QUOTE.matcher(String.valueOf(str.charAt(x))).find(0);
                            boolean isColon = str.charAt(x) == ':';
                            boolean isSpreadOperator = str.length()>x+2 && "...".equals(str.substring(x,x+3));
                            if ((isNotPunctuator || isQuote || isColon || isSpreadOperator) && this.assertExpression(val, true)) {
                                done = true;
                            }
                            break;
                        }
                    }

                    // if everything else is whitespace, return now so last attribute
                    // does not include trailing whitespace
                    if(done || x == str.length()){
                        break;
                    }
                }

                // if there's no whitespace and the character is not ',', the
                // attribute did not end.
                if(str.charAt(i) == ',' && this.assertExpression(val, true)){
                    break;
                }
            }

            state = characterParser.parseChar(str.charAt(i), state);
            val += str.charAt(i);

            if (str.charAt(i) == '\n') {
                line++;
                col = 1;
            } else {
                col++;
            }
        }

        this.lineno = line;
        this.colno = col;

        if (val.isEmpty()) {
            return new AttributeValueResponse("",false,str.substring(i));
        } else {
            return new AttributeValueResponse(val, escapeAttr, str.substring(i));
        }
    }

    private boolean attributesBlock() {
        Matcher matcher = scanner.getMatcherForPattern(PATTERN_ATTRIBUTES_BLOCK);
        if (matcher.find(0) && matcher.group(0) != null) {
            int consumed = 11;
            this.scanner.consume(consumed);
            Token attributesBlock = tok(new AttributesBlock());
            incrementColumn(consumed);
            CharacterParser.Match match = this.bracketExpression();
            consumed = match.getEnd() + 1;
            this.scanner.consume(consumed);
            attributesBlock.setValue(match.getSrc());
            incrementColumn(consumed);
            pushToken(tokEnd(attributesBlock));
            return true;
        }
        return false;
    }

    private Matcher scanIndentation(){
        Matcher matcher;
        Pattern re;

        if (indentRe != null) {
            matcher = scanner.getMatcherForPattern(indentRe);
        } else {
            // tabs
            re = PATTERN_TABS;
            matcher = scanner.getMatcherForPattern(re);

            // spaces
            if (matcher.find(0) && matcher.group(1).isEmpty()) {
                re = PATTERN_SPACES;
                matcher = scanner.getMatcherForPattern(re);
            }

            // established
            if (matcher.find(0) && !matcher.group(1).isEmpty())
                this.indentRe = re;
        }
        return matcher;
    }

    private boolean indent() {
        Matcher matcher = scanIndentation();
        Token tok;
        if (matcher.find(0) && matcher.groupCount() > 0) {
            int indents = matcher.group(1).length();
            incrementLine(1);
            consume(indents + 1);

            if(!scanner.getInput().isEmpty() && (scanner.getInput().charAt(0) == ' ' || scanner.getInput().charAt(0) == '\t')){
                throw error("INVALID_INDENTATION","Invalid indentation, you can use tabs or spaces but not both");
            }

            // blank line
            if (scanner.isBlankLine()) {
                this.interpolationAllowed = true;
                pushToken(tokEnd(tok(new Newline())));
                return true;
            }

            // outdent
            if (!indentStack.isEmpty() && indents < indentStack.get(0)) {
                int outdent_count = 0;
                while (!indentStack.isEmpty() && indentStack.get(0) > indents) {
                    if(indentStack.size() > 1 && indentStack.get(1) < indents){
                        throw error("INCONSISTENT_INDENTATION","Inconsistent indentation. Expecting either " + indentStack.get(1) + " or " + indentStack.get(0) + " spaces/tabs.");
                    }
                    outdent_count++;
                    indentStack.poll();
                }
                while(outdent_count--!=0){
                    colno=1;
                    tok = tok(new Outdent());
                    if(!indentStack.isEmpty())
                        colno = indentStack.get(0) + 1;
                    else {
                        colno = 1;
                    }
                    pushToken(tokEnd(tok));
                }
            // indent
            } else if (indents > 0 && (indentStack.isEmpty() || indents != indentStack.get(0))) {
                tok = tok(new Indent(String.valueOf(indents), lineno));
                this.colno = 1 + indents;
                tok.setIndents(indents);
                pushToken(tokEnd(tok));
                indentStack.push(indents);
                // newline
            } else {
                tok = tok(new Newline());
                Integer indentStack0 = 0;
                if(!indentStack.isEmpty()) {
                    indentStack0 = indentStack.get(0);
                }
                if(indentStack0==null)
                    indentStack0 = 0;
                this.colno = 1 + Math.min(indentStack0,indents);
                pushToken(tokEnd(tok));
            }
            this.interpolationAllowed = true;
            return true;
        }
        return false;
    }

    private void pushToken(Token token){
        tokens.add(token); // Append to an Array
    }

    private Token tok(Token token){
        try {
            Token newToken = token.clone();
            newToken.setStartLineNumber(this.lineno);
            newToken.setStartColumn(this.colno);
            newToken.setFileName(this.filename);
            return newToken;
        } catch (CloneNotSupportedException e) {
            throw new PugLexerException("Clone Not Supported",this.filename, this.lineno,this.colno,templateLoader);
        }
    }

    private void pipelessText() {
        pipelessText(null);
    }

    private boolean pipelessText(Integer indents) {
        while (blank());
        Matcher matcher = scanIndentation();

        if (matcher.find(0) && !matcher.group(1).isEmpty()) {

            if(indents==null && matcher.groupCount()>0)
                indents = matcher.group(1).length();
            if(indents==null)
                indents=0;

            if (indents > 0 && (this.indentStack.isEmpty() || indents > this.indentStack.get(0))) {
                pushToken(tokEnd(tok(new StartPipelessText())));
                LinkedList<String> tokenList = new LinkedList<>();
                ArrayList<Boolean> token_indent = new ArrayList<>();
                boolean isMatch;

                int stringPtr = 0;
                do {
                    // text has `\n` as a prefix
                    int nextLineBreak = scanner.getInput().substring(stringPtr + 1).indexOf('\n');
                    if (-1 == nextLineBreak)
                        nextLineBreak = scanner.getInput().length() - stringPtr - 1;

                    String line = scanner.getInput().substring(stringPtr + 1,stringPtr + 1 + nextLineBreak);
                    Matcher lineCaptures = indentRe.matcher("\n"+line);
                    int lineIndents = 0;
                    if(lineCaptures.find(0) && lineCaptures.groupCount()>0) {
                        lineIndents = lineCaptures.group(1).length();
                    }

                    isMatch = lineIndents >= indents;
                    token_indent.add(isMatch);
                    isMatch = isMatch || line.trim().isEmpty();
                    if (isMatch) {
                        // consume test along with `\n` prefix if match
                        stringPtr += line.length() + 1;
                        String substring = "";
                        if(indents<=line.length()) {
                            substring = line.substring(indents);
                        }
                        tokenList.add(substring);
                    }else if(!this.indentStack.isEmpty() && lineIndents > this.indentStack.get(0)){
                        // line is indented less than the first line but is still indented
                        // need to retry lexing the text block
                        this.tokens.pollLast();
                        return pipelessText(lineCaptures.group(1).length());
                    }
                } while (scanner.getInput().length() - stringPtr > 0 && isMatch);

                this.consume(stringPtr);

                while (scanner.getInput().isEmpty() && tokenList.get(tokenList.size() - 1).isEmpty())
                    tokenList.remove(tokenList.size() - 1);

                for (int i = 0; i<tokenList.size(); i++) {
                    Token token = null;
                    String tokenString = tokenList.get(i);
                    incrementLine(1);
                    if(i!=0){
                        token = tok(new Newline());
                    }
                    if(token_indent.get(i)){
                        incrementColumn(indents);
                    }
                    if(token!=null){
                        pushToken(tokEnd(token));
                    }
                    this.addText(new Text(),tokenString);
                }

                pushToken(tokEnd(tok(new EndPipelessText())));
                return true;
            }
        }
        return false;
    }

    private boolean slash() {
        Token token = scan(PATTERN_SLASH,new Slash());
        if (token != null) {
            pushToken(tokEnd(token));
            return true;
        }
        return false;
    }

    private boolean colon() {
        Token token = scan(PATTERN_COLON,new Colon());
        if (token != null) {
            pushToken(tokEnd(token));
            return true;
        }
        return false;
    }

    private boolean fail() {
        throw error("UNEXPECTED_TEXT","unexpected text \"" + StringUtils.substring(scanner.getInput(),0,5) + "\"");
    }

    public boolean getPipeless() {
        return pipeless;
    }

    public LinkedList<Token> getTokens(){
        LinkedList<Token> list = new LinkedList<>();
        while(!ended || !tokens.isEmpty()){
            Token advance = advance();
            if(advance!=null)
                list.add(advance);
        }
        return list;
    }

    public String getInput(){
        return scanner.getInput();
    }

    public void assertExpression(String value){
        assertExpression(value, false);
    }

    public boolean assertExpression(String value,boolean noThrow){
        try {
            expressionHandler.assertExpression(value);
            return true;
        } catch (ExpressionException e) {
            if(noThrow) {
                return false;
            }
            throw error("SYNTAX_ERROR","Syntax Error: "+ e.getMessage());
        }
    }
}
