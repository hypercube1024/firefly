package com.firefly.template2.model;

import com.firefly.utils.function.Action1;

import java.util.List;
import java.util.Map;

/**
 * @author Pengtao Qiu
 */
public interface ModelService {

    Object get(String key);

    Object put(String key, Object object);

    Map<String, Object> createMap();

    Map<String, Object> popMap();

    void addFirst(Map<String, Object> map);

    void addLast(Map<String, Object> map);

    Map<String, Object> getFirst();

    Map<String, Object> getLast();

    Map<String, Object> removeFirst();

    Map<String, Object> removeLast();

    int size();

    void callAction(Action1<ModelService> action1);

}
