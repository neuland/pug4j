package de.neuland.pug4j.jexl3;

import de.neuland.pug4j.model.RecordWrapper;
import java.lang.reflect.Method;
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.JexlException;
import org.apache.commons.jexl3.internal.introspection.Uberspect;
import org.apache.commons.jexl3.introspection.JexlMethod;
import org.apache.commons.jexl3.introspection.JexlPermissions;
import org.apache.commons.jexl3.introspection.JexlUberspect;
import org.apache.commons.logging.Log;

/**
 * Custom Uberspect that enables method calls on RecordWrapper by delegating to the underlying
 * record.
 */
public class RecordWrapperUberspect extends Uberspect {

  public RecordWrapperUberspect(
      Log log, JexlUberspect.ResolverStrategy strategy, JexlPermissions permissions) {
    super(log, strategy, permissions);
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
        } catch (NoSuchMethodException e) {
          // Method not found, fall through to default handling
        }
      }

      // If not a record or method not found, try standard JEXL resolution
      JexlMethod recordMethod = super.getMethod(record, method, args);
      if (recordMethod != null) {
        return new RecordDelegatingMethod(recordMethod);
      }
    }

    // For all other objects, use default behavior
    return super.getMethod(obj, method, args);
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
      Object result = method.invoke(unwrapReceiver(obj), params);
      // Wrap the result if it's a record
      return RecordWrapper.wrapIfRecord(result);
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
        return RecordWrapper.wrapIfRecord(method.invoke(target));
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
      Object result = delegate.invoke(unwrapReceiver(obj), params);
      // Wrap the result if it's a record
      return RecordWrapper.wrapIfRecord(result);
    }

    @Override
    public Object tryInvoke(String name, Object obj, Object... params) {
      Object result = delegate.tryInvoke(name, unwrapReceiver(obj), params);
      if (delegate.tryFailed(result)) {
        return JexlEngine.TRY_FAILED;
      }
      return RecordWrapper.wrapIfRecord(result);
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
