package de.neuland.pug4j.expression;

import de.neuland.pug4j.exceptions.ExpressionException;
import de.neuland.pug4j.model.PugModel;
import org.graalvm.polyglot.*;
import org.graalvm.polyglot.proxy.ProxyArray;
import org.graalvm.polyglot.proxy.ProxyObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static de.neuland.pug4j.model.PugModel.PUG4J_MODEL_PREFIX;
import static org.graalvm.polyglot.HostAccess.newBuilder;

public class GraalJsExpressionHandler extends AbstractExpressionHandler {
    JexlExpressionHandler jexlExpressionHandler = new JexlExpressionHandler();
    private final Context jsContext;
    private Map<String,Value> cache = new ConcurrentHashMap();

    public GraalJsExpressionHandler() {
        jsContext = createContext();
    }

    private Context createContext() {
        HostAccess all = newBuilder().allowPublicAccess(true).allowAllImplementations(true).allowArrayAccess(true).allowListAccess(true).build();
        return Context.newBuilder("js").allowHostAccess(all).allowAllAccess(true).allowExperimentalOptions(true)
                .allowHostClassLookup(s -> true).allowPolyglotAccess(PolyglotAccess.ALL).option("engine.WarnInterpreterOnly","false").build();
    }


    @Override
    public Boolean evaluateBooleanExpression(String expression, PugModel model) throws ExpressionException {
        return BooleanUtil.convert(evaluateExpression(expression, model));
    }

    @Override
    public Object evaluateExpression(String expression, PugModel model) throws ExpressionException {
        try{
            saveLocalVariableName(expression, model);
            Value jsContextBindings = jsContext.getBindings("js");
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
                eval = jsContext.parse(js);
                cache.put(expression,eval);
                eval = eval.execute();
            }
            Set<String> memberKeys = jsContextBindings.getMemberKeys();
            for (String memberKey : memberKeys) {
                Value member = jsContextBindings.getMember(memberKey);
                if (model.knowsKey(memberKey)){
                    if (!memberKey.startsWith(PUG4J_MODEL_PREFIX)) {
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
        }
    }

    private Object jsValue(Map.Entry<String, Object> objectEntry) {
        Object value = objectEntry.getValue();
        if(!objectEntry.getKey().startsWith(PUG4J_MODEL_PREFIX)){
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
        if(eval.hasArrayElements()) {
            return eval.as(List.class);
        }
        if(eval.isNumber()  && eval.fitsInInt() && String.valueOf(eval.asInt()).equals(eval.toString())){
            return eval.asInt();
        }
        if(eval.isNumber()  && eval.fitsInLong() && String.valueOf(eval.asLong()).equals(eval.toString())){
            return eval.asLong();
        }
        if(eval.fitsInDouble()){
            return eval.asDouble();
        }
        if(eval.fitsInInt()){
            return eval.asInt();
        }
        if(eval.isHostObject()){
            return eval.asHostObject();
        }
        if(eval.isMetaObject()){
            return eval;
        }
        if(eval.hasMembers()){
            return eval.as(Map.class);
        }
        if(eval.fitsInDouble() && !eval.fitsInInt()){
            return eval.asDouble();
        }
        if(eval.isString()){
            return eval.asString();
        }
        if(eval.isBoolean()){
            return eval.asBoolean();
        }

        return eval;
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

