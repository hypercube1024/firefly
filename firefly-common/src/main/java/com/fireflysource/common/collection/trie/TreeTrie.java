package com.fireflysource.common.collection.trie;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;


/**
 * A Trie String lookup data structure using a tree
 * <p>
 * This implementation is always case-insensitive and is optimal for a variable
 * number of fixed strings with few special characters.
 * </p>
 * <p>
 * This Trie is stored in a Tree and is unlimited in capacity
 * </p>
 * <p>
 * <p>
 * This Trie is not Threadsafe and contains no mutual exclusion or deliberate
 * memory barriers. It is intended for an ArrayTrie to be built by a single
 * thread and then used concurrently by multiple threads and not mutated during
 * that access. If concurrent mutations of the Trie is required external locks
 * need to be applied.
 * </p>
 *
 * @param <V> the entry type
 */
public class TreeTrie<V> extends AbstractTrie<V> {
    private static final int[] LOOKUP =
            { // 0   1   2   3   4   5   6   7   8   9   A   B   C   D   E   F
                    /*0*/-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
                    /*1*/-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
                    /*2*/31, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 26, -1, 27, 30, -1,
                    /*3*/-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 28, 29, -1, -1, -1, -1,
                    /*4*/-1, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14,
                    /*5*/15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, -1, -1, -1, -1, -1,
                    /*6*/-1, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14,
                    /*7*/15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, -1, -1, -1, -1, -1,
            };
    private static final int INDEX = 32;
    private final TreeTrie<V>[] nextIndex;
    private final List<TreeTrie<V>> nextOther = new ArrayList<>();
    private final char ch;
    private String key;
    private V value;

    @SuppressWarnings("unchecked")
    public TreeTrie() {
        super(true);
        nextIndex = new TreeTrie[INDEX];
        ch = 0;
    }

    @SuppressWarnings("unchecked")
    private TreeTrie(char c) {
        super(true);
        nextIndex = new TreeTrie[INDEX];
        this.ch = c;
    }

    private static <V> void toString(Appendable out, TreeTrie<V> t) {
        if (t != null) {
            if (t.value != null) {
                try {
                    out.append(',');
                    out.append(t.key);
                    out.append('=');
                    out.append(t.value.toString());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            for (int i = 0; i < INDEX; i++) {
                if (t.nextIndex[i] != null)
                    toString(out, t.nextIndex[i]);
            }
            for (int i = t.nextOther.size(); i-- > 0; )
                toString(out, t.nextOther.get(i));
        }
    }

    private static <V> void keySet(Set<String> set, TreeTrie<V> t) {
        if (t != null) {
            if (t.key != null)
                set.add(t.key);

            for (int i = 0; i < INDEX; i++) {
                if (t.nextIndex[i] != null)
                    keySet(set, t.nextIndex[i]);
            }
            for (int i = t.nextOther.size(); i-- > 0; )
                keySet(set, t.nextOther.get(i));
        }
    }

    @Override
    public void clear() {
        Arrays.fill(nextIndex, null);
        nextOther.clear();
        key = null;
        value = null;
    }

    @Override
    public boolean put(String s, V v) {
        TreeTrie<V> t = this;
        int limit = s.length();
        for (int k = 0; k < limit; k++) {
            char c = s.charAt(k);

            int index = c >= 0 && c < 0x7f ? LOOKUP[c] : -1;
            if (index >= 0) {
                if (t.nextIndex[index] == null)
                    t.nextIndex[index] = new TreeTrie<V>(c);
                t = t.nextIndex[index];
            } else {
                TreeTrie<V> n = null;
                for (int i = t.nextOther.size(); i-- > 0; ) {
                    n = t.nextOther.get(i);
                    if (n.ch == c)
                        break;
                    n = null;
                }
                if (n == null) {
                    n = new TreeTrie<V>(c);
                    t.nextOther.add(n);
                }
                t = n;
            }
        }
        t.key = v == null ? null : s;
        t.value = v;
        return true;
    }

    @Override
    public V get(String s, int offset, int len) {
        TreeTrie<V> t = this;
        for (int i = 0; i < len; i++) {
            char c = s.charAt(offset + i);
            int index = c >= 0 && c < 0x7f ? LOOKUP[c] : -1;
            if (index >= 0) {
                if (t.nextIndex[index] == null)
                    return null;
                t = t.nextIndex[index];
            } else {
                TreeTrie<V> n = null;
                for (int j = t.nextOther.size(); j-- > 0; ) {
                    n = t.nextOther.get(j);
                    if (n.ch == c)
                        break;
                    n = null;
                }
                if (n == null)
                    return null;
                t = n;
            }
        }
        return t.value;
    }

    @Override
    public V get(ByteBuffer b, int offset, int len) {
        TreeTrie<V> t = this;
        for (int i = 0; i < len; i++) {
            byte c = b.get(offset + i);
            int index = c >= 0 && c < 0x7f ? LOOKUP[c] : -1;
            if (index >= 0) {
                if (t.nextIndex[index] == null)
                    return null;
                t = t.nextIndex[index];
            } else {
                TreeTrie<V> n = null;
                for (int j = t.nextOther.size(); j-- > 0; ) {
                    n = t.nextOther.get(j);
                    if (n.ch == c)
                        break;
                    n = null;
                }
                if (n == null)
                    return null;
                t = n;
            }
        }
        return t.value;
    }

    @Override
    public V getBest(byte[] b, int offset, int len) {
        TreeTrie<V> t = this;
        for (int i = 0; i < len; i++) {
            byte c = b[offset + i];
            int index = c >= 0 && c < 0x7f ? LOOKUP[c] : -1;
            if (index >= 0) {
                if (t.nextIndex[index] == null)
                    break;
                t = t.nextIndex[index];
            } else {
                TreeTrie<V> n = null;
                for (int j = t.nextOther.size(); j-- > 0; ) {
                    n = t.nextOther.get(j);
                    if (n.ch == c)
                        break;
                    n = null;
                }
                if (n == null)
                    break;
                t = n;
            }

            // Is the next Trie is a match
            if (t.key != null) {
                // Recurse so we can remember this possibility
                V best = t.getBest(b, offset + i + 1, len - i - 1);
                if (best != null)
                    return best;
                break;
            }
        }
        return t.value;
    }

    @Override
    public V getBest(String s, int offset, int len) {
        TreeTrie<V> t = this;
        for (int i = 0; i < len; i++) {
            byte c = (byte) (0xff & s.charAt(offset + i));
            int index = c >= 0 && c < 0x7f ? LOOKUP[c] : -1;
            if (index >= 0) {
                if (t.nextIndex[index] == null)
                    break;
                t = t.nextIndex[index];
            } else {
                TreeTrie<V> n = null;
                for (int j = t.nextOther.size(); j-- > 0; ) {
                    n = t.nextOther.get(j);
                    if (n.ch == c)
                        break;
                    n = null;
                }
                if (n == null)
                    break;
                t = n;
            }

            // Is the next Trie is a match
            if (t.key != null) {
                // Recurse so we can remember this possibility
                V best = t.getBest(s, offset + i + 1, len - i - 1);
                if (best != null)
                    return best;
                break;
            }
        }
        return t.value;
    }

    @Override
    public V getBest(ByteBuffer b, int offset, int len) {
        if (b.hasArray())
            return getBest(b.array(), b.arrayOffset() + b.position() + offset, len);
        return getBestByteBuffer(b, offset, len);
    }

    private V getBestByteBuffer(ByteBuffer b, int offset, int len) {
        TreeTrie<V> t = this;
        int pos = b.position() + offset;
        for (int i = 0; i < len; i++) {
            byte c = b.get(pos++);
            int index = c >= 0 && c < 0x7f ? LOOKUP[c] : -1;
            if (index >= 0) {
                if (t.nextIndex[index] == null)
                    break;
                t = t.nextIndex[index];
            } else {
                TreeTrie<V> n = null;
                for (int j = t.nextOther.size(); j-- > 0; ) {
                    n = t.nextOther.get(j);
                    if (n.ch == c)
                        break;
                    n = null;
                }
                if (n == null)
                    break;
                t = n;
            }

            // Is the next Trie is a match
            if (t.key != null) {
                // Recurse so we can remember this possibility
                V best = t.getBest(b, offset + i + 1, len - i - 1);
                if (best != null)
                    return best;
                break;
            }
        }
        return t.value;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        toString(buf, this);

        if (buf.length() == 0)
            return "{}";

        buf.setCharAt(0, '{');
        buf.append('}');
        return buf.toString();
    }

    @Override
    public Set<String> keySet() {
        Set<String> keys = new HashSet<>();
        keySet(keys, this);
        return keys;
    }

    @Override
    public boolean isFull() {
        return false;
    }

}
