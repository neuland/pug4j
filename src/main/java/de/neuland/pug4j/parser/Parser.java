package de.neuland.pug4j.parser;

import de.neuland.pug4j.exceptions.PugParserException;
import de.neuland.pug4j.expression.ExpressionHandler;
import de.neuland.pug4j.lexer.token.Each;
import de.neuland.pug4j.lexer.Lexer;
import de.neuland.pug4j.lexer.token.*;
import de.neuland.pug4j.parser.node.*;
import de.neuland.pug4j.template.TemplateLoader;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.Reader;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Parser {

    public static final Pattern PATTERN_REST = Pattern.compile("^\\.\\.\\.");
    private final Lexer lexer;
    private LinkedHashMap<String, BlockNode> blocks = new LinkedHashMap<>();
    private final TemplateLoader templateLoader;
    private final ExpressionHandler expressionHandler;
    private Parser extending;
    private final String filename;
    private LinkedList<Parser> contexts = new LinkedList<>();
    private int inMixin = 0;
    private Map<String, MixinNode> mixins = new HashMap<>();
    private final PathHelper pathHelper = new PathHelper();
    private Node extendingNode;

    public Parser(String filename, TemplateLoader templateLoader, ExpressionHandler expressionHandler) throws IOException {
        this.filename = filename;
        this.templateLoader = templateLoader;
        this.expressionHandler = expressionHandler;
        lexer = new Lexer(filename, templateLoader, expressionHandler);
        getContexts().push(this);
    }

    public Parser(String src, String filename, TemplateLoader templateLoader, ExpressionHandler expressionHandler) throws IOException {
        this.filename = filename;
        this.templateLoader = templateLoader;
        this.expressionHandler = expressionHandler;
        lexer = new Lexer(src, filename, templateLoader, expressionHandler);
        getContexts().push(this);
    }

    private PugParserException error(String code, String message, Token token) {
        return new PugParserException(this.filename, token.getStartLineNumber(), token.getStartColumn(), templateLoader, message, code);
    }

    private BlockNode emptyBlock() {
        return this.emptyBlock(0);
    }

    private BlockNode emptyBlock(int line) {
        return this.initBlock(line, new LinkedList<>());
    }

    public Node parse() {
        BlockNode block = emptyBlock(0);
        while (!(peek() instanceof Eos)) {
            if (peek() instanceof Newline) {
                advance();
            } else if (peek() instanceof TextHtml) {
                block.getNodes().addAll(parseTextHtml());
            } else {
                Node expr = parseExpr();
                if (expr != null) {
                    if (expr instanceof BlockNode && !((BlockNode) expr).isYield() && !((BlockNode) expr).isNamedBlock()) {
                        block.getNodes().addAll(expr.getNodes());
                    } else {
                        block.push(expr);
                    }
                }
            }
        }

        if (extending != null) {
            Node rootNode = extendingNode;

            // hoist mixins
            Set<String> keySet = this.mixins.keySet();
            for (String name : keySet) {
                rootNode.getNodes().push(this.mixins.get(name));
            }
            return rootNode;
        }

        return block;
    }

    private Node parseExpr() {
        Token token = peek();
        if (token instanceof Tag) {
            return parseTag();
        }
        if (token instanceof Mixin) {
            return parseMixin();
        }
        if (token instanceof Block) {
            return parseBlock();
        }
        if (token instanceof MixinBlock) {
            return parseMixinBlock();
        }
        if (token instanceof CaseToken) {
            return parseCase();
        }
        if (token instanceof ExtendsToken) {
            return parseExtends();
        }
        if (token instanceof Include) {
            return parseInclude();
        }
        if (token instanceof Doctype) {
            return parseDoctype();
        }
        if (token instanceof Filter) {
            return parseFilter();
        }
        if (token instanceof Comment) {
            return parseComment();
        }
        if (token instanceof Text || token instanceof InterpolatedCode || token instanceof StartPugInterpolation) {
            return parseText(true);
        }
        if (token instanceof TextHtml) {
            return initBlock(this.peek().getStartLineNumber(), parseTextHtml());
        }
        if (token instanceof Dot) {
            return parseDot();
        }
        if (token instanceof Each) {
            return parseEach();
        }
        if (token instanceof Expression) {
            return parseCode();
        }
        if (token instanceof BlockCode) {
            return parseBlockCode();
        }
        if (token instanceof If) {
            return parseConditional();
        }
        if (token instanceof While) {
            return parseWhile();
        }
        if (token instanceof Call) {
            return parseCall();
        }
        if (token instanceof Interpolation) {
            return parseInterpolation();
        }
        if (token instanceof Yield) {
            return parseYield();
        }
        if (token instanceof CssClass || token instanceof CssId) {
            return parseCssClassOrId();
        }
        throw error("INVALID_TOKEN", "unexpected token \"" + peek().getType() + "\"", peek());
    }

    private BlockNode initBlock(int startLineNumber, LinkedList<Node> nodes) {
        if (nodes == null) {
            throw new PugParserException(this.filename, this.line(), lexer.getColno(), templateLoader, "`nodes` is not an array");
        }
        BlockNode blockNode = new BlockNode();
        blockNode.setNodes(nodes);
        blockNode.setLineNumber(startLineNumber);
        blockNode.setFileName(this.filename);
        return blockNode;
    }

    private Node parseBlockCode() {
        Token tok = this.expect(BlockCode.class);
        int line = tok.getStartLineNumber();
        int column = tok.getStartColumn();
        Token body = this.peek();
        StringBuilder text = new StringBuilder();

        if (body instanceof StartPipelessText) {
            advance();
            while (!(peek() instanceof EndPipelessText)) {
                tok = advance();
                if (tok instanceof Text) {
                    text.append(tok.getValue());
                } else if (tok instanceof Newline) {
                    text.append("\n");
                } else {
                    throw error("INVALID_TOKEN", "Unexpected token type: " + tok.getType(), tok);
                }
            }
            advance();
        }

        ExpressionNode node = new ExpressionNode();
        node.setValue(text.toString());
        node.setBuffer(false);
        node.setEscape(false);
        node.setInline(false);
        node.setLineNumber(line);
        node.setColumn(column);
        return node;
    }

    private Node parseComment() {
        Token token = expect(Comment.class);

        Node block = this.parseTextBlock();
        if (block != null) {
            BlockCommentNode node = new BlockCommentNode();
            node.setValue(token.getValue());
            node.setBlock(block);
            node.setBuffered(token.isBuffer());
            node.setLineNumber(token.getStartLineNumber());
            node.setColumn(token.getStartColumn());
            node.setFileName(filename);
            return node;
        } else {
            CommentNode node = new CommentNode();
            node.setValue(token.getValue());
            node.setBuffered(token.isBuffer());
            node.setLineNumber(token.getStartLineNumber());
            node.setColumn(token.getStartColumn());
            node.setFileName(filename);
            return node;
        }
    }

    private Node parseMixin() {
        Mixin mixinToken = (Mixin) expect(Mixin.class);


        if (peek() instanceof Indent) {
            this.inMixin++;
            MixinNode node = new MixinNode();
            node.setName(mixinToken.getValue());
            node.setLineNumber(mixinToken.getStartLineNumber());
            node.setColumn(mixinToken.getStartColumn());
            node.setFileName(filename);

            if (StringUtils.isNotBlank(mixinToken.getArguments())) {
                node.setArguments(mixinToken.getArguments());
            }
            List<String> args = node.getArguments();

            String rest;

            if (!args.isEmpty()) {
                Matcher matcher = PATTERN_REST.matcher(args.get(args.size() - 1).trim());
                if (matcher.find(0)) {
                    rest = args.remove(args.size() - 1).trim().replaceAll("^\\.\\.\\.", "");
                    node.setRest(rest);

                }
            }
            List<String> newArgs = new ArrayList<>();
            HashMap<String, String> defaultValues = new HashMap<>();
            for (String arg : args) {
                String key;
                Matcher matcher = Pattern.compile("^([a-zA-Z][a-zA-Z0-9]*)=(.*)$").matcher(arg);
                if (matcher.find(0) && matcher.groupCount() > 1) {
                    key = matcher.group(1);
                    defaultValues.put(key, matcher.group(2));
                } else {
                    key = arg;
                }
                newArgs.add(key);
            }
            node.setArguments(newArgs);
            node.setDefaultValues(defaultValues);
            node.setBlock(block());
            node.setCall(false);
            this.mixins.put(mixinToken.getValue(), node);
            this.inMixin--;
            return node;
        } else {
            throw error("MIXIN_WITHOUT_BODY", "Mixin " + mixinToken.getValue() + " declared without body", mixinToken);
        }
    }

    private Node parseCall() {
        Call callToken = (Call) expect(Call.class);
        CallNode callNode = new CallNode();
        callNode.setBlock(emptyBlock(callToken.getStartLineNumber()));
        callNode.setName(callToken.getValue());
        callNode.setLineNumber(callToken.getStartLineNumber());
        callNode.setColumn(callToken.getStartColumn());
        callNode.setFileName(filename);
        callNode.setCall(true);

        if (StringUtils.isNotBlank(callToken.getArguments())) {
            callNode.setArguments(callToken.getArguments());
        }


        this.tag(callNode);

        if (callNode.hasBlock() && callNode.getBlock().getNodes().isEmpty())
            callNode.setBlock(null);

        return callNode;
    }

    private Node parseCssClassOrId() {
        Tag div = new Tag("div");
        div.setStartColumn(peek().getStartColumn());
        div.setStartLineNumber(peek().getStartLineNumber());
        div.setEndColumn(peek().getEndColumn());
        div.setEndLineNumber(peek().getEndLineNumber());
        div.setFileName(this.filename);
        lexer.defer(div);
        return parseExpr();
    }

    private BlockNode parseBlock() {
        Block blockToken = (Block) expect(Block.class);
        String mode = blockToken.getMode();
        String name = blockToken.getValue().trim();

        BlockNode blockNode;
        if (peek() instanceof Indent) {
            blockNode = block();
        } else {
            blockNode = emptyBlock(blockToken.getStartLineNumber());
        }
        blockNode.setNamedBlock(true);
        blockNode.setName(name);
        blockNode.setLineNumber(blockToken.getStartLineNumber());
        blockNode.setColumn(blockToken.getStartColumn());
        blockNode.setFileName(this.filename);
        blockNode.setMode(mode);

        BlockNode prev = this.blocks.get(name);
        if (prev != null) {
            LinkedList<Node> nodes = new LinkedList<>();
            if ("replace".equals(mode)) {
                nodes = blockNode.getNodes();
            } else if ("append".equals(mode)) {
                nodes.addAll(prev.getNodes());
                nodes.addAll(blockNode.getNodes());
            } else if ("prepend".equals(mode)) {
                nodes.addAll(blockNode.getNodes());
                nodes.addAll(prev.getNodes());
            }
            prev.setNodes(nodes);
            blockNode = prev;
        }

        blocks.put(name, blockNode);
        return blockNode;

    }

    private Node parseMixinBlock() {
        Token tok = expect(MixinBlock.class);
        if (this.inMixin == 0) {
            throw error("BLOCK_OUTSIDE_MIXIN", "Anonymous blocks are not allowed unless they are part of a mixin.", tok);
        }

        MixinBlockNode mixinBlockNode = new MixinBlockNode();
        mixinBlockNode.setLineNumber(tok.getStartLineNumber());
        mixinBlockNode.setColumn(tok.getStartColumn());
        mixinBlockNode.setFileName(this.filename);
        return mixinBlockNode;
    }

    private Node parseInclude() {
        Include includeToken = (Include) expect(Include.class);

        LinkedList<IncludeFilterNode> filters = new LinkedList<>();
        while (peek() instanceof Filter) {
            filters.add(parseIncludeFilter());
        }

        Path pathToken = (Path) expect(Path.class);

        String templateName = pathToken.getValue().trim();
        String path = pathHelper.resolvePath(filename, templateName, templateLoader.getBase());
        if (path == null) {
            throw new PugParserException(
                    this.filename,
                    lexer.getLineno(),
                    lexer.getColno(),
                    templateLoader,
                    "The template [" + templateName + "] could not be opened. Maybe outside template path.");
        }


        try {
            if (!filters.isEmpty()) {
                Reader reader = templateLoader.getReader(path);
                FilterNode node = new FilterNode();
                node.setFilter(filters);
                node.setLineNumber(line());
                node.setFileName(filename);

                TextNode text = new TextNode();
                text.setValue(IOUtils.toString(reader));

                BlockNode block = new BlockNode();
                LinkedList<Node> nodes = new LinkedList<>();
                nodes.add(text);
                block.setNodes(nodes);
                block.setLineNumber(includeToken.getStartLineNumber());
                block.setFileName(this.filename);
                node.setBlock(block);
                return node;
            }
        } catch (IOException e) {
            throw error("PATH_EXCEPTION", "the included file [" + templateName + "] could not be opened\n" + e.getMessage(), pathToken);
        }

        // non-jade
        String extension = FilenameUtils.getExtension(path);
        if (!templateLoader.getExtension().equals(extension) && !extension.isEmpty()) {
            try {
                Reader reader = templateLoader.getReader(path);
                LiteralNode node = new LiteralNode();
                node.setLineNumber(pathToken.getStartLineNumber());
                node.setColumn(pathToken.getStartColumn());
                node.setFileName(filename);
                node.setValue(IOUtils.toString(reader));
                return node;
            } catch (IOException e) {
                throw error("PATH_EXCEPTION", "the included file [" + templateName + "] could not be opened\n" + e.getMessage(), pathToken);
            }
        }

        Parser parser = createParser(templateName);
        parser.setBlocks(blocks);
        parser.setMixins(mixins);
        contexts.push(parser);
        Node ast = parser.parse();
        contexts.pop();
        ast.setFileName(path);
        if (peek() instanceof Indent) {
            //Fill YieldBlock with Nodes and make it a normal Block
            BlockNode includeBlock = ((BlockNode) ast).getYieldBlock();
            includeBlock.push(block());
            includeBlock.setYield(false);
        }

        return ast;
    }

    private Node parseExtends() {
        expect(ExtendsToken.class);
        Path path = (Path) expect(Path.class);

        String templateName = path.getValue().trim();
        Parser parser = createParser(templateName);

        parser.setContexts(contexts);
        extending = parser;
        getContexts().push(extending);
        extendingNode = extending.parse();
        this.blocks = parser.getBlocks();
        getContexts().pop();

        LiteralNode node = new LiteralNode();
        node.setValue("");
        return node;
    }

    private Parser createParser(String templateName) {
        templateName = ensurePugExtension(templateName);
        try {
            String resolvedPath = pathHelper.resolvePath(filename, templateName, templateLoader.getBase());
            if (resolvedPath == null) {
                throw new PugParserException(
                        this.filename,
                        lexer.getLineno(),
                        lexer.getColno(),
                        templateLoader,
                        "The template [" + templateName + "] could not be opened. Maybe outside template path.");
            }
            return new Parser(resolvedPath, templateLoader, expressionHandler);
        } catch (IOException e) {
            throw new PugParserException(
                    this.filename,
                    lexer.getLineno(),
                    lexer.getColno(),
                    templateLoader,
                    "The template [" + templateName + "] could not be opened. \n" + e.getMessage());
        }
    }

    private String ensurePugExtension(String templateName) {
        if (!templateLoader.getExtension().equals(FilenameUtils.getExtension(templateName))) {
            return templateName + "." + templateLoader.getExtension();
        }
        return templateName;
    }

    private BlockNode parseYield() {
        Token token = expect(Yield.class);
        BlockNode block = new BlockNode();
        block.setLineNumber(token.getStartLineNumber());
        block.setColumn(token.getStartColumn());
        block.setFileName(filename);
        block.setYield(true);
        return block;
    }

    private Node parseInterpolation() {
        Token token = advance();
        String name = token.getValue();
        TagNode tagNode = new TagNode();
        tagNode.setBlock(emptyBlock(token.getStartLineNumber()));
        tagNode.setLineNumber(token.getStartLineNumber());
        tagNode.setColumn(token.getStartColumn());
        tagNode.setFileName(filename);
        tagNode.setName(name);
        tagNode.setInterpolated(true);
        return this.tag(tagNode, true);
    }

    private BlockNode block() {
        Token token = expect(Indent.class);

        BlockNode block = emptyBlock(token.getStartLineNumber());
        while (!(peek() instanceof Outdent)) {
            if (peek() instanceof Newline) {
                advance();
            } else if (peek() instanceof TextHtml) {
                block.getNodes().addAll(parseTextHtml());
            } else {
                Node expr = parseExpr();
                if (expr != null) {
                    if (expr instanceof BlockNode && !((BlockNode) expr).isYield() && !((BlockNode) expr).isNamedBlock()) {
                        block.getNodes().addAll(expr.getNodes());
                    } else {
                        block.push(expr);
                    }
                }
            }
        }
        expect(Outdent.class);
        return block;
    }

    private Node parseText() {
        return parseText(false);
    }

    private Node parseText(boolean block) {
        LinkedList<Node> tags = new LinkedList<>();
        int lineno = peek().getStartLineNumber();
        Token nextToken = peek();

        while (true) {
            if (nextToken instanceof Text) {
                Token token = advance();
                TextNode textNode = new TextNode();
                textNode.setValue(token.getValue());
                textNode.setLineNumber(token.getStartLineNumber());
                textNode.setColumn(token.getStartColumn());
                textNode.setFileName(this.filename);
                tags.add(textNode);
            } else if (nextToken instanceof InterpolatedCode) {
                InterpolatedCode token = (InterpolatedCode) advance();
                ExpressionNode expressionNode = new ExpressionNode();
                expressionNode.setValue(token.getValue());
                expressionNode.setBuffer(token.isBuffer());
                expressionNode.setEscape(token.isMustEscape());
                expressionNode.setInline(true);
                expressionNode.setLineNumber(token.getStartLineNumber());
                expressionNode.setColumn(token.getStartColumn());
                expressionNode.setFileName(this.filename);
                tags.add(expressionNode);
            } else if (nextToken instanceof Newline) {
                if (!block)
                    break;
                Token token = advance();
                Token nextType = peek();
                if (nextType instanceof Text || nextType instanceof InterpolatedCode) {
                    TextNode textNode = new TextNode();
                    textNode.setValue("\n");
                    textNode.setLineNumber(token.getStartLineNumber());
                    textNode.setColumn(token.getStartColumn());
                    textNode.setFileName(this.filename);
                    tags.add(textNode);
                }

            } else if (nextToken instanceof StartPugInterpolation) {
                advance();
                tags.add(parseExpr());
                expect(EndPugInterpolation.class);
            } else {
                break;
            }
            nextToken = peek();
        }
        if (tags.size() == 1)
            return tags.get(0);
        else
            return initBlock(lineno, tags);
    }

    private LinkedList<Node> parseTextHtml() {
        LinkedList<Node> nodes = new LinkedList<>();
        Node currentNode = null;
        while (true) {
            if (peek() instanceof TextHtml) {
                Token text = advance();
                if (currentNode == null) {
                    TextNode textNode = new TextNode();
                    textNode.setValue(text.getValue());
                    textNode.setFileName(this.filename);
                    textNode.setLineNumber(text.getStartLineNumber());
                    textNode.setColumn(text.getStartColumn());
                    textNode.setHtml(true);
                    currentNode = textNode;
                    nodes.add(currentNode);
                } else {
                    currentNode.setValue(currentNode.getValue() + "\n" + text.getValue());
                }
            } else if (peek() instanceof Indent) {
                BlockNode block = block();
                LinkedList<Node> blockNodes = block.getNodes();
                for (Node node : blockNodes) {
                    if (node instanceof TextNode && ((TextNode) node).isHtml()) {
                        if (currentNode == null) {
                            currentNode = node;
                            nodes.add(currentNode);
                        } else {
                            currentNode.setValue(currentNode.getValue() + "\n" + node.getValue());
                        }
                    } else {
                        currentNode = null;
                        nodes.add(node);
                    }
                }
            } else if (peek() instanceof Expression) {
                currentNode = null;
                nodes.add(parseCode(true));
            } else if (peek() instanceof Newline) {
                advance();
            } else {
                break;
            }
        }
        return nodes;
    }

    private Node parseDot() {
        this.advance();
        return parseTextBlock();
    }

    private Node parseEach() {
        Each eachToken = (Each) expect(Each.class);
        EachNode node = new EachNode();
        node.setValue(eachToken.getValue());
        node.setKey(eachToken.getKey());
        node.setCode(eachToken.getCode());
        node.setLineNumber(eachToken.getStartLineNumber());
        node.setColumn(eachToken.getStartColumn());
        node.setFileName(filename);
        node.setBlock(block());
        if (peek() instanceof Else) {
            advance();
            node.setElseNode(block());
        }
        return node;
    }


    private Node parseWhile() {
        While whileToken = (While) expect(While.class);
        WhileNode node = new WhileNode();
        node.setValue(whileToken.getValue());
        node.setLineNumber(whileToken.getStartLineNumber());
        node.setColumn(whileToken.getStartColumn());
        node.setFileName(filename);

        //handle block
        if (peek() instanceof Indent) {
            node.setBlock(block());
        } else {
            node.setBlock(emptyBlock());
        }

        return node;
    }

    private Node parseTag() {
        Token token = advance();
        String name = token.getValue();
        TagNode tagNode = new TagNode();
        tagNode.setName(name);
        tagNode.setBlock(emptyBlock());
        tagNode.setLineNumber(token.getStartLineNumber());
        tagNode.setColumn(token.getStartColumn());
        tagNode.setFileName(filename);
        tagNode.setValue(name);
        return this.tag(tagNode, true);
    }

    private void tag(AttrsNode tagNode) {
        tag(tagNode, false);
    }

    private Node tag(AttrsNode tagNode, boolean selfClosingAllowed) {
        // ast-filter look-ahead
        boolean seenAttrs = false;
        while (true) {
            Token incomingToken = peek();
            if (incomingToken instanceof CssId) {
                Token tok = advance();
                tagNode.setAttribute("id", tok.getValue(), false);
            } else if (incomingToken instanceof CssClass) {
                Token tok = advance();
                tagNode.setAttribute("class", tok.getValue(), false);
            } else if (incomingToken instanceof StartAttributes) {
                if (seenAttrs) {
                    throw new PugParserException(filename, line(), lexer.getColno(), templateLoader, this.filename + ", line " + this.peek().getStartLineNumber() + ":\nYou should not have jade tags with multiple attributes.");
                }
                seenAttrs = true;
                parseAttributes(tagNode);
            } else if (incomingToken instanceof AttributesBlock) {
                Token tok = this.advance();
                tagNode.addAttributes(tok.getValue());
            } else {
                break;
            }
        }

        // check immediate '.'
        if (peek() instanceof Dot) {
            tagNode.setTextOnly(true);
            advance();
        }

        // (text | code | ':')?
        if (peek() instanceof Text || peek() instanceof InterpolatedCode) {
            Node node = parseText();
            if (node instanceof BlockNode) {
                BlockNode block = (BlockNode) node;
                tagNode.getBlock().getNodes().addAll(block.getNodes());
            } else {
                tagNode.getBlock().push(node);
            }
        } else if (peek() instanceof Expression) {
            tagNode.getBlock().push(parseCode(true));
        } else if (peek() instanceof Colon) {
            advance();
            Node node = parseExpr();
            if (node instanceof BlockNode) {
                tagNode.setBlock(node);
            } else {
                LinkedList<Node> nodes = new LinkedList<>();
                nodes.add(node);
                tagNode.setBlock(initBlock(tagNode.getLineNumber(), nodes));
            }
        } else if (peek() instanceof Slash) {
            if (selfClosingAllowed) {
                advance();
                tagNode.setSelfClosing(true);
            }
        }

        // newline*
        while (peek() instanceof Newline) {
            advance();
        }
        if (tagNode.isTextOnly()) {
            Node block = this.parseTextBlock();
            if (block == null)
                block = emptyBlock(tagNode.getLineNumber());
            tagNode.setBlock(block);
        } else if (peek() instanceof Indent) {
            BlockNode block = block();
            for (int i = 0, len = block.getNodes().size(); i < len; ++i) {
                tagNode.getBlock().push(block.getNodes().get(i));
            }
        }

        return tagNode;

    }

    private void parseAttributes(AttrsNode attrsNode) {
        expect(StartAttributes.class);

        Token token = advance();
        while (token instanceof Attribute) {

            Attribute attr = (Attribute) token;
            String name = attr.getName();
            Object value = attr.getAttributeValue();
            attrsNode.setAttribute(name, value, attr.mustEscape());

            token = advance();
        }
        defer(token);
        expect(EndAttributes.class);
    }

    private BlockNode parseTextBlock() {
        Token token = accept(StartPipelessText.class);
        if (token == null)
            return null;

        BlockNode blockNode = emptyBlock(token.getStartLineNumber());
        while (!(peek() instanceof EndPipelessText)) {
            token = advance();
            if (token instanceof Text) {
                TextNode textNode = new TextNode();
                textNode.setValue(token.getValue());
                textNode.setLineNumber(token.getStartLineNumber());
                textNode.setColumn(token.getStartColumn());
                textNode.setFileName(this.filename);
                blockNode.getNodes().add(textNode);
            } else if (token instanceof Newline) {
                TextNode textNode = new TextNode();
                textNode.setValue("\n");
                textNode.setLineNumber(token.getStartLineNumber());
                textNode.setColumn(token.getStartColumn());
                textNode.setFileName(this.filename);
                blockNode.getNodes().add(textNode);
            } else if (token instanceof StartPugInterpolation) {
                blockNode.getNodes().add(parseExpr());
                expect(EndPugInterpolation.class);
            } else if (token instanceof InterpolatedCode) {
                ExpressionNode expressionNode = new ExpressionNode();
                expressionNode.setValue(token.getValue());
                expressionNode.setBuffer(token.isBuffer());
                expressionNode.setEscape(((InterpolatedCode) token).isMustEscape());
                expressionNode.setInline(true);
                expressionNode.setLineNumber(token.getStartLineNumber());
                expressionNode.setColumn(token.getStartColumn());
                expressionNode.setFileName(this.filename);
                blockNode.getNodes().add(expressionNode);
            } else {
                throw error("INVALID_TOKEN", "Unexpected token type: " + token.getType(), token);
            }
        }
        advance();
        return blockNode;
    }

    private Node parseConditional() {
        If conditionalToken = (If) expect(If.class);
        ConditionalNode conditional = new ConditionalNode();
        conditional.setLineNumber(conditionalToken.getStartLineNumber());
        conditional.setColumn(conditionalToken.getStartColumn());
        conditional.setFileName(filename);

        IfConditionNode main = new IfConditionNode(conditionalToken.getValue(), conditionalToken.getStartLineNumber());
        main.setInverse(conditionalToken.isInverseCondition());
        main.setFileName(filename);
        if (peek() instanceof Indent) {
            main.setBlock(block());
        } else {
            main.setBlock(emptyBlock());
        }
        conditional.addCondition(main);

        while (true) {
            if (peek() instanceof Newline) {
                expect(Newline.class);
            } else if (peek() instanceof ElseIf) {
                ElseIf token = (ElseIf) expect(ElseIf.class);
                IfConditionNode elseIf = new IfConditionNode(token.getValue(), token.getStartLineNumber());
                elseIf.setFileName(filename);
                if (peek() instanceof Indent) {
                    elseIf.setBlock(block());
                } else {
                    elseIf.setBlock(emptyBlock());
                }
                conditional.addCondition(elseIf);
            } else if (peek() instanceof Else) {
                Else token = (Else) expect(Else.class);
                IfConditionNode elseNode = new IfConditionNode(null, token.getStartLineNumber());
                elseNode.setFileName(filename);
                elseNode.setDefault(true);
                if (peek() instanceof Indent) {
                    elseNode.setBlock(block());
                } else {
                    elseNode.setBlock(emptyBlock());
                }
                conditional.addCondition(elseNode);

                break;
            } else {
                break;
            }
        }

        return conditional;
    }

    private BlockNode parseBlockExpansion() {
        Token token = accept(Colon.class);
        if (token != null) {
            Node node = this.parseExpr();
            if (node instanceof BlockNode) {
                return (BlockNode) node;
            } else {
                LinkedList<Node> nodes = new LinkedList<>();
                nodes.add(node);
                return initBlock(node.getLineNumber(), nodes);
            }
        } else {
            return this.block();
        }
    }

    private CaseNode parseCase() {
        Token token = expect(CaseToken.class);
        String val = token.getValue();
        CaseNode node = new CaseNode();
        node.setValue(val);
        node.setLineNumber(token.getStartLineNumber());
        node.setColumn(token.getStartColumn());
        node.setFileName(this.filename);

        Node block = emptyBlock(token.getStartLineNumber() + 1);
        expect(Indent.class);
        while (!(peek() instanceof Outdent)) {
            if (peek() instanceof Comment) {
                advance();
            } else if (peek() instanceof Newline) {
                advance();
            } else if (peek() instanceof When) {
                block.push(parseWhen());
            } else if (peek() instanceof Default) {
                block.push(parseDefault());
            } else {
                throw error("INVALID_TOKEN", "Unexpected token \"" + this.peek() + "\", expected \"when\", \"default\" or \"newline\"", peek());
            }
        }
        expect(Outdent.class);
        node.setBlock(block);
        return node;
    }

    private Node parseWhen() {
        Token token = this.expect(When.class);
        String val = token.getValue();
        CaseNode.When when = new CaseNode.When();
        when.setValue(val);
        when.setLineNumber(token.getStartLineNumber());
        when.setColumn(token.getStartColumn());
        when.setFileName(this.filename);
        if (!(this.peek() instanceof Newline)) {
            when.setBlock(this.parseBlockExpansion());
        }
        return when;
    }

    private Node parseDefault() {
        Token token = expect(Default.class);
        Node when = new CaseNode.When();
        when.setValue("default");
        when.setBlock(this.parseBlockExpansion());
        when.setLineNumber(token.getStartLineNumber());
        when.setColumn(token.getStartColumn());
        when.setFileName(this.filename);
        return when;
    }

    private Node parseCode() {
        return parseCode(false);
    }

    private Node parseCode(boolean noBlock) {
        Token token = expect(Expression.class);
        Expression expressionToken = (Expression) token;
        ExpressionNode codeNode = new ExpressionNode();
        codeNode.setValue(expressionToken.getValue());
        codeNode.setBuffer(expressionToken.isBuffer());
        codeNode.setEscape(expressionToken.isEscape());
        codeNode.setInline(noBlock);
        codeNode.setLineNumber(expressionToken.getStartLineNumber());
        codeNode.setColumn(expressionToken.getStartColumn());
        codeNode.setFileName(filename);
        if (noBlock)
            return codeNode;
        boolean block;
        block = peek() instanceof Indent;
        if (block) {
            if (token.isBuffer()) {
                throw error("BLOCK_IN_BUFFERED_CODE", "Buffered code cannot have a block attached to it", peek());
            }
            codeNode.setBlock(block());
        }
        return codeNode;
    }

    private Node parseDoctype() {
        Doctype doctype = (Doctype) expect(Doctype.class);
        DoctypeNode doctypeNode = new DoctypeNode();
        doctypeNode.setValue(doctype.getValue());
        doctypeNode.setLineNumber(doctype.getStartLineNumber());
        doctypeNode.setColumn(doctype.getStartColumn());
        doctypeNode.setFileName(this.filename);
        return doctypeNode;
    }

    private IncludeFilterNode parseIncludeFilter() {
        Filter token = (Filter) expect(Filter.class);
        IncludeFilterNode includeFilter = new IncludeFilterNode();
        includeFilter.setValue(token.getValue());
        includeFilter.setLineNumber(token.getStartLineNumber());
        includeFilter.setColumn(token.getStartColumn());
        includeFilter.setFileName(this.filename);
        if (peek() instanceof StartAttributes) {
            parseAttributes(includeFilter);
        }

        return includeFilter;
    }

    private Node parseFilter() {
        Filter filterToken = (Filter) expect(Filter.class);

        FilterNode node = new FilterNode();
        node.setValue(filterToken.getValue());
        node.setLineNumber(line());
        node.setFileName(filename);
        node.setColumn(filterToken.getStartColumn());

        if (peek() instanceof StartAttributes) {
            parseAttributes(node);
        }
        BlockNode blockNode;
        if (peek() instanceof Text) {
            Token textToken = advance();
            LinkedList<Node> nodes = new LinkedList<>();
            TextNode textNode = new TextNode();
            textNode.setValue(textToken.getValue());
            textNode.setLineNumber(textToken.getStartLineNumber());
            textNode.setColumn(textToken.getStartColumn());
            textNode.setFileName(this.filename);
            nodes.add(textNode);
            blockNode = initBlock(textToken.getStartLineNumber(), nodes);
        } else if (peek() instanceof Filter) {
            LinkedList<Node> nodes = new LinkedList<>();
            nodes.add(parseFilter());
            blockNode = initBlock(filterToken.getStartLineNumber(), nodes);
        } else {
            BlockNode textBlock = parseTextBlock();
            if (textBlock != null) {
                blockNode = textBlock;
            } else {
                blockNode = emptyBlock(filterToken.getStartLineNumber());
            }
        }

        node.setBlock(blockNode);
        return node;
    }

    private Token peek() {
        return lexer.lookahead(0);
    }

    private Token advance() {
        return lexer.advance();
    }

    private void defer(Token token) {
        lexer.defer(token);
    }

    private <T extends Token> T accept(Class<T> clazz) {
        if (this.peek().getClass().equals(clazz)) {
            return clazz.cast(lexer.advance());
        }
        return null;
    }

    private int line() {
        return lexer.getLineno();
    }

    private <T extends Token> T expect(Class<T> expectedTokenClass) {
        Token t = this.peek();
        if (t.getClass().equals(expectedTokenClass)) {
            return expectedTokenClass.cast(advance());
        } else {
            throw error("INVALID_TOKEN", "expected \"" + expectedTokenClass.toString() + "\", but got " + peek().getType() + "\"", peek());
        }
    }

    public LinkedHashMap<String, BlockNode> getBlocks() {
        return blocks;
    }

    public void setBlocks(LinkedHashMap<String, BlockNode> blocks) {
        this.blocks = blocks;
    }

    public LinkedList<Parser> getContexts() {
        return contexts;
    }

    public void setContexts(LinkedList<Parser> contexts) {
        this.contexts = contexts;
    }

    public void setMixins(Map<String, MixinNode> mixins) {
        this.mixins = mixins;
    }
}
