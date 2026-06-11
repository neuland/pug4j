package de.neuland.pug4j.expression;

import static de.neuland.pug4j.model.PugModel.PUG4J_MODEL_PREFIX;
import static org.graalvm.polyglot.HostAccess.newBuilder;

import de.neuland.pug4j.exceptions.ExpressionException;
import de.neuland.pug4j.model.PugModel;
import de.neuland.pug4j.model.RecordWrapper;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.graalvm.polyglot.*;
import org.graalvm.polyglot.proxy.ProxyObject;

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
  final ThreadLocal<PugModelProxy> modelProxyThreadLocal =
      ThreadLocal.withInitial(PugModelProxy::new);
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
              context.getBindings("js").putMember(MODEL_SCOPE_BINDING, modelProxyThreadLocal.get());
              return context;
            }
          });
  final ThreadLocal<ArrayDeque<Value>> resolverStack =
      ThreadLocal.withInitial(ArrayDeque::new);

  static final String MODEL_SCOPE_BINDING = PUG4J_MODEL_PREFIX + "scope";

  /**
   * Bound once per context as the {@code with}-scope for every expression. Delegates variable
   * resolution directly to the current PugModel, so reads cost one proxy call per referenced
   * variable and writes hit the model immediately — replacing the former per-expression copy of
   * the whole model into the global bindings and the write-back afterwards. The model reference is
   * swapped per evaluation (and restored afterwards) because nested block rendering evaluates
   * against nested model scopes mid-execution.
   */
  static final class PugModelProxy implements ProxyObject {
    private PugModel model;

    PugModel swap(PugModel newModel) {
      PugModel previous = this.model;
      this.model = newModel;
      return previous;
    }

    @Override
    public Object getMember(String key) {
      Object value = model.get(key);
      // GraalJS treats objects implementing Map specially, ignoring their ProxyObject
      // implementation, so RecordWrapper needs its pure-proxy view
      if (value instanceof RecordWrapper wrapper) {
        return wrapper.asGraalProxy();
      }
      return value;
    }

    @Override
    public Object getMemberKeys() {
      List<String> keys = new ArrayList<>();
      for (String key : model.keySet()) {
        if (!PugModel.LOCAL_VARS.equals(key)) {
          keys.add(key);
        }
      }
      return keys.toArray();
    }

    @Override
    public boolean hasMember(String key) {
      // knowsKey includes declared-but-unset locals registered by saveLocalVariableName, so
      // assignments from rewritten declarations resolve to the model instead of the JS global
      return !PugModel.LOCAL_VARS.equals(key) && model.knowsKey(key);
    }

    @Override
    public void putMember(String key, Value value) {
      model.put(key, value == null ? null : value.as(Object.class));
    }
  }

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

    NestedBlockCallback(Runnable blockRenderer) {
      this.blockRenderer = blockRenderer;
    }

    public void accept(Value resolver) {
      // Mutations of model variables by the outer JS code already reached the model live through
      // the with-scope proxy, so no bindings sync is needed before rendering the nested block.
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
    return new NestedBlockCallback(blockRenderer);
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
    PugModelProxy modelProxy = modelProxyThreadLocal.get();
    context.enter();
    PugModel previousModel = modelProxy.swap(model);
    try {
      saveLocalVariableName(expression, model);

      Value eval;
      ArrayDeque<Value> stack = resolverStack.get();
      boolean nested = !stack.isEmpty();
      if (nested) {
        // Evaluate via direct eval in the lexical scope of the enclosing JS function, so
        // function-local variables resolve. The enclosing buffered code already runs inside the
        // model with-scope, so the eval'd string sees model variables through the scope chain.
        // Eval'd strings cannot be pre-parsed against a scope, so the source cache is bypassed.
        eval = stack.peek().execute(rewriteLeadingDeclaration(expression));
      } else {
        Value parsed = cache.get(expression);
        if (parsed == null) {
          Source js = Source.create("js", wrapInModelScope(rewriteLeadingDeclaration(expression)));
          parsed = context.parse(js);
          cache.put(expression, parsed);
        }
        eval = parsed.execute();
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
          Object result = retryWithPropertyAccessRewrite(expression, context, cache);
          if (result != RETRY_FAILED) {
            return result;
          }
        }
      }
      throw new ExpressionException(expression, ex);
    } finally {
      modelProxy.swap(previousModel);
      context.leave();
    }
  }

  /**
   * Wraps an expression in the model with-scope, so identifier resolution goes through {@link
   * PugModelProxy}. The trailing newline guards against expressions ending in a line comment. The
   * statement completion value of the with-body is the evaluation result, matching the previous
   * bare-program semantics. Note: {@code with} is illegal in strict-mode code; expressions
   * declaring {@code 'use strict'} at program level are not supported (function-level strict mode
   * is fine).
   */
  private static String wrapInModelScope(String expression) {
    return "with(" + MODEL_SCOPE_BINDING + "){ " + expression + "\n}";
  }

  /**
   * Rewrites a leading var/let/const declaration into a plain assignment. The assignment then
   * resolves through the model with-scope (saveLocalVariableName registers the name in the model
   * first), so declared values land in the PugModel instead of the persistent thread-local
   * context, which would otherwise accumulate global lexical bindings (re-evaluating a cached
   * {@code let x = ...} Source would throw "has already been declared" on the next render;
   * {@code const} bindings additionally are neither removable nor writable). Tradeoff: {@code
   * const} reassignment no longer errors. Non-declarations are returned unchanged, except for the
   * object-literal paren wrap.
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
      String expression, Context context, Map<String, Value> cache) {
    String rewritten = rewriteLeadingDeclaration(expression);
    Matcher matcher = EMPTY_MEMBER_CALL.matcher(rewritten);
    while (matcher.find()) {
      rewritten =
          rewritten.substring(0, matcher.start())
              + "."
              + matcher.group(1)
              + rewritten.substring(matcher.end());
      try {
        Value parsed = context.parse(Source.create("js", wrapInModelScope(rewritten)));
        Value eval = parsed.execute();
        cache.put(expression, parsed);
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
