package com.firefly.utils.collection;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;

public class WeakReferenceConcurrentHashMap<K, V> extends AbstractConcurrentAutomaticClearMap<K, V> {

    private class ValueWeakReference extends WeakReference<V> {

        K key;

        public ValueWeakReference(K key, V value) {
            super(value, refQueue);
            this.key = key;
        }

    }

    @SuppressWarnings("unchecked")
    @Override
    protected void clearInvalidEntry(Reference<? extends V> ref) {
        map.remove(((ValueWeakReference) ref).key);
    }

    @Override
    protected Reference<V> createRefence(K key, V value) {
        return new ValueWeakReference(key, value);
    }

}
