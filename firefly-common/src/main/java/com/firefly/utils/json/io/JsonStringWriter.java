package com.firefly.utils.json.io;

import com.firefly.utils.function.Action1;
import com.firefly.utils.lang.ArrayUtils;

import java.lang.ref.SoftReference;
import java.util.*;

import static com.firefly.utils.json.JsonStringSymbol.*;

public class JsonStringWriter extends AbstractJsonStringWriter {

    private static final ThreadLocal<SoftReference<LRUHashMap<String, char[]>>> escapedJsonStringCache = ThreadLocal.withInitial(() -> new SoftReference<>(new LRUHashMap<>()));

    private static class LRUHashMap<K, V> extends LinkedHashMap<K, V> {

        private static final int maxCacheSize = Integer.getInteger("com.fireflysource.utils.json.writer.string.cache", 512);

        LRUHashMap() {
            super(maxCacheSize, 0.75f, true);
        }

        @Override
        protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
            return size() > maxCacheSize;
        }
    }

    private static final int MAX_CACHE_SIZE = 1024;

    public static Map<Character, char[]> SPECIAL_CHARACTER = new HashMap<Character, char[]>() {{
        for (int i = 0; i <= 0x1f; i++) {
            put((char) i, toHexUnicode((char) i));
        }
        for (int i = 0x7f; i <= 0x9f; i++) {
            put((char) i, toHexUnicode((char) i));
        }
        put((char) 0x00ad, toHexUnicode((char) 0x00ad));
        for (int i = 0x0600; i <= 0x0604; i++) {
            put((char) i, toHexUnicode((char) i));
        }
        put((char) 0x070f, toHexUnicode((char) 0x070f));
        put((char) 0x17b4, toHexUnicode((char) 0x17b4));
        put((char) 0x17b5, toHexUnicode((char) 0x17b5));
        for (int i = 0x200c; i <= 0x200f; i++) {
            put((char) i, toHexUnicode((char) i));
        }
        for (int i = 0x2028; i <= 0x202f; i++) {
            put((char) i, toHexUnicode((char) i));
        }
        for (int i = 0x2060; i <= 0x206f; i++) {
            put((char) i, toHexUnicode((char) i));
        }
        put((char) 0xfeff, toHexUnicode((char) 0xfeff));
        for (int i = 0xff01; i <= 0xff0f; i++) {
            put((char) i, toHexUnicode((char) i));
        }
        for (int i = 0xfff0; i <= 0xffff; i++) {
            put((char) i, toHexUnicode((char) i));
        }

        put('\b', new char[]{'\\', 'b'});
        put('\n', new char[]{'\\', 'n'});
        put('\r', new char[]{'\\', 'r'});
        put('\f', new char[]{'\\', 'f'});
        put('\\', new char[]{'\\', '\\'});
        put('/', new char[]{'\\', '/'});
        put('"', new char[]{'\\', '"'});
        put('\t', new char[]{'\\', 't'});
    }};

    public static char[] toHexUnicode(char ch) {
        char[] chars = new char[6];
        chars[0] = '\\';
        chars[1] = 'u';
        int index = 2;
        String hexStr = Integer.toHexString(ch);
        for (int j = hexStr.length(); j < 4; j++) {
            chars[index++] = '0';
        }
        hexStr.getChars(0, hexStr.length(), chars, index);
        return chars;
    }

    private static char[] getCachedJsonString(String value) {
        if (value.length() >= MAX_CACHE_SIZE) {
            return null;
        }
        SoftReference<LRUHashMap<String, char[]>> softReference = escapedJsonStringCache.get();
        if (softReference != null) {
            LRUHashMap<String, char[]> map = softReference.get();
            if (map != null) {
                return map.get(value);
            }
        }
        return null;
    }

    private static void putJsonStringToCache(String value, char[] chars) {
        if (value.length() >= MAX_CACHE_SIZE) {
            return;
        }
        SoftReference<LRUHashMap<String, char[]>> softReference = escapedJsonStringCache.get();
        if (softReference == null) {
            LRUHashMap<String, char[]> map = new LRUHashMap<>();
            map.put(value, chars);
            softReference = new SoftReference<>(map);
            escapedJsonStringCache.set(softReference);
        } else {
            LRUHashMap<String, char[]> map = softReference.get();
            if (map == null) {
                map = new LRUHashMap<>();
                map.put(value, chars);
                softReference = new SoftReference<>(map);
                escapedJsonStringCache.set(softReference);
            } else {
                map.put(value, chars);
            }
        }
    }

    public static char[] escapeJsonString(String value) {
        char[] chars = getCachedJsonString(value);
        if (chars == null) {
            if (hasSpecialChar(value)) {
                try (JsonStringWriter writer = new JsonStringWriter()) {
                    for (int i = 0; i < value.length(); i++) {
                        char ch = value.charAt(i);
                        char[] s = SPECIAL_CHARACTER.get(ch);
                        if (s != null) {
                            writer.write(s);
                        } else {
                            writer.write(ch);
                        }
                    }
                    chars = new char[writer.count];
                    System.arraycopy(writer.buf, 0, chars, 0, writer.count);
                }
                putJsonStringToCache(value, chars);
            } else {
                chars = value.toCharArray();
                putJsonStringToCache(value, chars);
            }
        }
        return chars;
    }

    private static boolean hasSpecialChar(String value) {
        for (int i = 0; i < value.length(); i++) {
            if (SPECIAL_CHARACTER.containsKey(value.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    private Deque<Object> deque = new LinkedList<>();

    @Override
    public void pushRef(Object obj) {
        deque.addFirst(obj);
    }

    @Override
    public boolean existRef(Object obj) {
        return deque.contains(obj);
    }

    @Override
    public void popRef() {
        deque.removeFirst();
    }

    private void writeCharsWithQuote(char[] chars) {
        buf[count++] = QUOTE;
        int len = chars.length;
        int newCount = count + len;
        System.arraycopy(chars, 0, buf, count, len);
        count = newCount;
        buf[count++] = QUOTE;
    }

    @Override
    public void writeStringWithQuote(String value) {
        char[] escapedValue = escapeJsonString(value);
        int newCount = count + escapedValue.length + 2;
        if (newCount > buf.length) {
            expandCapacity(newCount);
        }
        writeCharsWithQuote(escapedValue);
    }

    @Override
    public void writeStringArray(String[] array) {
        int arrayLen = array.length;
        if (arrayLen == 0) {
            buf[count++] = ARRAY_PRE;
            buf[count++] = ARRAY_SUF;
            return;
        }

        char[][] escapedChars = new char[array.length][];
        int iMax = arrayLen - 1;
        int totalSize = 2;
        for (int i = 0; i < array.length; i++) {
            String str = array[i];
            char[] escapedValue = escapeJsonString(str);
            escapedChars[i] = escapedValue;
            totalSize += escapedValue.length + 2 + 1;
        }

        int newCount = count + totalSize;
        if (newCount > buf.length) {
            expandCapacity(newCount);
        }

        buf[count++] = ARRAY_PRE;
        for (int i = 0; ; ++i) {
            writeCharsWithQuote(escapedChars[i]);
            if (i == iMax) {
                buf[count++] = ARRAY_SUF;
                return;
            }
            buf[count++] = SEPARATOR;
        }
    }

    private <T extends Number> void writeNumberArray(T[] array, int elementMaxLen, Action1<T> copyToChars) {
        int arrayLen = array.length;
        if (arrayLen == 0) {
            buf[count++] = ARRAY_PRE;
            buf[count++] = ARRAY_SUF;
            return;
        }

        int iMax = arrayLen - 1;
        int newCount = count + (elementMaxLen + 1) * arrayLen + 2 - 1;
        if (newCount > buf.length) {
            expandCapacity(newCount);
        }

        buf[count++] = ARRAY_PRE;
        for (int i = 0; ; i++) {
            copyToChars.call(array[i]);
            if (i == iMax) {
                buf[count++] = ARRAY_SUF;
                return;
            }
            buf[count++] = SEPARATOR;
        }
    }

    @Override
    public void writeIntArray(int[] array) {
        writeIntArray(ArrayUtils.toObject(array));
    }

    @Override
    public void writeIntArray(Integer[] array) {
        int elementMaxLen = MIN_INT_VALUE.length;
        writeNumberArray(array, elementMaxLen, val -> {
            if (val == Integer.MIN_VALUE) {
                System.arraycopy(MIN_INT_VALUE, 0, buf, count, elementMaxLen);
                count += elementMaxLen;
            } else {
                count += (val < 0) ? IOUtils.stringSize(-val) + 1 : IOUtils.stringSize(val);
                IOUtils.getChars(val, count, buf);
            }
        });
    }

    @Override
    public void writeShortArray(short[] array) {
        writeShortArray(ArrayUtils.toObject(array));
    }

    @Override
    public void writeShortArray(Short[] array) {
        int elementMaxLen = MIN_SHORT_VALUE.length;
        writeNumberArray(array, elementMaxLen, val -> {
            if (val == Short.MIN_VALUE) {
                System.arraycopy(MIN_SHORT_VALUE, 0, buf, count, elementMaxLen);
                count += elementMaxLen;
            } else {
                count += (val < 0) ? IOUtils.stringSize(-val) + 1 : IOUtils.stringSize(val);
                IOUtils.getChars(val, count, buf);
            }
        });
    }

    @Override
    public void writeLongArray(long[] array) {
        writeLongArray(ArrayUtils.toObject(array));
    }

    @Override
    public void writeLongArray(Long[] array) {
        int elementMaxLen = MIN_LONG_VALUE.length;
        writeNumberArray(array, elementMaxLen, val -> {
            if (val == Long.MIN_VALUE) {
                System.arraycopy(MIN_LONG_VALUE, 0, buf, count, elementMaxLen);
                count += elementMaxLen;
            } else {
                count += (val < 0) ? IOUtils.stringSize(-val) + 1 : IOUtils.stringSize(val);
                IOUtils.getChars(val, count, buf);
            }
        });
    }

    @Override
    public void writeBooleanArray(boolean[] array) {
        writeBooleanArray(ArrayUtils.toObject(array));
    }

    @Override
    public void writeBooleanArray(Boolean[] array) {
        int arrayLen = array.length;
        if (arrayLen == 0) {
            buf[count++] = ARRAY_PRE;
            buf[count++] = ARRAY_SUF;
            return;
        }
        int iMax = arrayLen - 1;
        int newCount = count + (5 + 1) * arrayLen + 2 - 1;
        if (newCount > buf.length) {
            expandCapacity(newCount);
        }

        buf[count++] = ARRAY_PRE;
        for (int i = 0; ; i++) {
            if (array[i]) {
                System.arraycopy(TRUE_VALUE, 0, buf, count, 4);
                count += 4;
            } else {
                System.arraycopy(FALSE_VALUE, 0, buf, count, 5);
                count += 5;
            }
            if (i == iMax) {
                buf[count++] = ARRAY_SUF;
                return;
            }
            buf[count++] = SEPARATOR;
        }
    }
}
