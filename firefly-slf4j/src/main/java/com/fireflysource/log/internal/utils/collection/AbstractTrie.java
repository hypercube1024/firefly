package com.fireflysource.log.internal.utils.collection;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public abstract class AbstractTrie<V> implements Trie<V> {
    final boolean _caseInsensitive;

    protected AbstractTrie(boolean insensitive) {
        _caseInsensitive = insensitive;
    }

    @Override
    public boolean put(V v) {
        return put(v.toString(), v);
    }

    @Override
    public V remove(String s) {
        V o = get(s);
        put(s, null);
        return o;
    }

    @Override
    public V get(String s) {
        return get(s, 0, s.length());
    }

    @Override
    public V get(ByteBuffer b) {
        return get(b, 0, b.remaining());
    }

    @Override
    public V getBest(String s) {
        return getBest(s, 0, s.length());
    }

    @Override
    public V getBest(byte[] b, int offset, int len) {
        return getBest(new String(b, offset, len, StandardCharsets.UTF_8));
    }

    @Override
    public boolean isCaseInsensitive() {
        return _caseInsensitive;
    }

}
