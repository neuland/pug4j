package de.neuland.pug4j.expression;

import de.neuland.pug4j.exceptions.ExpressionException;
import de.neuland.pug4j.model.PugModel;
import org.graalvm.polyglot.*;
import org.graalvm.polyglot.proxy.ProxyArray;
import org.graalvm.polyglot.proxy.ProxyObject;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import static de.neuland.pug4j.model.PugModel.PUG4J_MODEL_PREFIX;
import static org.graalvm.polyglot.HostAccess.newBuilder;

public class GraalJsExpressionHandler extends AbstractExpressionHandler {
    JexlExpressionHandler jexlExpressionHandler = new JexlExpressionHandler();
    final HostAccess all = newBuilder().allowPublicAccess(true).allowArrayAccess(true).allowListAccess(true).allowMapAccess(true)
            .targetTypeMapping(Integer.class, Object.class, null, (v) -> v)
            .targetTypeMapping(Long.class, Object.class, null, (v) -> v)
            .targetTypeMapping(Float.class, Object.class, null, (v) -> v)
            .targetTypeMapping(Double.class, Object.class, null, (v) -> v)
            .targetTypeMapping(Boolean.class, Object.class, null, (v) -> v)
            .targetTypeMapping(String.class, Object.class, null, (v) -> v)
            .targetTypeMapping(List.class, Object.class, null, (v) -> v)
            .targetTypeMapping(Value.class, Object.class, Value::isInstant, Value::asInstant)
            .targetTypeMapping(Value.class, Object.class, Value::isTime, Value::asTime)
            .targetTypeMapping(Value.class, Object.class, Value::isTimeZone, Value::asTimeZone)
            .targetTypeMapping(Value.class, Object.class, Value::isHostObject, Value::asHostObject)
            .targetTypeMapping(Value.class, Object.class, Value::isMetaObject, (v) -> v)
            .targetTypeMapping(Value.class, Object.class, Value::hasMembers, (v) -> new LinkedHashMap(v.as(Map.class)))
            .build();
    final Engine engine = Engine.newBuilder().option("engine.WarnInterpreterOnly", "false").allowExperimentalOptions(true).build();
    final ThreadLocal<Map<String,Value>> cacheThreadLocal = ThreadLocal.withInitial(new Supplier<Map<String,Value>>() {

        @Override
        public Map<String,Value> get() {
            return new ConcurrentHashMap<String,Value>();
        }
    });
    final ThreadLocal<Context> contextThreadLocal = ThreadLocal.withInitial(new Supplier<Context>() {

        @Override
        public Context get() {
            Context context = Context.newBuilder("js")
                    .engine(engine)
                    .allowHostAccess(all)
                    .allowAllAccess(true)
                    .allowHostClassLookup(s -> true)
                    .allowPolyglotAccess(PolyglotAccess.ALL)
                    .build();
            context.initialize("js");
            return context;
        }
    });

    @Override
    public Boolean evaluateBooleanExpression(String expression, PugModel model) throws ExpressionException {
        return BooleanUtil.convert(evaluateExpression(expression, model));
    }

    @Override
    public Object evaluateExpression(String expression, PugModel model) throws ExpressionException {
        Context context = contextThreadLocal.get();
        Map<String,Value> cache = cacheThreadLocal.get();
        context.enter();
        try{
            saveLocalVariableName(expression, model);
            Value jsContextBindings = context.getBindings("js");
            for (Map.Entry<String, Object> objectEntry : model.entrySet()) {
                String key = objectEntry.getKey();
                if(!PugModel.LOCAL_VARS.equals(key)) {
                    Object value = jsValue(objectEntry);
                    jsContextBindings.putMember(key, value);
                }
            }

            Source js;
            Value eval = cache.get(expression);
            if(eval==null){
                if(expression.startsWith("{")){
                    js = Source.create("js", "(" + expression + ")");
                }else{
                    js = Source.create("js", expression);
                }
                eval = context.parse(js);

                cache.put(expression,eval);
            }
            eval = eval.execute();
            Set<String> memberKeys = jsContextBindings.getMemberKeys();
            for (String memberKey : memberKeys) {
                if (model.knowsKey(memberKey)){
                    if (!memberKey.startsWith(PUG4J_MODEL_PREFIX)) {
                        Value member = jsContextBindings.getMember(memberKey);
                        model.put(memberKey, javaValue(member));
                        jsContextBindings.putMember(memberKey, null);
                    }
                }
            }
            return javaValue(eval);
        }
        catch (Exception ex){
            if(ex.getMessage().startsWith("ReferenceError:")){
                return null;
            }
            throw new ExpressionException(expression, ex);
        }finally {
            context.leave();
        }
    }

    private Object jsValue(Map.Entry<String, Object> objectEntry) {
        Object value = objectEntry.getValue();
        if(!objectEntry.getKey().startsWith(PUG4J_MODEL_PREFIX) && !(objectEntry.getValue() instanceof PugModel)){
            if(value instanceof Map)
                value = ProxyObject.fromMap((Map)value);
            if(value instanceof List)
                value = ProxyArray.fromList((List)value);
        }
        return value;
    }

    private Object javaValue(Value eval) {
        return eval.as(Object.class);
    }

    @Override
    public String evaluateStringExpression(String expression, PugModel model) throws ExpressionException {
        Object result = evaluateExpression(expression, model);
        return result == null ? "" : result.toString();
    }

    @Override
    public void assertExpression(String expression) throws ExpressionException {
        jexlExpressionHandler.assertExpression(expression);
    }

    @Override
    public void setCache(boolean cache) {

    }

    @Override
    public void clearCache() {

    }

}

