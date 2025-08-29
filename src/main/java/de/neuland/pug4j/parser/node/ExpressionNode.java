package de.neuland.pug4j.parser.node;

import de.neuland.pug4j.PugConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;

import de.neuland.pug4j.compiler.IndentWriter;
import de.neuland.pug4j.exceptions.ExpressionException;
import de.neuland.pug4j.exceptions.PugCompilerException;
import de.neuland.pug4j.model.PugModel;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static de.neuland.pug4j.model.PugModel.PUG4J_MODEL_PREFIX;

public class ExpressionNode extends Node {

    private boolean escape;
    private boolean buffer;
    private boolean inline;
    private String nodeId;

    public ExpressionNode() {
        super();
        nodeId = createNodeId();
    }

    public void setEscape(boolean escape) {
        this.escape = escape;
    }

    public boolean isEscape() {
        return escape;
    }

    public void setBuffer(boolean buffer) {
        this.buffer = buffer;
    }

    public boolean isBuffer() {
        return buffer;
    }

    public String getNodeId() {
        return nodeId;
    }

    public boolean isInline() {
        return inline;
    }

    public void setInline(boolean inline) {
        this.inline = inline;
    }

    private String createNodeId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    @Override
    public void setValue(String value) {
        super.setValue(value.trim());
    }
}
