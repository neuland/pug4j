package de.neuland.pug4j.expression;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyObject;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * Temporary feasibility probe: can a ProxyObject act as a `with` scope in GraalJS, so the model
 * can be bound directly instead of copied into the global bindings per expression?
 */
public class WithProxyScopeProbe {

    @Test
    public void probe() {
        Map<String, Object> model = new HashMap<>();
        model.put("a", 40);
        model.put("b", 2);

        ProxyObject modelProxy = new ProxyObject() {
            @Override
            public Object getMember(String key) {
                System.out.println("PROBE read  " + key);
                return model.get(key);
            }

            @Override
            public Object getMemberKeys() {
                return model.keySet().toArray();
            }

            @Override
            public boolean hasMember(String key) {
                return model.containsKey(key);
            }

            @Override
            public void putMember(String key, Value value) {
                System.out.println("PROBE write " + key + " = " + value);
                model.put(key, value.as(Object.class));
            }
        };

        try (Context context = Context.newBuilder("js").allowAllAccess(true).build()) {
            context.getBindings("js").putMember("pug4j__model", modelProxy);

            // 1. read through with-scope
            Value v = context.eval(Source.create("js", "with(pug4j__model){ a + b }"));
            System.out.println("PROBE result " + v);

            // 2. write through with-scope (existing key)
            context.eval(Source.create("js", "with(pug4j__model){ a = 100 }"));
            System.out.println("PROBE model.a after write = " + model.get("a"));

            // 3. unknown key falls through to global -> ReferenceError?
            try {
                context.eval(Source.create("js", "with(pug4j__model){ unknownVar }"));
            } catch (Exception e) {
                System.out.println("PROBE unknown -> " + e.getMessage());
            }

            // 4. builtins not shadowed
            Value json = context.eval(Source.create("js", "with(pug4j__model){ JSON.stringify({x: a}) }"));
            System.out.println("PROBE builtin " + json);

            // 5. parse/cache the with-wrapped source once, execute twice
            Value parsed = context.parse(Source.create("js", "with(pug4j__model){ a + b }"));
            System.out.println("PROBE cached exec 1 = " + parsed.execute());
            model.put("a", 1000);
            System.out.println("PROBE cached exec 2 = " + parsed.execute());
        }
    }
}
