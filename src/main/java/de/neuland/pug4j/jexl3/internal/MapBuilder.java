package de.neuland.pug4j.jexl3.internal;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class MapBuilder implements org.apache.commons.jexl3.JexlArithmetic.MapBuilder {
    protected final Map<Object, Object> map;

    public MapBuilder(int size) {
        this.map = new LinkedHashMap(size);
    }
    public MapBuilder(int size, boolean extended) {
        this.map = (Map<Object, Object>)(extended ? new LinkedHashMap(size) : new LinkedHashMap(size));
    }

    public void put(Object key, Object value) {
        this.map.put(key, value);
    }

    public Object create() {
        return this.map;
    }
}
