package com.fireflysource.log;

import java.util.Map;
import java.util.Set;

/**
 * @author Pengtao Qiu
 */
public interface MappedDiagnosticContext {

    void put(String key, String val);

    String get(String key);

    void remove(String key);

    void clear();

    Set<String> getKeys();

    Map<String, String> getCopyOfContextMap();

    void setContextMap(Map<String, String> contextMap);
}
