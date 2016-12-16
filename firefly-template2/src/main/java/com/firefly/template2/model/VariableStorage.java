package com.firefly.template2.model;

import com.firefly.utils.function.Action0;

import java.util.Map;

/**
 * @author Pengtao Qiu
 */
public interface VariableStorage {

    Object get(String key);

    Object put(String key, Object object);

    Map<String, Object> createVariable();

    Map<String, Object> removeVariable();

    void addFirst(Map<String, Object> map);

    void addLast(Map<String, Object> map);

    Map<String, Object> getFirst();

    Map<String, Object> getLast();

    Map<String, Object> removeFirst();

    Map<String, Object> removeLast();

    int size();

    void callAction(Action0 action0);

}
