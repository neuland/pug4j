package de.neuland.pug4j.model;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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

    private static final Log logger = LogFactory.getLog(RecordWrapper.class);

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
            } catch (IllegalAccessException e) {
                // This can happen if the record accessor is not accessible due to module restrictions
                logger.warn("Cannot access record component '" + component.getName() +
                           "' on record type " + recordClass.getName() + ": " + e.getMessage());
            } catch (Exception e) {
                // Unexpected error - log as error since this shouldn't happen with valid records
                logger.error("Unexpected error accessing record component '" + component.getName() +
                            "' on record type " + recordClass.getName(), e);
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

    /**
     * Returns a GraalJS-compatible proxy that only implements ProxyObject (not Map).
     * This is needed because GraalJS treats objects implementing Map specially,
     * ignoring their ProxyObject implementation.
     *
     * @return a ProxyObject that delegates to this RecordWrapper
     */
    public ProxyObject asGraalProxy() {
        return new GraalJsRecordProxy(this);
    }

    /**
     * A pure ProxyObject implementation that delegates to RecordWrapper.
     * This doesn't implement Map, so GraalJS will use the ProxyObject methods.
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
        // First, check if this is a record component - return the cached value directly
        // This ensures record components are accessed as properties (person.name)
        // rather than methods (person.name())
        if (componentValues.containsKey(key)) {
            return componentValues.get(key);
        }

        // If not a component, check for custom methods on the record
        // This allows calling custom methods like person.customMethod()
        // Since component accessors were already checked above, we won't
        // accidentally expose them as callable functions
        try {
            Method method = record.getClass().getMethod(key);
            method.setAccessible(true);
            // Return a ProxyExecutable that invokes the method when called as a function
            return (ProxyExecutable) args -> {
                try {
                    Object result = method.invoke(record);
                    // Wrap record results to support nested records
                    return wrapIfRecord(result);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to invoke method '" + key + "' on record " +
                                             record.getClass().getSimpleName(), e);
                }
            };
        } catch (NoSuchMethodException e) {
            // Method not found, return null (property doesn't exist)
        }

        return null;
    }

    @Override
    public Object getMemberKeys() {
        return componentValues.keySet().toArray();
    }

    @Override
    public boolean hasMember(String key) {
        // Check if it's a component value
        if (componentValues.containsKey(key)) {
            return true;
        }

        // Also check if there's a no-arg method with this name
        try {
            record.getClass().getMethod(key);
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
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
