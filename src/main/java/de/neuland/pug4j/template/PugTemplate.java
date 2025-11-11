package de.neuland.pug4j.template;

import java.io.StringWriter;
import java.io.Writer;
import java.util.Map;

import de.neuland.pug4j.Pug4J.Mode;
import de.neuland.pug4j.PugConfiguration;
import de.neuland.pug4j.PugEngine;
import de.neuland.pug4j.RenderContext;
import de.neuland.pug4j.compiler.Compiler;
import de.neuland.pug4j.exceptions.PugCompilerException;
import de.neuland.pug4j.filter.Filter;
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
        setDoctype(mode.name().toLowerCase());
        setRootNode(rootNode);
    }

    public void process(PugModel model, Writer writer, PugConfiguration pugConfiguration) throws PugCompilerException {
        // Convert deprecated PugConfiguration to new API
        PugEngine engine = PugEngine.builder()
                .templateLoader(pugConfiguration.getTemplateLoader())
                .expressionHandler(pugConfiguration.getExpressionHandler())
                .caching(pugConfiguration.isCaching())
                .filters(pugConfiguration.getFilters())
                .build();

        RenderContext context = RenderContext.builder()
                .prettyPrint(pugConfiguration.isPrettyPrint())
                .defaultMode(pugConfiguration.getMode())
                .build();

        Compiler compiler = new Compiler(this, context, engine);
        compiler.compile(model, writer);
    }

    /**
     * Renders this template with the given model, render context, and engine.
     * This is the new API (3.0+) where templates render themselves.
     *
     * @param model the model data
     * @param context the render context with settings
     * @param engine the engine providing filters and handlers
     * @param writer the writer to output rendered HTML
     * @throws PugCompilerException if rendering fails
     * @since 3.0.0
     */
    public void render(Map<String, Object> model, RenderContext context, PugEngine engine, Writer writer)
            throws PugCompilerException {
        if (model == null) {
            throw new IllegalArgumentException("model cannot be null");
        }
        if (context == null) {
            throw new IllegalArgumentException("context cannot be null");
        }
        if (engine == null) {
            throw new IllegalArgumentException("engine cannot be null");
        }
        if (writer == null) {
            throw new IllegalArgumentException("writer cannot be null");
        }

        // Create PugModel with global variables from context
        PugModel pugModel = new PugModel(context.getGlobalVariables());

        // Add all filters from the engine
        for (Map.Entry<String, Filter> entry : engine.getFilters().entrySet()) {
            pugModel.addFilter(entry.getKey(), entry.getValue());
        }

        // Add user model data
        pugModel.putAll(model);

        // Compile and render
        Compiler compiler = new Compiler(this, context, engine);
        compiler.compile(pugModel, writer);
    }

    /**
     * Renders this template with the given model and render context, returning HTML as a String.
     *
     * @param model the model data
     * @param context the render context with settings
     * @param engine the engine providing filters and handlers
     * @return the rendered HTML as a String
     * @throws PugCompilerException if rendering fails
     * @since 3.0.0
     */
    public String render(Map<String, Object> model, RenderContext context, PugEngine engine)
            throws PugCompilerException {
        StringWriter writer = new StringWriter();
        render(model, context, engine, writer);
        return writer.toString();
    }

    /**
     * Renders this template with the given model using default render settings.
     *
     * @param model the model data
     * @param engine the engine providing filters and handlers
     * @return the rendered HTML as a String
     * @throws PugCompilerException if rendering fails
     * @since 3.0.0
     */
    public String render(Map<String, Object> model, PugEngine engine) throws PugCompilerException {
        return render(model, RenderContext.defaults(), engine);
    }

    public Node getRootNode() {
        return rootNode;
    }


    private void setRootNode(Node rootNode) {
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

    private void setDoctype(String name) {
        this.terse = "html".equals(name);
        this.xml = "xml".equals(name);
    }

}
