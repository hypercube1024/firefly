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
        collection.forEach(deque::addFirst);
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
    public Object getFirst(String key) {
        return deque.isEmpty() ? null : deque.getFirst().get(key);
    }

    @Override
    public Object put(String key, Object object) {
        return deque.getFirst().put(key, object);
    }

    @Override
    public int size() {
        return deque.size();
    }

    @Override
    public void callAction(Action0 action0, Map<String, Object> args) {
        if (args != null) {
            deque.addFirst(args);
        }
        deque.addFirst(new HashMap<>());
        try {
            action0.call();
        } finally {
            if (args != null) {
                deque.removeFirst();
            }
            deque.removeFirst();
        }
    }

    @Override
    public void callAction(Action0 action0) {
        this.callAction(action0, null);
    }
}
