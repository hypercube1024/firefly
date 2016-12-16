package com.firefly.template2.model.impl;

import com.firefly.template2.model.VariableStorage;
import com.firefly.utils.function.Action0;

import java.util.*;

/**
 * @author Pengtao Qiu
 */
public class VariableStorageImpl implements VariableStorage {

    private Deque<Map<String, Object>> deque = new LinkedList<>();

    public VariableStorageImpl() {
    }

    public VariableStorageImpl(Collection<Map<String, Object>> collection) {
        collection.forEach(this::addFirst);
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
    public Map<String, Object> createVariable() {
        Map<String, Object> map = new HashMap<>();
        addFirst(map);
        return map;
    }

    @Override
    public Map<String, Object> removeVariable() {
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
    public void callAction(Action0 action0) {
        createVariable();
        try {
            action0.call();
        } finally {
            removeVariable();
        }
    }
}
