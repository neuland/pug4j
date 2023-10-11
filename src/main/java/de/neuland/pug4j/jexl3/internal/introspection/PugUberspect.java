package de.neuland.pug4j.jexl3.internal.introspection;

import org.apache.commons.jexl3.internal.introspection.Uberspect;
import org.apache.commons.jexl3.introspection.JexlPermissions;
import org.apache.commons.logging.Log;


public class PugUberspect extends Uberspect {
    public PugUberspect(Log runtimeLogger, ResolverStrategy sty) {
        super(runtimeLogger, sty);
    }

    public PugUberspect(final Log runtimeLogger, final ResolverStrategy sty, final JexlPermissions perms) {
        super(runtimeLogger, sty, perms);
    }
}
