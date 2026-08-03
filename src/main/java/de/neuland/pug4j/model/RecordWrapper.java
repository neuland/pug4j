package de.neuland.pug4j.model;

import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;

/**
 * A wrapper for Java records that makes them behave like Maps for template expression evaluation.
 * This allows record components to be accessed using dot notation (e.g., person.name) in templates,
 * automatically mapping to the record's accessor methods (e.g., person.name()).
 *
 * <p>Nested records are automatically wrapped recursively, so person.address.city works seamlessly.
 *
 * <p>Also implements GraalVM's ProxyObject to support method call syntax (e.g., person.name()) when
 * using the GraalJS expression handler.
 */
public class RecordWrapper implements Map<String, Object>, ProxyObject {

  private static final Log logger = LogFactory.getLog(RecordWrapper.class);

  /**
   * Component accessors per record class, in declaration order. Cached via ClassValue so wrapper
   * construction costs a single allocation - components are read lazily, one reflective call per
   * actually accessed component. ClassValue keeps the cache GC-friendly with respect to class
   * unloading.
   */
  private static final ClassValue<LinkedHashMap<String, Method>> COMPONENT_ACCESSORS =
      new ClassValue<>() {
        @Override
        protected LinkedHashMap<String, Method> computeValue(Class<?> type) {
          LinkedHashMap<String, Method> accessors = new LinkedHashMap<>();
          for (RecordComponent component : type.getRecordComponents()) {
            Method accessor = component.getAccessor();
            accessor.setAccessible(
                true); // Required for accessing records in different packages/modules
            accessors.put(component.getName(), accessor);
          }
          return accessors;
        }
      };

  private final Object record;
  private final Map<String, Method> componentAccessors;

  /**
   * Creates a wrapper for the given record instance.
   *
   * @param record the record instance to wrap
   * @throws IllegalArgumentException if the object is not a record
   */
  public RecordWrapper(Object record) {
    if (record == null || !record.getClass().isRecord()) {
      throw new IllegalArgumentException("Object must be a record");
    }
    this.record = record;
    this.componentAccessors = COMPONENT_ACCESSORS.get(record.getClass());
  }

  /** Reads a single component value, wrapping nested records. */
  private Object readComponent(Method accessor) {
    try {
      Object value = accessor.invoke(record);
      // Recursively wrap nested records
      if (value != null && value.getClass().isRecord()) {
        value = new RecordWrapper(value);
      }
      return value;
    } catch (IllegalAccessException e) {
      // This can happen if the record accessor is not accessible due to module restrictions
      logger.warn(
          "Cannot access record component '"
              + accessor.getName()
              + "' on record type "
              + record.getClass().getName()
              + ": "
              + e.getMessage());
      return null;
    } catch (Exception e) {
      // Unexpected error - log as error since this shouldn't happen with valid records
      logger.error(
          "Unexpected error accessing record component '"
              + accessor.getName()
              + "' on record type "
              + record.getClass().getName(),
          e);
      return null;
    }
  }

  /**
   * Wraps an object if it's a record, otherwise returns it unchanged.
   *
   * @param obj the object to potentially wrap
   * @return the wrapped record or the original object
   */
  public static Object wrapIfRecord(Object obj) {
    if (obj != null && obj.getClass().isRecord()) {
      return new RecordWrapper(obj);
    }
    return obj;
  }

  // Map interface implementation

  @Override
  public int size() {
    return componentAccessors.size();
  }

  @Override
  public boolean isEmpty() {
    return componentAccessors.isEmpty();
  }

  @Override
  public boolean containsKey(Object key) {
    return componentAccessors.containsKey(key);
  }

  @Override
  public boolean containsValue(Object value) {
    return values().contains(value);
  }

  @Override
  public Object get(Object key) {
    Method accessor = componentAccessors.get(key);
    return accessor == null ? null : readComponent(accessor);
  }

  @Override
  public Object put(String key, Object value) {
    throw new UnsupportedOperationException("RecordWrapper is immutable");
  }

  @Override
  public Object remove(Object key) {
    throw new UnsupportedOperationException("RecordWrapper is immutable");
  }

  @Override
  public void putAll(Map<? extends String, ?> m) {
    throw new UnsupportedOperationException("RecordWrapper is immutable");
  }

  @Override
  public void clear() {
    throw new UnsupportedOperationException("RecordWrapper is immutable");
  }

  @Override
  public Set<String> keySet() {
    return componentAccessors.keySet();
  }

  @Override
  public Collection<Object> values() {
    List<Object> values = new ArrayList<>(componentAccessors.size());
    for (Method accessor : componentAccessors.values()) {
      values.add(readComponent(accessor));
    }
    return values;
  }

  @Override
  public Set<Entry<String, Object>> entrySet() {
    Map<String, Object> snapshot = new LinkedHashMap<>();
    for (Entry<String, Method> entry : componentAccessors.entrySet()) {
      snapshot.put(entry.getKey(), readComponent(entry.getValue()));
    }
    return snapshot.entrySet();
  }

  /**
   * Returns the original wrapped record instance. This can be useful for expression handlers that
   * need direct method access.
   *
   * @return the original record
   */
  public Object getRecord() {
    return record;
  }

  /**
   * Returns a GraalJS-compatible proxy that only implements ProxyObject (not Map). This is needed
   * because GraalJS treats objects implementing Map specially, ignoring their ProxyObject
   * implementation.
   *
   * @return a ProxyObject that delegates to this RecordWrapper
   */
  public ProxyObject asGraalProxy() {
    return new GraalJsRecordProxy(this);
  }

  /**
   * A pure ProxyObject implementation that delegates to RecordWrapper. This doesn't implement Map,
   * so GraalJS will use the ProxyObject methods.
   */
  private static class GraalJsRecordProxy implements ProxyObject {
    private final RecordWrapper wrapper;

    GraalJsRecordProxy(RecordWrapper wrapper) {
      this.wrapper = wrapper;
    }

    @Override
    public Object getMember(String key) {
      return wrapper.getMember(key);
    }

    @Override
    public Object getMemberKeys() {
      return wrapper.getMemberKeys();
    }

    @Override
    public boolean hasMember(String key) {
      return wrapper.hasMember(key);
    }

    @Override
    public void putMember(String key, Value value) {
      wrapper.putMember(key, value);
    }

    @Override
    public String toString() {
      return wrapper.toString();
    }
  }

  // ProxyObject implementation for GraalVM support

  @Override
  public Object getMember(String key) {
    // First, check if this is a record component - return its value directly
    // This ensures record components are accessed as properties (person.name)
    // rather than methods (person.name())
    Method accessor = componentAccessors.get(key);
    if (accessor != null) {
      return readComponent(accessor);
    }

    // If not a component, check for custom methods on the record
    // This allows calling custom methods like person.customMethod()
    // Since component accessors were already checked above, we won't
    // accidentally expose them as callable functions
    Method method = findNoArgMethod(key);
    if (method == null) {
      return null;
    }
    // Return a ProxyExecutable that invokes the method when called as a function
    return (ProxyExecutable)
        args -> {
          try {
            Object result = method.invoke(record);
            // Wrap record results to support nested records
            return wrapIfRecord(result);
          } catch (Exception e) {
            throw new RuntimeException(
                "Failed to invoke method '"
                    + key
                    + "' on record "
                    + record.getClass().getSimpleName(),
                e);
          }
        };
  }

  @Override
  public Object getMemberKeys() {
    return componentAccessors.keySet().toArray();
  }

  @Override
  public boolean hasMember(String key) {
    // Component value or a no-arg method with this name
    return componentAccessors.containsKey(key) || findNoArgMethod(key) != null;
  }

  /**
   * Reflective no-arg method lookups are cached per record class; ClassValue keeps the cache
   * GC-friendly with respect to class unloading. Misses are cached too, since hasMember probes
   * arbitrary keys.
   */
  private static final ClassValue<Map<String, Optional<Method>>> NO_ARG_METHODS =
      new ClassValue<>() {
        @Override
        protected Map<String, Optional<Method>> computeValue(Class<?> type) {
          return new ConcurrentHashMap<>();
        }
      };

  private Method findNoArgMethod(String key) {
    Class<?> recordClass = record.getClass();
    return NO_ARG_METHODS
        .get(recordClass)
        .computeIfAbsent(
            key,
            k -> {
              try {
                Method method = recordClass.getMethod(k);
                method.setAccessible(true);
                return Optional.of(method);
              } catch (NoSuchMethodException e) {
                return Optional.empty();
              }
            })
        .orElse(null);
  }

  @Override
  public void putMember(String key, Value value) {
    throw new UnsupportedOperationException("RecordWrapper is immutable");
  }

  @Override
  public String toString() {
    return record.toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o instanceof RecordWrapper) {
      return record.equals(((RecordWrapper) o).record);
    }
    return record.equals(o);
  }

  @Override
  public int hashCode() {
    return record.hashCode();
  }
}
