package de.neuland.pug4j.expression;

import static de.neuland.pug4j.model.PugModel.PUG4J_MODEL_PREFIX;
import static org.graalvm.polyglot.HostAccess.newBuilder;

import de.neuland.pug4j.exceptions.ExpressionException;
import de.neuland.pug4j.model.PugModel;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.graalvm.polyglot.*;

public class GraalJsExpressionHandler extends AbstractExpressionHandler {
  final HostAccess all =
      newBuilder()
          .allowAllImplementations(true)
          .allowPublicAccess(true)
          .allowArrayAccess(true)
          .allowListAccess(true)
          .allowMapAccess(true)
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
          .targetTypeMapping(Value.class, Object.class, Value::hasMembers, (v) -> v.as(Map.class))
          .build();
  final Engine engine =
      Engine.newBuilder()
          .option("engine.WarnInterpreterOnly", "false")
          .allowExperimentalOptions(true)
          .build();
  final ThreadLocal<Map<String, Value>> cacheThreadLocal =
      ThreadLocal.withInitial(
          new Supplier<Map<String, Value>>() {

            @Override
            public Map<String, Value> get() {
              return new ConcurrentHashMap<String, Value>();
            }
          });
  final ThreadLocal<Context> contextThreadLocal =
      ThreadLocal.withInitial(
          new Supplier<Context>() {

            @Override
            public Context get() {
              Context context =
                  Context.newBuilder("js")
                      .engine(engine)
                      .allowHostAccess(all)
                      .allowAllAccess(true)
                      .allowHostClassLookup(s -> true)
                      .allowCreateThread(false)
                      .allowCreateProcess(false)
                      .allowPolyglotAccess(PolyglotAccess.ALL)
                      .build();
              context.initialize("js");
              return context;
            }
          });
  final ThreadLocal<ArrayDeque<Value>> resolverStack =
      ThreadLocal.withInitial(ArrayDeque::new);

  /**
   * Callback exposed to buffered JS code that contains a nested pug block. The generated code
   * passes a JS closure {@code function(pug4j__expr){ return eval(pug4j__expr); }} whose direct
   * eval resolves expressions in the lexical scope of the enclosing JS function. While the block
   * renders, that resolver is the innermost entry on {@link #resolverStack}, so nested expression
   * evaluations (e.g. {@code li= item} inside {@code forEach(function(item){...})}) see the
   * function-local variables.
   *
   * <p>Note: under {@code 'use strict'} reading lexical variables still works, but {@code var}
   * declarations made by nested {@code - var x = ...} lines do not persist between sibling
   * expressions.
   */
  public final class NestedBlockCallback {
    private final Runnable blockRenderer;
    private final PugModel model;

    NestedBlockCallback(Runnable blockRenderer, PugModel model) {
      this.blockRenderer = blockRenderer;
      this.model = model;
    }

    public void accept(Value resolver) {
      // Sync globals mutated by the outer JS code earlier in this iteration into the model, so
      // the nested model->bindings copy does not clobber them with stale values.
      Value bindings = contextThreadLocal.get().getBindings("js");
      writeBackBindingsToModel(bindings, model, false);
      ArrayDeque<Value> stack = resolverStack.get();
      stack.push(resolver);
      try {
        blockRenderer.run();
      } finally {
        stack.pop();
      }
    }
  }

  @Override
  public Object createBlockCallback(Runnable blockRenderer, PugModel model) {
    return new NestedBlockCallback(blockRenderer, model);
  }

  @Override
  public String getBlockInvocation(String callbackKey) {
    return callbackKey + ".accept(function(pug4j__expr){ return eval(pug4j__expr); });";
  }

  @Override
  public Boolean evaluateBooleanExpression(String expression, PugModel model)
      throws ExpressionException {
    return BooleanUtil.convert(evaluateExpression(expression, model));
  }

  @Override
  public Object evaluateExpression(String expression, PugModel model) throws ExpressionException {
    Context context = contextThreadLocal.get();
    Map<String, Value> cache = cacheThreadLocal.get();
    context.enter();
    try {
      saveLocalVariableName(expression, model);
      Value jsContextBindings = context.getBindings("js");
      for (Map.Entry<String, Object> objectEntry : model.entrySet()) {
        String key = objectEntry.getKey();
        if (!PugModel.LOCAL_VARS.equals(key)) {
          Object value = objectEntry.getValue();

          // If the value is a RecordWrapper, we need to convert it to a pure ProxyObject
          // because GraalJS treats objects implementing Map specially, ignoring their ProxyObject
          // implementation
          if (value instanceof de.neuland.pug4j.model.RecordWrapper wrapper) {
            value = wrapper.asGraalProxy();
          }

          jsContextBindings.putMember(key, value);
        }
      }

      Value eval;
      ArrayDeque<Value> stack = resolverStack.get();
      boolean nested = !stack.isEmpty();
      if (nested) {
        // Evaluate via direct eval in the lexical scope of the enclosing JS function, so
        // function-local variables resolve. Eval'd strings cannot be pre-parsed against a scope,
        // therefore the source cache is bypassed here.
        eval = stack.peek().execute(rewriteLeadingDeclaration(expression));
        writeBackBindingsToModel(jsContextBindings, model, false);
      } else {
        Value parsed = cache.get(expression);
        if (parsed == null) {
          Source js = Source.create("js", rewriteLeadingDeclaration(expression));
          parsed = context.parse(js);
          cache.put(expression, parsed);
        }
        eval = parsed.execute();
        writeBackBindingsToModel(jsContextBindings, model, true);
      }
      return eval.as(Object.class);
    } catch (PolyglotException ex) {
      // Exceptions thrown by the Java-side block renderer (e.g. PugCompilerException) must keep
      // their original location info instead of being wrapped as an ExpressionException of the
      // outer buffered string.
      if (ex.isHostException() && ex.asHostException() instanceof RuntimeException re) {
        throw re;
      }
      String msg = ex.getMessage();
      if (msg != null) {
        if (msg.startsWith("ReferenceError:")) {
          return null;
        }
        // Record components are exposed as properties, so method-call syntax like person.name()
        // fails with a TypeError. Retry with the offending zero-arg member calls rewritten to
        // property access.
        if (resolverStack.get().isEmpty() && isNotInvocable(msg)) {
          Object result = retryWithPropertyAccessRewrite(expression, context, cache, model);
          if (result != RETRY_FAILED) {
            return result;
          }
        }
      }
      throw new ExpressionException(expression, ex);
    } finally {
      context.leave();
    }
  }

  /**
   * Rewrites a leading var/let/const declaration into a plain assignment. Declarations become
   * configurable global-object properties, so the writeback can sync and remove them, and the
   * persistent thread-local context accumulates no global lexical bindings (re-evaluating a cached
   * {@code let x = ...} Source would otherwise throw "has already been declared" on the next
   * render; {@code const} bindings additionally are neither removable nor writable). Tradeoff:
   * {@code const} reassignment no longer errors. Non-declarations are returned unchanged, except
   * for the object-literal paren wrap.
   */
  static String rewriteLeadingDeclaration(String expression) {
    DeclarationScanner.Result declaration = DeclarationScanner.scan(expression);
    if (declaration == null) {
      return expression.startsWith("{") ? "(" + expression + ")" : expression;
    }
    StringBuilder statement = new StringBuilder();
    for (int i = 0; i < declaration.declarators.size(); i++) {
      DeclarationScanner.Declarator declarator = declaration.declarators.get(i);
      if (i > 0) {
        statement.append(", ");
      }
      statement.append(declarator.source);
      if (!declarator.destructuring && !declarator.hasInitializer) {
        statement.append(" = undefined");
      }
    }
    String statementString = statement.toString();
    if (statementString.startsWith("{")) {
      statementString = "(" + statementString + ")";
    }
    return statementString + expression.substring(declaration.boundary);
  }

  private void writeBackBindingsToModel(
      Value jsContextBindings, PugModel model, boolean removeFromBindings) {
    Set<String> memberKeys = jsContextBindings.getMemberKeys();
    for (String memberKey : memberKeys) {
      if (model.knowsKey(memberKey)) {
        if (!memberKey.startsWith(PUG4J_MODEL_PREFIX)) {
          Value member = jsContextBindings.getMember(memberKey);
          model.put(memberKey, member.as(Object.class));
          if (removeFromBindings) {
            try {
              jsContextBindings.removeMember(memberKey);
            } catch (UnsupportedOperationException e) {
              try {
                jsContextBindings.putMember(memberKey, null);
              } catch (UnsupportedOperationException e2) {
                // non-removable, non-writable binding (e.g. a const or function declared inside a
                // multi-statement expression): leave it in the context
              }
            }
          }
        }
      }
    }
  }

  private static final Pattern EMPTY_MEMBER_CALL =
      Pattern.compile("\\.([A-Za-z_$][A-Za-z0-9_$]*)\\(\\)");
  private static final Object RETRY_FAILED = new Object();

  /**
   * Matches the two TypeErrors GraalJS raises when a member is called but not callable: plain
   * values yield "... is not a function", ProxyObject members (e.g. record components) yield
   * "invokeMember (name) on ... failed due to: Message not supported".
   */
  private static boolean isNotInvocable(String message) {
    return message.startsWith("TypeError:")
        && (message.contains("is not a function")
            || message.contains("failed due to: Message not supported"));
  }

  /**
   * Retries a failed evaluation with zero-arg member calls progressively rewritten to property
   * access, leftmost first ({@code person.name()} becomes {@code person.name}). A TypeError occurs
   * at the first member that is not callable, so rewriting left to right converges on the working
   * variant without touching real method calls further right (e.g. {@code
   * person.name().toUpperCase()} only needs the first rewrite). On success the parsed rewrite is
   * cached under the original expression, so subsequent renders skip the failing evaluation
   * entirely.
   */
  private Object retryWithPropertyAccessRewrite(
      String expression, Context context, Map<String, Value> cache, PugModel model) {
    String rewritten = rewriteLeadingDeclaration(expression);
    Matcher matcher = EMPTY_MEMBER_CALL.matcher(rewritten);
    while (matcher.find()) {
      rewritten =
          rewritten.substring(0, matcher.start())
              + "."
              + matcher.group(1)
              + rewritten.substring(matcher.end());
      try {
        Value parsed = context.parse(Source.create("js", rewritten));
        Value eval = parsed.execute();
        cache.put(expression, parsed);
        writeBackBindingsToModel(context.getBindings("js"), model, true);
        return eval.as(Object.class);
      } catch (PolyglotException retryEx) {
        if (retryEx.isHostException() && retryEx.asHostException() instanceof RuntimeException re) {
          throw re;
        }
        String retryMsg = retryEx.getMessage();
        if (retryMsg == null || !isNotInvocable(retryMsg)) {
          return RETRY_FAILED;
        }
      }
      matcher = EMPTY_MEMBER_CALL.matcher(rewritten);
    }
    return RETRY_FAILED;
  }

  @Override
  public String evaluateStringExpression(String expression, PugModel model)
      throws ExpressionException {
    Object result = evaluateExpression(expression, model);
    return result == null ? "" : result.toString();
  }

  @Override
  public void assertExpression(String expression) throws ExpressionException {
    Context context = contextThreadLocal.get();
    Source js;
    if (expression.startsWith("{")) {
      js = Source.create("js", "(" + expression + ")");
    } else {
      js = Source.create("js", expression);
    }
    try {
      Value parse = context.eval(js);
    } catch (PolyglotException e) {
      if (e.getMessage().startsWith("SyntaxError:")) {
        throw new ExpressionException(e.getMessage());
      }
    }
  }

  @Override
  public void setCache(boolean cache) {}

  @Override
  public void clearCache() {}

  public Context getContext() {
    return contextThreadLocal.get();
  }
}
