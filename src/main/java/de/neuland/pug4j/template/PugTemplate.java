package de.neuland.pug4j.template;

import java.io.Writer;

import de.neuland.pug4j.Pug4J.Mode;
import de.neuland.pug4j.PugConfiguration;
import de.neuland.pug4j.compiler.Compiler;
import de.neuland.pug4j.exceptions.PugCompilerException;
import de.neuland.pug4j.model.PugModel;
import de.neuland.pug4j.parser.node.DoctypeNode;
import de.neuland.pug4j.parser.node.Node;

public class PugTemplate {

    private Node rootNode;
    private boolean terse = false;
    private boolean xml = false;

    public PugTemplate() {
    }

    public PugTemplate(final Node rootNode) {
        setRootNode(rootNode);
    }

    public PugTemplate(final Node rootNode, final Mode mode) {
        setMode(mode);
        setRootNode(rootNode);
    }

    public void process(PugModel model, Writer writer, PugConfiguration pugConfiguration) throws PugCompilerException {
        Compiler compiler = new Compiler(this, pugConfiguration);
        compiler.compile(model, writer);
    }

    public Node getRootNode() {
        return rootNode;
    }

    //@TODO: Deprecated: Remove in 3.0.0
    public void setRootNode(Node rootNode) {
        final Node peek = rootNode.getNodes().peek();
        if (peek instanceof DoctypeNode) {
            DoctypeNode doctypeNode = (DoctypeNode) peek;
            setDoctype(doctypeNode.getValue());
        }
        this.rootNode = rootNode;
    }

    public boolean isTerse() {
        return terse;
    }

    public boolean isXml() {
        return xml;
    }

    public void setDoctype(String name) {
        this.terse = "html".equals(name);
        this.xml = "xml".equals(name);
    }

    //@TODO: Deprecated: Remove in 3.0.0, use constructor
    public void setMode(Mode mode) {
        setDoctype(mode.name().toLowerCase());
    }
}
