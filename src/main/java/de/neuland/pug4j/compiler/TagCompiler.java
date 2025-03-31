package de.neuland.pug4j.compiler;

import de.neuland.pug4j.PugConfiguration;

public class TagCompiler {
    PugConfiguration configuration;
    AttributesCompiler attributesCompiler;

    public TagCompiler(final PugConfiguration pugConfiguration) {
        this.configuration = pugConfiguration;
        attributesCompiler = new AttributesCompiler(pugConfiguration);
    }

}
