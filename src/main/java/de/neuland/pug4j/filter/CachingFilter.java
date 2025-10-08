package de.neuland.pug4j.filter;


import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.util.Map;

public abstract class CachingFilter implements Filter {

    private static final int MAX_ENTRIES = 1000;

    private static final Cache<String, String> cache = Caffeine.newBuilder()
            .maximumSize(MAX_ENTRIES)
            .build();

    @Override
    public String convert(String source, Map<String, Object> attributes, Map<String, Object> model) {
        String key = source.hashCode() + "-" + attributes.hashCode();
        return cache.get(key, k -> convert(source, attributes));
    }

    abstract protected String convert(String source, Map<String, Object> attributes);

}
