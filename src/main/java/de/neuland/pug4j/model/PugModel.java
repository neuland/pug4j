package de.neuland.pug4j.model;

import de.neuland.pug4j.filter.Filter;
import de.neuland.pug4j.parser.node.MixinNode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PugModel implements Map<String, Object> {

  private static final String LOCALS = "locals";
  public static final String LOCAL_VARS = "pug4j__localVars";
  public static final String PUG4J_MODEL_PREFIX = "pug4j__";
  private final Deque<Map<String, Object>> scopes = new LinkedList<>();
  private final Map<String, MixinNode> mixins = new HashMap<>();
  private final Map<String, Filter> filter = new HashMap<>();

  public PugModel(Map<String, Object> defaults) {
    pushScope();

    if (defaults != null) {
      putAll(defaults);
    }

    putLocal(LOCALS, this);
  }

  public void pushScope() {
    HashMap<String, Object> scope = new HashMap<>();
    scope.put(LOCAL_VARS, new HashSet<>());
    scopes.add(scope);
  }

  public void popScope() {
    scopes.removeLast();
  }

  public void setMixin(String name, MixinNode node) {
    mixins.put(name, node);
  }

  public MixinNode getMixin(String name) {
    return mixins.get(name);
  }

  @Override
  public void clear() {
    scopes.clear();
    scopes.add(new HashMap<>());
  }

  @Override
  public boolean containsKey(Object key) {
    for (Iterator<Map<String, Object>> i = scopes.descendingIterator(); i.hasNext(); ) {
      Map<String, Object> scope = i.next();
      if (scope.containsKey(key)) {
        return true;
      }
    }
    return false;
  }

  public boolean knowsKey(String key) {
    for (Iterator<Map<String, Object>> i = scopes.descendingIterator(); i.hasNext(); ) {
      Map<String, Object> scope = i.next();
      Set<String> localVars = (Set<String>) scope.get(LOCAL_VARS);
      if (scope.containsKey(key) || localVars.contains(key)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean containsValue(Object value) {
    for (Iterator<Map<String, Object>> i = scopes.descendingIterator(); i.hasNext(); ) {
      Map<String, Object> scope = i.next();
      if (scope.containsValue(value)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public Set<java.util.Map.Entry<String, Object>> entrySet() {
    HashMap<String, Object> map = new HashMap<>();
    for (String key : keySet()) {
      map.put(key, get(key));
    }
    return map.entrySet();
  }

  @Override
  // adds the object to the highest scope
  public Object get(Object key) {
    for (Iterator<Map<String, Object>> i = scopes.descendingIterator(); i.hasNext(); ) {
      Map<String, Object> scope = i.next();
      if (scope.containsKey(key)) {
        return scope.get(key);
      }
    }
    return null;
  }

  private Map<String, Object> getScopeWithKey(Object key) {
    for (Iterator<Map<String, Object>> i = scopes.descendingIterator(); i.hasNext(); ) {
      Map<String, Object> scope = i.next();
      Set<String> localVars = (Set<String>) scope.get(LOCAL_VARS);
      if (scope.containsKey(key) || localVars.contains(key)) {
        return scope;
      }
    }
    return null;
  }

  @Override
  public boolean isEmpty() {
    return keySet().isEmpty();
  }

  @Override
  // returns a set of unique keys
  public Set<String> keySet() {
    Set<String> keys = new HashSet<>();
    for (Iterator<Map<String, Object>> i = scopes.descendingIterator(); i.hasNext(); ) {
      keys.addAll(i.next().keySet());
    }
    return keys;
  }

  @Override
  // adds the object to the correct scope
  public Object put(String key, Object value) {
    Set<String> localVars = getLocalVars();
    if (localVars.contains(key)) {
      return putLocal(key, value);
    } else {
      return putGlobal(key, value);
    }
  }

  private Set<String> getLocalVars() {
    return (Set<String>) scopes.getLast().get(LOCAL_VARS);
  }

  // adds the object to the current scope
  public Object putLocal(String key, Object value) {
    Object currentValue = get(key);
    Map<String, Object> scope = scopes.getLast();
    scope.put(key, RecordWrapper.wrapIfRecord(value));
    return currentValue;
  }

  // adds the object to the scope where the variable was last defined
  public Object putGlobal(String key, Object value) {
    Object currentValue = get(key);
    Map<String, Object> scope = getScopeWithKey(key);
    if (scope == null) scope = scopes.getLast();
    scope.put(key, RecordWrapper.wrapIfRecord(value));
    return currentValue;
  }

  @Override
  // addes all map entries to the current scope map
  public void putAll(Map<? extends String, ? extends Object> m) {
    Map<String, Object> scope = scopes.getLast();
    for (Map.Entry<? extends String, ? extends Object> entry : m.entrySet()) {
      Object wrapped = RecordWrapper.wrapIfRecord(entry.getValue());
      scope.put(entry.getKey(), wrapped);
    }
  }

  @Override
  // removes the scopes first object with the given key
  public Object remove(Object key) {
    for (Iterator<Map<String, Object>> i = scopes.descendingIterator(); i.hasNext(); ) {
      Map<String, Object> scope = i.next();
      if (scope.containsKey(key)) {
        Object object = scope.get(key);
        scope.remove(key);
        return object;
      }
    }
    return null;
  }

  @Override
  // returns the size of all unique keys
  public int size() {
    return keySet().size();
  }

  @Override
  // returns the size of all unique keys
  public Collection<Object> values() {
    List<Object> values = new ArrayList<>();
    for (String key : keySet()) {
      values.add(get(key));
    }
    return values;
  }

  public Filter getFilter(String name) {
    return filter.get(name);
  }

  public void addFilter(String name, Filter filter) {
    this.filter.put(name, filter);
  }

  public void putLocalVariableName(String name) {
    getLocalVars().add(name);
  }
}
