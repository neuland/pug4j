package de.neuland.pug4j.filter;


import java.util.Map;


public interface Filter {
    public String convert(String source, Map<String, Object> options, Map<String, Object> model);
}
