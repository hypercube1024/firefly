package com.fireflysource.common.collection.trie;

import java.nio.ByteBuffer;
import java.util.*;

/**
 * <p>A Ternary Trie String lookup data structure.</p>
 * <p>
 * This Trie is of a fixed size and cannot grow (which can be a good thing with the regards to DOS when used as a cache).
 * </p>
 * <p>
 * The Trie is stored in 3 arrays:
 * </p>
 * <dl>
 * <dt>char[] tree</dt><dd>This is semantically 2 dimensional array flattened into a 1 dimensional char array. The second dimension
 * is that every 4 sequential elements represents a row of: character; hi index; eq index; low index, used to build a
 * ternary trie of key strings.</dd>
 * <dt>String[] key</dt><dd>An array of key values where each element matches a row in the tree array. A non-zero key element
 * indicates that the tree row is a complete key rather than an intermediate character of a longer key.</dd>
 * <dt>V[] value</dt><dd>An array of values corresponding to the key array</dd>
 * </dl>
 * <p>The lookup of a value will iterate through the tree array matching characters. If the equal tree branch follows,
 * then the key array looks up to see if this is a complete match.  If a match finds then the value array looks up
 * to return the matching value.
 * </p>
 * <p>
 * This Trie may instantiate either as case-sensitive or insensitive.
 * </p>
 * <p>This Trie is not Threadsafe and contains no mutual exclusion
 * or deliberate memory barriers.  It is intended for an ArrayTrie to be
 * built by a single thread and then used concurrently by multiple threads
 * and not mutated during that access.  If concurrent mutations of the
 * Trie is required external locks need to be applied.
 * </p>
 *
 * @param <V> the Entry type
 */
@SuppressWarnings("unchecked")
public class ArrayTernaryTrie<V> extends AbstractTrie<V> {

    public static final char[] LOWER_CASES = {'\000', '\001', '\002', '\003', '\004', '\005', '\006', '\007', '\010',
            '\011', '\012', '\013', '\014', '\015', '\016', '\017', '\020', '\021', '\022', '\023', '\024', '\025',
            '\026', '\027', '\030', '\031', '\032', '\033', '\034', '\035', '\036', '\037', '\040', '\041', '\042',
            '\043', '\044', '\045', '\046', '\047', '\050', '\051', '\052', '\053', '\054', '\055', '\056', '\057',
            '\060', '\061', '\062', '\063', '\064', '\065', '\066', '\067', '\070', '\071', '\072', '\073', '\074',
            '\075', '\076', '\077', '\100', '\141', '\142', '\143', '\144', '\145', '\146', '\147', '\150', '\151',
            '\152', '\153', '\154', '\155', '\156', '\157', '\160', '\161', '\162', '\163', '\164', '\165', '\166',
            '\167', '\170', '\171', '\172', '\133', '\134', '\135', '\136', '\137', '\140', '\141', '\142', '\143',
            '\144', '\145', '\146', '\147', '\150', '\151', '\152', '\153', '\154', '\155', '\156', '\157', '\160',
            '\161', '\162', '\163', '\164', '\165', '\166', '\167', '\170', '\171', '\172', '\173', '\174', '\175',
            '\176', '\177'};
    /**
     * The Size of a Trie row is the char, and the low, equal and high
     * child pointers
     */
    private static final int ROW_SIZE = 4;
    private static int LO = 1;
    private static int EQ = 2;
    private static int HI = 3;
    /**
     * The Trie rows in a single array which allows a lookup of row,character
     * to the next row in the Trie.  This is actually a 2 dimensional
     * array that has been flattened to achieve locality of reference.
     */
    private final char[] tree;

    /**
     * The key (if any) for a Trie row.
     * A row may be a leaf, a node or both in the Trie tree.
     */
    private final String[] key;

    /**
     * The value (if any) for a Trie row.
     * A row may be a leaf, a node or both in the Trie tree.
     */
    private final V[] value;

    /**
     * The number of rows allocated
     */
    private char rows;

    /**
     * Create a case-insensitive Trie of default capacity.
     */
    public ArrayTernaryTrie() {
        this(128);
    }

    /**
     * Create a Trie of default capacity
     *
     * @param insensitive true if the Trie is insensitive to the case of the key.
     */
    public ArrayTernaryTrie(boolean insensitive) {
        this(insensitive, 128);
    }

    /**
     * Create a case-insensitive Trie
     *
     * @param capacity The capacity of the Trie, which is in the worst case
     *                 is the total number of characters of all keys stored in the Trie.
     *                 The capacity needed is dependent of the shared prefixes of the keys.
     *                 For example, a capacity of 6 nodes require to store the keys "foo"
     *                 and "bar", but a capacity of only 4 is required to
     *                 store "bar" and "bat".
     */
    public ArrayTernaryTrie(int capacity) {
        this(true, capacity);
    }

    /**
     * Create a Trie
     *
     * @param insensitive true if the Trie is insensitive to the case of the key.
     * @param capacity    The capacity of the Trie, which is in the worst case
     *                    is the total number of characters of all keys stored in the Trie.
     *                    The capacity needed is dependent of the shared prefixes of the keys.
     *                    For example, a capacity of 6 nodes require to store the keys "foo"
     *                    and "bar", but a capacity of only 4 is required to
     *                    store "bar" and "bat".
     */
    public ArrayTernaryTrie(boolean insensitive, int capacity) {
        super(insensitive);
        value = (V[]) new Object[capacity];
        tree = new char[capacity * ROW_SIZE];
        key = new String[capacity];
    }

    /**
     * Copy Trie and change capacity by a factor
     *
     * @param trie   the trie to copy from
     * @param factor the factor to grow the capacity by
     */
    public ArrayTernaryTrie(ArrayTernaryTrie<V> trie, double factor) {
        super(trie.isCaseInsensitive());
        int capacity = (int) (trie.value.length * factor);
        rows = trie.rows;
        value = Arrays.copyOf(trie.value, capacity);
        tree = Arrays.copyOf(trie.tree, capacity * ROW_SIZE);
        key = Arrays.copyOf(trie.key, capacity);
    }

    public static int hilo(int diff) {
        // branchless equivalent to return ((diff<0)?LO:HI);
        // return 3+2*((diff&Integer.MIN_VALUE)>>Integer.SIZE-1);
        return 1 + (diff | Integer.MAX_VALUE) / (Integer.MAX_VALUE / 2);
    }

    @Override
    public void clear() {
        rows = 0;
        Arrays.fill(value, null);
        Arrays.fill(tree, (char) 0);
        Arrays.fill(key, null);
    }

    @Override
    public boolean put(String s, V v) {
        int t = 0;
        int limit = s.length();
        int last;
        for (int k = 0; k < limit; k++) {
            char c = s.charAt(k);
            if (isCaseInsensitive() && c < 128)
                c = LOWER_CASES[c];

            while (true) {
                int row = ROW_SIZE * t;

                // Do we need to create the new row?
                if (t == rows) {
                    rows++;
                    if (rows >= key.length) {
                        rows--;
                        return false;
                    }
                    tree[row] = c;
                }

                char n = tree[row];
                int diff = n - c;
                if (diff == 0)
                    t = tree[last = (row + EQ)];
                else if (diff < 0)
                    t = tree[last = (row + LO)];
                else
                    t = tree[last = (row + HI)];

                // do we need a new row?
                if (t == 0) {
                    t = rows;
                    tree[last] = (char) t;
                }

                if (diff == 0) break;
            }
        }

        // Do we need to create the new row?
        if (t == rows) {
            rows++;
            if (rows >= key.length) {
                rows--;
                return false;
            }
        }

        // Put the key and value
        key[t] = v == null ? null : s;
        value[t] = v;

        return true;
    }

    @Override
    public V get(String s, int offset, int len) {
        int t = 0;
        for (int i = 0; i < len; ) {
            char c = s.charAt(offset + i++);
            if (isCaseInsensitive() && c < 128)
                c = LOWER_CASES[c];

            while (true) {
                int row = ROW_SIZE * t;
                char n = tree[row];
                int diff = n - c;

                if (diff == 0) {
                    t = tree[row + EQ];
                    if (t == 0) return null;
                    break;
                }

                t = tree[row + hilo(diff)];
                if (t == 0) return null;
            }
        }

        return value[t];
    }

    @Override
    public V get(ByteBuffer b, int offset, int len) {
        int t = 0;
        offset += b.position();

        for (int i = 0; i < len; ) {
            byte c = (byte) (b.get(offset + i++) & 0x7f);
            if (isCaseInsensitive())
                c = (byte) LOWER_CASES[c];

            while (true) {
                int row = ROW_SIZE * t;
                char n = tree[row];
                int diff = n - c;

                if (diff == 0) {
                    t = tree[row + EQ];
                    if (t == 0) return null;
                    break;
                }

                t = tree[row + hilo(diff)];
                if (t == 0) return null;
            }
        }
        return value[t];
    }

    @Override
    public V getBest(String s) {
        return getBest(0, s, 0, s.length());
    }

    @Override
    public V getBest(String s, int offset, int length) {
        return getBest(0, s, offset, length);
    }

    private V getBest(int t, String s, int offset, int len) {
        int node = t;
        int end = offset + len;
        loop:
        while (offset < end) {
            char c = s.charAt(offset++);
            len--;
            if (isCaseInsensitive() && c < 128)
                c = LOWER_CASES[c];

            while (true) {
                int row = ROW_SIZE * t;
                char n = tree[row];
                int diff = n - c;

                if (diff == 0) {
                    t = tree[row + EQ];
                    if (t == 0) break loop;

                    // if this node is a match, recurse to remember
                    if (key[t] != null) {
                        node = t;
                        V better = getBest(t, s, offset, len);
                        if (better != null) return better;
                    }
                    break;
                }

                t = tree[row + hilo(diff)];
                if (t == 0) break loop;
            }
        }
        return value[node];
    }

    @Override
    public V getBest(ByteBuffer b, int offset, int len) {
        if (b.hasArray()) return getBest(0, b.array(), b.arrayOffset() + b.position() + offset, len);
        else return getBest(0, b, offset, len);
    }

    private V getBest(int t, byte[] b, int offset, int len) {
        int node = t;
        int end = offset + len;
        loop:
        while (offset < end) {
            byte c = (byte) (b[offset++] & 0x7f);
            len--;
            if (isCaseInsensitive())
                c = (byte) LOWER_CASES[c];

            while (true) {
                int row = ROW_SIZE * t;
                char n = tree[row];
                int diff = n - c;

                if (diff == 0) {
                    t = tree[row + EQ];
                    if (t == 0) break loop;

                    // if this node is a match, recurse to remember
                    if (key[t] != null) {
                        node = t;
                        V better = getBest(t, b, offset, len);
                        if (better != null) return better;
                    }
                    break;
                }

                t = tree[row + hilo(diff)];
                if (t == 0) break loop;
            }
        }
        return (V) value[node];
    }

    private V getBest(int t, ByteBuffer b, int offset, int len) {
        int node = t;
        int o = offset + b.position();

        loop:
        for (int i = 0; i < len; i++) {
            byte c = (byte) (b.get(o + i) & 0x7f);
            if (isCaseInsensitive())
                c = (byte) LOWER_CASES[c];

            while (true) {
                int row = ROW_SIZE * t;
                char n = tree[row];
                int diff = n - c;

                if (diff == 0) {
                    t = tree[row + EQ];
                    if (t == 0) break loop;

                    // if this node is a match, recurse to remember
                    if (key[t] != null) {
                        node = t;
                        V best = getBest(t, b, offset + i + 1, len - i - 1);
                        if (best != null) return best;
                    }
                    break;
                }

                t = tree[row + hilo(diff)];
                if (t == 0) break loop;
            }
        }
        return value[node];
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        for (int r = 0; r <= rows; r++) {
            if (key[r] != null && value[r] != null) {
                buf.append(',');
                buf.append(key[r]);
                buf.append('=');
                buf.append(value[r].toString());
            }
        }
        if (buf.length() == 0) return "{}";

        buf.setCharAt(0, '{');
        buf.append('}');
        return buf.toString();
    }

    @Override
    public Set<String> keySet() {
        Set<String> keys = new HashSet<>();

        for (int r = 0; r <= rows; r++) {
            if (key[r] != null && value[r] != null)
                keys.add(key[r]);
        }
        return keys;
    }

    public int size() {
        int s = 0;
        for (int r = 0; r <= rows; r++) {
            if (key[r] != null && value[r] != null)
                s++;
        }
        return s;
    }

    public boolean isEmpty() {
        for (int r = 0; r <= rows; r++) {
            if (key[r] != null && value[r] != null)
                return false;
        }
        return true;
    }

    public Set<Map.Entry<String, V>> entrySet() {
        Set<Map.Entry<String, V>> entries = new HashSet<>();
        for (int r = 0; r <= rows; r++) {
            if (key[r] != null && value[r] != null)
                entries.add(new AbstractMap.SimpleEntry<>(key[r], value[r]));
        }
        return entries;
    }

    @Override
    public boolean isFull() {
        return rows + 1 == key.length;
    }

    public void dump() {
        for (int r = 0; r < rows; r++) {
            char c = tree[r * ROW_SIZE];
            System.err.printf("%4d [%s,%d,%d,%d] '%s':%s%n",
                    r,
                    (c < ' ' || c > 127) ? ("" + (int) c) : "'" + c + "'",
                    (int) tree[r * ROW_SIZE + LO],
                    (int) tree[r * ROW_SIZE + EQ],
                    (int) tree[r * ROW_SIZE + HI],
                    key[r],
                    value[r]);
        }

    }

    public static class Growing<V> implements Trie<V> {
        private final int growBy;
        private ArrayTernaryTrie<V> trie;

        public Growing() {
            this(1024, 1024);
        }

        public Growing(int capacity, int growBy) {
            this.growBy = growBy;
            trie = new ArrayTernaryTrie<>(capacity);
        }

        public Growing(boolean insensitive, int capacity, int growby) {
            growBy = growby;
            trie = new ArrayTernaryTrie<>(insensitive, capacity);
        }

        public boolean put(V v) {
            return put(v.toString(), v);
        }

        public int hashCode() {
            return trie.hashCode();
        }

        public V remove(String s) {
            return trie.remove(s);
        }

        public V get(String s) {
            return trie.get(s);
        }

        public V get(ByteBuffer b) {
            return trie.get(b);
        }

        public V getBest(byte[] b, int offset, int len) {
            return trie.getBest(b, offset, len);
        }

        public boolean isCaseInsensitive() {
            return trie.isCaseInsensitive();
        }

        public boolean equals(Object obj) {
            return trie.equals(obj);
        }

        public void clear() {
            trie.clear();
        }

        public boolean put(String s, V v) {
            boolean added = trie.put(s, v);
            while (!added && growBy > 0) {
                ArrayTernaryTrie<V> bigger = new ArrayTernaryTrie<>(trie.key.length + growBy);
                for (Map.Entry<String, V> entry : trie.entrySet())
                    bigger.put(entry.getKey(), entry.getValue());
                trie = bigger;
                added = trie.put(s, v);
            }

            return added;
        }

        public V get(String s, int offset, int len) {
            return trie.get(s, offset, len);
        }

        public V get(ByteBuffer b, int offset, int len) {
            return trie.get(b, offset, len);
        }

        public V getBest(String s) {
            return trie.getBest(s);
        }

        public V getBest(String s, int offset, int length) {
            return trie.getBest(s, offset, length);
        }

        public V getBest(ByteBuffer b, int offset, int len) {
            return trie.getBest(b, offset, len);
        }

        public String toString() {
            return trie.toString();
        }

        public Set<String> keySet() {
            return trie.keySet();
        }

        public boolean isFull() {
            return false;
        }

        public void dump() {
            trie.dump();
        }

        public boolean isEmpty() {
            return trie.isEmpty();
        }

        public int size() {
            return trie.size();
        }

    }
}
