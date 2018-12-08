package com.fireflysource.net.http.v2.hpack;

import com.fireflysource.common.collection.trie.ArrayTernaryTrie;
import com.fireflysource.common.collection.trie.Trie;
import com.fireflysource.common.slf4j.LazyLogger;
import com.fireflysource.common.string.StringUtils;
import com.fireflysource.common.sys.SystemLogger;
import com.fireflysource.net.http.model.HttpField;
import com.fireflysource.net.http.model.HttpHeader;
import com.fireflysource.net.http.model.HttpMethod;
import com.fireflysource.net.http.model.HttpScheme;

import java.nio.ByteBuffer;
import java.util.*;

/**
 * HPACK - Header Compression for HTTP/2
 * <p>
 * This class maintains the compression context for a single HTTP/2 connection.
 * Specifically it holds the static and dynamic Header Field Tables and the
 * associated sizes and limits.
 * </p>
 * <p>
 * It is compliant with draft 11 of the specification
 * </p>
 */
public class HpackContext {

    static final int STATIC_SIZE;
    private static final LazyLogger log = SystemLogger.create(HpackContext.class);
    private static final String EMPTY = "";
    public static final String[][] STATIC_TABLE = {
            {null, null},
            /* 1  */ {":authority", EMPTY},
            /* 2  */ {":method", "GET"},
            /* 3  */ {":method", "POST"},
            /* 4  */ {":path", "/"},
            /* 5  */ {":path", "/index.html"},
            /* 6  */ {":scheme", "http"},
            /* 7  */ {":scheme", "https"},
            /* 8  */ {":status", "200"},
            /* 9  */ {":status", "204"},
            /* 10 */ {":status", "206"},
            /* 11 */ {":status", "304"},
            /* 12 */ {":status", "400"},
            /* 13 */ {":status", "404"},
            /* 14 */ {":status", "500"},
            /* 15 */ {"accept-charset", EMPTY},
            /* 16 */ {"accept-encoding", "gzip, deflate"},
            /* 17 */ {"accept-language", EMPTY},
            /* 18 */ {"accept-ranges", EMPTY},
            /* 19 */ {"accept", EMPTY},
            /* 20 */ {"access-control-allow-origin", EMPTY},
            /* 21 */ {"age", EMPTY},
            /* 22 */ {"allow", EMPTY},
            /* 23 */ {"authorization", EMPTY},
            /* 24 */ {"cache-control", EMPTY},
            /* 25 */ {"content-disposition", EMPTY},
            /* 26 */ {"content-encoding", EMPTY},
            /* 27 */ {"content-language", EMPTY},
            /* 28 */ {"content-length", EMPTY},
            /* 29 */ {"content-location", EMPTY},
            /* 30 */ {"content-range", EMPTY},
            /* 31 */ {"content-type", EMPTY},
            /* 32 */ {"cookie", EMPTY},
            /* 33 */ {"date", EMPTY},
            /* 34 */ {"etag", EMPTY},
            /* 35 */ {"expect", EMPTY},
            /* 36 */ {"expires", EMPTY},
            /* 37 */ {"from", EMPTY},
            /* 38 */ {"host", EMPTY},
            /* 39 */ {"if-match", EMPTY},
            /* 40 */ {"if-modified-since", EMPTY},
            /* 41 */ {"if-none-match", EMPTY},
            /* 42 */ {"if-range", EMPTY},
            /* 43 */ {"if-unmodified-since", EMPTY},
            /* 44 */ {"last-modified", EMPTY},
            /* 45 */ {"link", EMPTY},
            /* 46 */ {"location", EMPTY},
            /* 47 */ {"max-forwards", EMPTY},
            /* 48 */ {"proxy-authenticate", EMPTY},
            /* 49 */ {"proxy-authorization", EMPTY},
            /* 50 */ {"range", EMPTY},
            /* 51 */ {"referer", EMPTY},
            /* 52 */ {"refresh", EMPTY},
            /* 53 */ {"retry-after", EMPTY},
            /* 54 */ {"server", EMPTY},
            /* 55 */ {"set-cookie", EMPTY},
            /* 56 */ {"strict-transport-security", EMPTY},
            /* 57 */ {"transfer-encoding", EMPTY},
            /* 58 */ {"user-agent", EMPTY},
            /* 59 */ {"vary", EMPTY},
            /* 60 */ {"via", EMPTY},
            /* 61 */ {"www-authenticate", EMPTY},
    };
    private static final Map<HttpField, Entry> STATIC_FIELD_MAP = new HashMap<>();
    private static final Trie<StaticEntry> STATIC_NAME_MAP = new ArrayTernaryTrie<>(true, 512);
    private static final StaticEntry[] STATIC_TABLE_BY_HEADER = new StaticEntry[HttpHeader.UNKNOWN.ordinal()];
    private static final StaticEntry[] STATIC_TABLE_ENTRIES = new StaticEntry[STATIC_TABLE.length];

    static {
        STATIC_SIZE = STATIC_TABLE.length - 1;
        Set<String> added = new HashSet<>();
        for (int i = 1; i < STATIC_TABLE.length; i++) {
            StaticEntry entry = null;

            String name = STATIC_TABLE[i][0];
            String value = STATIC_TABLE[i][1];
            HttpHeader header = HttpHeader.CACHE.get(name);
            if (header != null && value != null) {
                switch (header) {
                    case C_METHOD: {

                        HttpMethod method = HttpMethod.from(value);
                        if (method != null)
                            entry = new StaticEntry(i, new StaticTableHttpField(header, name, value, method));
                        break;
                    }

                    case C_SCHEME: {

                        HttpScheme scheme = HttpScheme.from(value);
                        if (scheme != null)
                            entry = new StaticEntry(i, new StaticTableHttpField(header, name, value, scheme));
                        break;
                    }

                    case C_STATUS: {
                        entry = new StaticEntry(i, new StaticTableHttpField(header, name, value, Integer.valueOf(value)));
                        break;
                    }

                    default:
                        break;
                }
            }

            if (entry == null)
                entry = new StaticEntry(i, header == null ? new HttpField(STATIC_TABLE[i][0], value) : new HttpField(header, name, value));


            STATIC_TABLE_ENTRIES[i] = entry;

            if (entry.field.getValue() != null)
                STATIC_FIELD_MAP.put(entry.field, entry);

            if (!added.contains(entry.field.getName())) {
                added.add(entry.field.getName());
                STATIC_NAME_MAP.put(entry.field.getName(), entry);
                if (STATIC_NAME_MAP.get(entry.field.getName()) == null)
                    throw new IllegalStateException("name trie too small");
            }
        }

        for (HttpHeader h : HttpHeader.values()) {
            StaticEntry entry = STATIC_NAME_MAP.get(h.getValue());
            if (entry != null)
                STATIC_TABLE_BY_HEADER[h.ordinal()] = entry;
        }
    }

    private final DynamicTable dynamicTable;
    private final Map<HttpField, Entry> fieldMap = new HashMap<>();
    private final Map<String, Entry> nameMap = new HashMap<>();
    private int maxDynamicTableSizeInBytes;
    private int dynamicTableSizeInBytes;

    public HpackContext(int maxDynamicTableSize) {
        maxDynamicTableSizeInBytes = maxDynamicTableSize;
        int guesstimateEntries = 10 + maxDynamicTableSize / (32 + 10 + 10);
        dynamicTable = new DynamicTable(guesstimateEntries);
        if (log.isDebugEnabled())
            log.debug(String.format("HdrTbl[%x] created max=%d", hashCode(), maxDynamicTableSize));
    }

    public static Entry getStatic(HttpHeader header) {
        return STATIC_TABLE_BY_HEADER[header.ordinal()];
    }

    public static int staticIndex(HttpHeader header) {
        if (header == null)
            return 0;
        Entry entry = STATIC_NAME_MAP.get(header.getValue());
        if (entry == null)
            return 0;
        return entry.slot;
    }

    public void resize(int newMaxDynamicTableSize) {
        if (log.isDebugEnabled())
            log.debug(String.format("HdrTbl[%x] resized max=%d->%d", hashCode(), maxDynamicTableSizeInBytes, newMaxDynamicTableSize));
        maxDynamicTableSizeInBytes = newMaxDynamicTableSize;
        dynamicTable.evict();
    }

    public Entry get(HttpField field) {
        Entry entry = fieldMap.get(field);
        if (entry == null)
            entry = STATIC_FIELD_MAP.get(field);
        return entry;
    }

    public Entry get(String name) {
        Entry entry = STATIC_NAME_MAP.get(name);
        if (entry != null)
            return entry;
        return nameMap.get(StringUtils.asciiToLowerCase(name));
    }

    public Entry get(int index) {
        if (index <= STATIC_SIZE)
            return STATIC_TABLE_ENTRIES[index];

        return dynamicTable.get(index);
    }

    public Entry get(HttpHeader header) {
        Entry e = STATIC_TABLE_BY_HEADER[header.ordinal()];
        if (e == null)
            return get(header.getValue());
        return e;
    }

    public Entry add(HttpField field) {
        Entry entry = new Entry(field);
        int size = entry.getSize();
        if (size > maxDynamicTableSizeInBytes) {
            if (log.isDebugEnabled())
                log.debug(String.format("HdrTbl[%x] !added size %d>%d", hashCode(), size, maxDynamicTableSizeInBytes));
            dynamicTable.evictAll();
            return null;
        }
        dynamicTableSizeInBytes += size;
        dynamicTable.add(entry);
        fieldMap.put(field, entry);
        nameMap.put(StringUtils.asciiToLowerCase(field.getName()), entry);

        if (log.isDebugEnabled())
            log.debug(String.format("HdrTbl[%x] added %s", hashCode(), entry));
        dynamicTable.evict();
        return entry;
    }

    /**
     * @return Current dynamic table size in entries
     */
    public int size() {
        return dynamicTable.size();
    }

    /**
     * @return Current Dynamic table size in Octets
     */
    public int getDynamicTableSize() {
        return dynamicTableSizeInBytes;
    }

    /**
     * @return Max Dynamic table size in Octets
     */
    public int getMaxDynamicTableSize() {
        return maxDynamicTableSizeInBytes;
    }

    public int index(Entry entry) {
        if (entry.slot < 0)
            return 0;
        if (entry.isStatic())
            return entry.slot;

        return dynamicTable.index(entry);
    }

    @Override
    public String toString() {
        return String.format("HpackContext@%x{entries=%d,size=%d,max=%d}", hashCode(), dynamicTable.size(), dynamicTableSizeInBytes, maxDynamicTableSizeInBytes);
    }

    public static class Entry {
        final HttpField field;
        int slot; // The index within it's array

        Entry() {
            slot = -1;
            field = null;
        }

        Entry(HttpField field) {
            this.field = field;
        }

        public int getSize() {
            String value = field.getValue();
            return 32 + field.getName().length() + (value == null ? 0 : value.length());
        }

        public HttpField getHttpField() {
            return field;
        }

        public boolean isStatic() {
            return false;
        }

        public byte[] getStaticHuffmanValue() {
            return null;
        }

        @Override
        public String toString() {
            return String.format("{%s,%d,%s,%x}", isStatic() ? "S" : "D", slot, field, hashCode());
        }
    }

    public static class StaticEntry extends Entry {
        private final byte[] _huffmanValue;
        private final byte _encodedField;

        StaticEntry(int index, HttpField field) {
            super(field);
            slot = index;
            String value = field.getValue();
            if (value != null && value.length() > 0) {
                int huffmanLen = Huffman.octetsNeeded(value);
                int lenLen = NBitInteger.octectsNeeded(7, huffmanLen);
                _huffmanValue = new byte[1 + lenLen + huffmanLen];
                ByteBuffer buffer = ByteBuffer.wrap(_huffmanValue);

                // Indicate Huffman
                buffer.put((byte) 0x80);
                // Add huffman length
                NBitInteger.encode(buffer, 7, huffmanLen);
                // Encode value
                Huffman.encode(buffer, value);
            } else
                _huffmanValue = null;

            _encodedField = (byte) (0x80 | index);
        }

        @Override
        public boolean isStatic() {
            return true;
        }

        @Override
        public byte[] getStaticHuffmanValue() {
            return _huffmanValue;
        }

        public byte getEncodedField() {
            return _encodedField;
        }
    }

    private class DynamicTable {
        Entry[] entries;
        int size;
        int offset;
        int growby;

        private DynamicTable(int initCapacity) {
            entries = new Entry[initCapacity];
            growby = initCapacity;
        }

        public void add(Entry entry) {
            if (size == entries.length) {
                Entry[] entries = new Entry[this.entries.length + growby];
                for (int i = 0; i < size; i++) {
                    int slot = (offset + i) % this.entries.length;
                    entries[i] = this.entries[slot];
                    entries[i].slot = i;
                }
                this.entries = entries;
                offset = 0;
            }
            int slot = (size++ + offset) % entries.length;
            entries[slot] = entry;
            entry.slot = slot;
        }

        public int index(Entry entry) {
            return STATIC_SIZE + size - (entry.slot - offset + entries.length) % entries.length;
        }

        public Entry get(int index) {
            int d = index - STATIC_SIZE - 1;
            if (d < 0 || d >= size)
                return null;
            int slot = (offset + size - d - 1) % entries.length;
            return entries[slot];
        }

        public int size() {
            return size;
        }

        private void evict() {
            while (dynamicTableSizeInBytes > maxDynamicTableSizeInBytes) {
                Entry entry = entries[offset];
                entries[offset] = null;
                offset = (offset + 1) % entries.length;
                size--;
                if (log.isDebugEnabled())
                    log.debug(String.format("HdrTbl[%x] evict %s", HpackContext.this.hashCode(), entry));
                dynamicTableSizeInBytes -= entry.getSize();
                entry.slot = -1;
                fieldMap.remove(entry.getHttpField());
                String lc = StringUtils.asciiToLowerCase(entry.getHttpField().getName());
                if (entry == nameMap.get(lc))
                    nameMap.remove(lc);

            }
            if (log.isDebugEnabled())
                log.debug(String.format("HdrTbl[%x] entries=%d, size=%d, max=%d", HpackContext.this.hashCode(), dynamicTable.size(), dynamicTableSizeInBytes, maxDynamicTableSizeInBytes));
        }

        private void evictAll() {
            if (log.isDebugEnabled())
                log.debug(String.format("HdrTbl[%x] evictAll", HpackContext.this.hashCode()));
            fieldMap.clear();
            nameMap.clear();
            offset = 0;
            size = 0;
            dynamicTableSizeInBytes = 0;
            Arrays.fill(entries, null);
        }
    }
}
