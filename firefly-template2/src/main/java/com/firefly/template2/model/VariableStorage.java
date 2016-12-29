package com.firefly.template2.model;

import com.firefly.utils.function.Action0;

import java.util.Map;

/**
 * @author Pengtao Qiu
 */
public interface VariableStorage {

    Object get(String key);

    Object getFirst(String key);

    Object put(String key, Object object);

    int size();

    void callAction(Action0 action0, Map<String, Object> args);

    void callAction(Action0 action0);

}
