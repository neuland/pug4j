package de.neuland.pug4j.parser.node;

import de.neuland.pug4j.compiler.IndentWriter;
import de.neuland.pug4j.exceptions.PugCompilerException;
import de.neuland.pug4j.model.PugModel;
import de.neuland.pug4j.template.PugTemplate;

/**
 * Created by christoph on 14.10.15.
 */
public class InterpolationNode extends Node {
    @Override
    public void execute(IndentWriter writer, PugModel model, PugTemplate template) throws PugCompilerException {

    }
}
