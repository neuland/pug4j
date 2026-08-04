package de.neuland.pug4j.jexl3;

import de.neuland.pug4j.model.RecordWrapper;
import java.lang.reflect.InaccessibleObjectException;
import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.util.List;
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.JexlException;
import org.apache.commons.jexl3.internal.introspection.Uberspect;
import org.apache.commons.jexl3.introspection.JexlMethod;
import org.apache.commons.jexl3.introspection.JexlPermissions;
import org.apache.commons.jexl3.introspection.JexlPropertyGet;
import org.apache.commons.jexl3.introspection.JexlUberspect;
import org.apache.commons.logging.Log;

/**
 * Custom Uberspect for Java records. Property-style access ({@code person.name}) resolves to the
 * record's component accessor, so records work like the JavaBeans and Maps that pug templates are
 * usually written against. Method calls on records need no help - records are plain objects to
 * JEXL. The RecordWrapper delegation in {@link #getMethod} remains for callers that put wrapper
 * instances into the model themselves.
 */
public class RecordWrapperUberspect extends Uberspect {

  public RecordWrapperUberspect(
      Log log, JexlUberspect.ResolverStrategy strategy, JexlPermissions permissions) {
    super(log, strategy, permissions);
  }

  @Override
  public JexlPropertyGet getPropertyGet(
      List<PropertyResolver> resolvers, Object obj, Object identifier) {
    JexlPropertyGet propertyGet = super.getPropertyGet(resolvers, obj, identifier);
    if (propertyGet == null && obj != null && obj.getClass().isRecord() && identifier != null) {
      return getRecordComponentGet(obj.getClass(), identifier.toString());
    }
    return propertyGet;
  }

  private static JexlPropertyGet getRecordComponentGet(Class<?> recordClass, String name) {
    for (RecordComponent component : recordClass.getRecordComponents()) {
      if (component.getName().equals(name)) {
        Method accessor = component.getAccessor();
        try {
          accessor.setAccessible(true);
        } catch (InaccessibleObjectException e) {
          // Module encapsulation forbids access - leave the property unresolved instead of
          // failing the whole render
          return null;
        }
        return new RecordComponentGet(accessor, name);
      }
    }
    return null;
  }

  /**
   * Property getter backed by a record component accessor. JEXL caches resolved property getters
   * on the parsed script's AST nodes, so tryInvoke must take the receiver from its arguments and
   * signal TRY_FAILED on a mismatch - never rely on state captured at resolution time.
   */
  private static class RecordComponentGet implements JexlPropertyGet {
    private final Method accessor;
    private final String name;

    RecordComponentGet(Method accessor, String name) {
      this.accessor = accessor;
      this.name = name;
    }

    @Override
    public Object invoke(Object obj) throws Exception {
      return accessor.invoke(obj);
    }

    @Override
    public Object tryInvoke(Object obj, Object key) {
      if (obj == null || !accessor.getDeclaringClass().isInstance(obj) || !name.equals(key)) {
        return JexlEngine.TRY_FAILED;
      }
      try {
        return accessor.invoke(obj);
      } catch (Exception xany) {
        return JexlEngine.TRY_FAILED;
      }
    }

    @Override
    public boolean tryFailed(Object rval) {
      return rval == JexlEngine.TRY_FAILED;
    }

    @Override
    public boolean isCacheable() {
      return true;
    }
  }

  @Override
  public JexlMethod getMethod(Object obj, String method, Object... args) {
    // For RecordWrapper, delegate method resolution to the underlying record
    if (obj instanceof RecordWrapper wrapper) {
      Object record = wrapper.getRecord();
      Class<?> recordClass = record.getClass();

      // Try to find the method using Java reflection directly
      // Record accessors are no-arg methods with the component name
      if (recordClass.isRecord() && (args == null || args.length == 0)) {
        try {
          Method javaMethod = recordClass.getMethod(method);
          // Found a matching method, create a JexlMethod wrapper for it
          return new DirectRecordMethod(javaMethod);
        } catch (NoSuchMethodException | InaccessibleObjectException e) {
          // Method not found or blocked by module encapsulation, fall through to default handling
        }
      }

      // If not a record or method not found, try standard JEXL resolution
      JexlMethod recordMethod = super.getMethod(record, method, args);
      if (recordMethod != null) {
        return new RecordDelegatingMethod(recordMethod);
      }
    }

    // For all other objects, use default behavior
    JexlMethod resolved = super.getMethod(obj, method, args);
    // Records themselves are often not public (e.g. nested in a class), which defeats JEXL's
    // introspection; fall back to direct reflection with setAccessible for no-arg methods
    if (resolved == null
        && obj != null
        && obj.getClass().isRecord()
        && (args == null || args.length == 0)) {
      try {
        return new DirectRecordMethod(obj.getClass().getMethod(method));
      } catch (NoSuchMethodException | InaccessibleObjectException e) {
        return null;
      }
    }
    return resolved;
  }

  /**
   * Returns the underlying record for a RecordWrapper receiver, any other receiver unchanged. JEXL
   * caches resolved JexlMethods on the parsed script's AST nodes and reuses them across
   * evaluations, so the receiver must always come from the invocation arguments — never from state
   * captured at resolution time.
   */
  private static Object unwrapReceiver(Object obj) {
    return obj instanceof RecordWrapper wrapper ? wrapper.getRecord() : obj;
  }

  /** Direct wrapper around a Java Method for record accessors. */
  private static class DirectRecordMethod implements JexlMethod {
    private final Method method;

    DirectRecordMethod(Method method) {
      this.method = method;
      method.setAccessible(true);
    }

    @Override
    public Object invoke(Object obj, Object... params) throws Exception {
      // Invoke on the underlying record, not the wrapper
      return method.invoke(unwrapReceiver(obj), params);
    }

    @Override
    public Object tryInvoke(String name, Object obj, Object... params) {
      Object target = unwrapReceiver(obj);
      if (!method.getName().equals(name)
          || target == null
          || !method.getDeclaringClass().isInstance(target)
          || (params != null && params.length != 0)) {
        return JexlEngine.TRY_FAILED;
      }
      try {
        return method.invoke(target);
      } catch (JexlException.TryFailed xjexl) {
        throw xjexl;
      } catch (Exception xany) {
        return JexlEngine.TRY_FAILED;
      }
    }

    @Override
    public boolean tryFailed(Object rval) {
      return rval == JexlEngine.TRY_FAILED;
    }

    @Override
    public boolean isCacheable() {
      return true;
    }

    @Override
    public Class<?> getReturnType() {
      return method.getReturnType();
    }
  }

  /** Wraps a JexlMethod to invoke it on the underlying record instead of the RecordWrapper. */
  private static class RecordDelegatingMethod implements JexlMethod {
    private final JexlMethod delegate;

    RecordDelegatingMethod(JexlMethod delegate) {
      this.delegate = delegate;
    }

    @Override
    public Object invoke(Object obj, Object... params) throws Exception {
      // Invoke on the underlying record, not the wrapper
      return delegate.invoke(unwrapReceiver(obj), params);
    }

    @Override
    public Object tryInvoke(String name, Object obj, Object... params) {
      return delegate.tryInvoke(name, unwrapReceiver(obj), params);
    }

    @Override
    public boolean tryFailed(Object rval) {
      return rval == JexlEngine.TRY_FAILED;
    }

    @Override
    public boolean isCacheable() {
      return delegate.isCacheable();
    }

    @Override
    public Class<?> getReturnType() {
      return delegate.getReturnType();
    }
  }
}
