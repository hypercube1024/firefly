package com.firefly.template2.model.impl;

import com.firefly.template2.model.ModelService;
import com.firefly.utils.function.Action1;

import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 * @author Pengtao Qiu
 */
public class ModelServiceImpl implements ModelService {

    private Deque<Map<String, Object>> deque;

    public ModelServiceImpl() {
        this(new LinkedList<>());
    }

    public ModelServiceImpl(Map<String, Object> globalVariable) {
        this(new LinkedList<>());
        addFirst(globalVariable);
    }

    public ModelServiceImpl(Deque<Map<String, Object>> deque) {
        this.deque = deque;
    }

    @Override
    public Object get(String key) {
        for (Map<String, Object> map : deque) {
            Object object = map.get(key);
            if (object != null) {
                return object;
            }
        }
        return null;
    }

    @Override
    public Object put(String key, Object object) {
        return deque.getFirst().put(key, object);
    }

    @Override
    public Map<String, Object> createMap() {
        Map<String, Object> map = new HashMap<>();
        addFirst(map);
        return map;
    }

    @Override
    public Map<String, Object> popMap() {
        return deque.pop();
    }

    @Override
    public void addFirst(Map<String, Object> map) {
        deque.addFirst(map);
    }

    @Override
    public void addLast(Map<String, Object> map) {
        deque.addLast(map);
    }

    @Override
    public Map<String, Object> getFirst() {
        return deque.getFirst();
    }

    @Override
    public Map<String, Object> getLast() {
        return deque.getLast();
    }

    @Override
    public Map<String, Object> removeFirst() {
        return deque.removeFirst();
    }

    @Override
    public Map<String, Object> removeLast() {
        return deque.removeLast();
    }

    @Override
    public int size() {
        return deque.size();
    }

    @Override
    public void callAction(Action1<ModelService> action1) {
        addFirst(new HashMap<>());
        try {
            action1.call(this);
        } finally {
            popMap();
        }
    }
}
