package de.neuland.pug4j.parser.node;

import de.neuland.pug4j.compiler.IndentWriter;
import de.neuland.pug4j.compiler.NodeVisitor;
import de.neuland.pug4j.lexer.token.Doctypes;
import de.neuland.pug4j.model.PugModel;
import org.apache.commons.lang3.StringUtils;

public class DoctypeNode extends Node {

    public String getDoctypeLine() {
        String name = getValue();
        if (name == null || StringUtils.isBlank(name)) {
            name = "html";
        }
        String doctypeLine = Doctypes.get(name);
        if (doctypeLine == null) {
            doctypeLine = "<!DOCTYPE " + name + ">";
        }
        return doctypeLine;
    }

    @Override
    public void accept(NodeVisitor visitor, IndentWriter writer, PugModel model) {
        visitor.visit(this, writer, model);
    }
}
