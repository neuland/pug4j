package de.neuland.pug4j.model;

import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;

import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.util.*;

/**
 * A wrapper for Java records that makes them behave like Maps for template expression evaluation.
 * This allows record components to be accessed using dot notation (e.g., person.name) in templates,
 * automatically mapping to the record's accessor methods (e.g., person.name()).
 *
 * Nested records are automatically wrapped recursively, so person.address.city works seamlessly.
 *
 * Also implements GraalVM's ProxyObject to support method call syntax (e.g., person.name()) when
 * using the GraalJS expression handler.
 */
public class RecordWrapper implements Map<String, Object>, ProxyObject {

    private final Object record;
    private final Map<String, Object> componentValues;

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
        this.componentValues = new HashMap<>();
        extractComponents();
    }

    /**
     * Extracts all component values from the record using reflection.
     */
    private void extractComponents() {
        Class<?> recordClass = record.getClass();
        RecordComponent[] components = recordClass.getRecordComponents();

        for (RecordComponent component : components) {
            try {
                String name = component.getName();
                var accessor = component.getAccessor();
                accessor.setAccessible(true);  // Required for accessing records in different packages/modules
                Object value = accessor.invoke(record);

                // Recursively wrap nested records
                if (value != null && value.getClass().isRecord()) {
                    value = new RecordWrapper(value);
                }

                componentValues.put(name, value);
            } catch (Exception e) {
                // If we can't access a component, skip it
                // This shouldn't happen with valid records
            }
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
        return componentValues.size();
    }

    @Override
    public boolean isEmpty() {
        return componentValues.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return componentValues.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return componentValues.containsValue(value);
    }

    @Override
    public Object get(Object key) {
        return componentValues.get(key);
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
        return componentValues.keySet();
    }

    @Override
    public Collection<Object> values() {
        return componentValues.values();
    }

    @Override
    public Set<Entry<String, Object>> entrySet() {
        return componentValues.entrySet();
    }

    /**
     * Returns the original wrapped record instance.
     * This can be useful for expression handlers that need direct method access.
     *
     * @return the original record
     */
    public Object getRecord() {
        return record;
    }

    // ProxyObject implementation for GraalVM support

    @Override
    public Object getMember(String key) {
        // For property access, return the value directly
        if (componentValues.containsKey(key)) {
            return componentValues.get(key);
        }

        // Also check if there's a corresponding no-arg method (record accessor)
        // This allows method call syntax like person.name() in addition to person.name
        try {
            Method method = record.getClass().getMethod(key);
            if (method.getParameterCount() == 0) {
                method.setAccessible(true);
                // Return a ProxyExecutable that invokes the method when called as a function
                return (ProxyExecutable) args -> {
                    try {
                        Object result = method.invoke(record);
                        // Wrap record results
                        if (result != null && result.getClass().isRecord()) {
                            return new RecordWrapper(result);
                        }
                        return result;
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to invoke " + key, e);
                    }
                };
            }
        } catch (NoSuchMethodException e) {
            // No method with this name
        }

        return null;
    }

    @Override
    public Object getMemberKeys() {
        return componentValues.keySet().toArray();
    }

    @Override
    public boolean hasMember(String key) {
        return componentValues.containsKey(key);
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
