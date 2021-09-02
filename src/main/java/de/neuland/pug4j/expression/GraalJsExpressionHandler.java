package de.neuland.pug4j.expression;

import de.neuland.pug4j.exceptions.ExpressionException;
import de.neuland.pug4j.model.PugModel;
import org.graalvm.polyglot.*;
import org.graalvm.polyglot.proxy.ProxyArray;
import org.graalvm.polyglot.proxy.ProxyObject;

import java.util.HashMap;
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
    final HostAccess all = newBuilder().allowPublicAccess(true).allowAllImplementations(true).allowArrayAccess(true).allowListAccess(true).build();
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

            Value eval;
            Source js;
            if(expression.startsWith("{")){
                 js = Source.create("js", "(" + expression + ")");
            }else{
                 js = Source.create("js", expression);
            }
            Value value = cache.get(expression);
            if(value!=null)
                eval = value.execute();
            else{
                eval = context.parse(js);
                cache.put(expression,eval);
                eval = eval.execute();
            }
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
        if(eval.isNull()) {
            return null;
        }
        if(eval.isBoolean()){
            return eval.asBoolean();
        }
        if(eval.isString()){
            return eval.asString();
        }
        if(eval.isNumber()) {
            if (eval.fitsInByte()) {
                return eval.asByte();
            }
            if (eval.fitsInShort()) {
                return eval.asShort();
            }
            if (eval.fitsInInt()) {
                return eval.asInt();
            }
            if (eval.fitsInLong()) {
                return eval.asLong();
            }
            if (eval.fitsInFloat()) {
                return eval.asFloat();
            }
            if (eval.fitsInDouble()) {
                return eval.asDouble();
            }
        }
        if(eval.isInstant()){
            return eval.asInstant();
        }
        if(eval.isDate()){
            return eval.asDate();
        }
        if(eval.isDuration()){
            return eval.asDuration();
        }
        if(eval.isTime()){
            return eval.asTime();
        }
        if(eval.isTimeZone()){
            return eval.asTimeZone();
        }
        if(eval.isHostObject()){
            return eval.asHostObject();
        }
        if(eval.isMetaObject()){
            return eval;
        }
        if(eval.hasArrayElements()) {
            return eval.as(List.class);
        }
        if(eval.hasMembers()){
            return new LinkedHashMap<>(eval.as(Map.class)); //If not copied to a LinkedHashMap it will result in a PolyglotMap in the Model which will drastically decrease performance in some special cases.
        }
        return null;
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

