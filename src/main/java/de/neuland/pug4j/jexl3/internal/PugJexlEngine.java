package de.neuland.pug4j.jexl3.internal;


import de.neuland.pug4j.jexl3.PugJexlArithmetic;
import org.apache.commons.jexl3.*;
import org.apache.commons.jexl3.internal.Engine;
import de.neuland.pug4j.jexl3.internal.introspection.PugUberspect;
import org.apache.commons.jexl3.internal.Frame;
import org.apache.commons.jexl3.internal.Interpreter;
import org.apache.commons.jexl3.introspection.JexlUberspect;
import org.apache.commons.logging.LogFactory;

import java.util.List;
import java.util.Map;

public class PugJexlEngine extends Engine {

	/*
	 * using a semi strict interpreter and non strict arithmetic
	 */
	public PugJexlEngine(int cacheSize) {
		super(new JexlBuilder().arithmetic(new PugJexlArithmetic(false)).uberspect(new PugUberspect(LogFactory.getLog(JexlEngine.class),
                new JexlUberspect.ResolverStrategy() {
                    public List<JexlUberspect.PropertyResolver> apply(JexlOperator op, Object obj) {
                        if(obj instanceof Map){
                            return JexlUberspect.MAP;
                        }
                        if (op == JexlOperator.ARRAY_GET) {
                            return JexlUberspect.MAP;
                        } else if (op == JexlOperator.ARRAY_SET) {
                            return JexlUberspect.MAP;
                        } else {
                            return op == null && obj instanceof Map ? JexlUberspect.MAP : JexlUberspect.POJO;
                        }
                    }
                })).strict(false).cache(cacheSize));
	}
    protected Interpreter createInterpreter(final JexlContext context, final Frame frame, final JexlOptions opts) {
        return new PugJexlInterpreter(this, opts, context == null ? EMPTY_CONTEXT : context, frame);
    }
}
