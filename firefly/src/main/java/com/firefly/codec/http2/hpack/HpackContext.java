package com.firefly.codec.http2.hpack;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.firefly.codec.http2.model.HttpField;
import com.firefly.codec.http2.model.HttpHeader;
import com.firefly.codec.http2.model.HttpMethod;
import com.firefly.codec.http2.model.HttpScheme;
import com.firefly.codec.http2.model.StaticTableHttpField;
import com.firefly.utils.StringUtils;
import com.firefly.utils.collection.ArrayQueue;
import com.firefly.utils.collection.ArrayTernaryTrie;
import com.firefly.utils.collection.Trie;
import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;

public class HpackContext {
	
	private static Log log = LogFactory.getInstance().getLog("firefly-system");
	
	public static final String[][] STATIC_TABLE = 
	    {
	        {null,null},
	        /* 1  */ {":authority",null},
	        /* 2  */ {":method","GET"},
	        /* 3  */ {":method","POST"},
	        /* 4  */ {":path","/"},
	        /* 5  */ {":path","/index.html"},
	        /* 6  */ {":scheme","http"},
	        /* 7  */ {":scheme","https"},
	        /* 8  */ {":status","200"},
	        /* 9  */ {":status","204"},
	        /* 10 */ {":status","206"},
	        /* 11 */ {":status","304"},
	        /* 12 */ {":status","400"},
	        /* 13 */ {":status","404"},
	        /* 14 */ {":status","500"},
	        /* 15 */ {"accept-charset",null},
	        /* 16 */ {"accept-encoding","gzip, deflate"},
	        /* 17 */ {"accept-language",null},
	        /* 18 */ {"accept-ranges",null},
	        /* 19 */ {"accept",null},
	        /* 20 */ {"access-control-allow-origin",null},
	        /* 21 */ {"age",null},
	        /* 22 */ {"allow",null},
	        /* 23 */ {"authorization",null},
	        /* 24 */ {"cache-control",null},
	        /* 25 */ {"content-disposition",null},
	        /* 26 */ {"content-encoding",null},
	        /* 27 */ {"content-language",null},
	        /* 28 */ {"content-length",null},
	        /* 29 */ {"content-location",null},
	        /* 30 */ {"content-range",null},
	        /* 31 */ {"content-type",null},
	        /* 32 */ {"cookie",null},
	        /* 33 */ {"date",null},
	        /* 34 */ {"etag",null},
	        /* 35 */ {"expect",null},
	        /* 36 */ {"expires",null},
	        /* 37 */ {"from",null},
	        /* 38 */ {"host",null},
	        /* 39 */ {"if-match",null},
	        /* 40 */ {"if-modified-since",null},
	        /* 41 */ {"if-none-match",null},
	        /* 42 */ {"if-range",null},
	        /* 43 */ {"if-unmodified-since",null},
	        /* 44 */ {"last-modified",null},
	        /* 45 */ {"link",null},
	        /* 46 */ {"location",null},
	        /* 47 */ {"max-forwards",null},
	        /* 48 */ {"proxy-authenticate",null},
	        /* 49 */ {"proxy-authorization",null},
	        /* 50 */ {"range",null},
	        /* 51 */ {"referer",null},
	        /* 52 */ {"refresh",null},
	        /* 53 */ {"retry-after",null},
	        /* 54 */ {"server",null},
	        /* 55 */ {"set-cookie",null},
	        /* 56 */ {"strict-transport-security",null},
	        /* 57 */ {"transfer-encoding",null},
	        /* 58 */ {"user-agent",null},
	        /* 59 */ {"vary",null},
	        /* 60 */ {"via",null},
	        /* 61 */ {"www-authenticate",null},
	    };
	
	private static final Map<HttpField,Entry> staticFieldMap = new HashMap<>();
    private static final Trie<StaticEntry> staticNameMap = new ArrayTernaryTrie<>(true,512);
    private static final StaticEntry[] staticTableByHeader = new StaticEntry[HttpHeader.UNKNOWN.ordinal()];
    private static final StaticEntry[] staticTable = new StaticEntry[STATIC_TABLE.length];
    
    static {
    	Set<String> added = new HashSet<>();
    	 for (int i = 1; i < STATIC_TABLE.length; i++) {
    		 StaticEntry entry = null;

             String name  = STATIC_TABLE[i][0];
             String value = STATIC_TABLE[i][1];
             HttpHeader header = HttpHeader.CACHE.get(name);
             if (header != null && value != null) {
            	 switch (header) {
            	 	case C_METHOD: {
            	 		HttpMethod method = HttpMethod.CACHE.get(value);
            	 		if (method != null) {
            	 			entry = new StaticEntry(i, new StaticTableHttpField(header,name,value,method));
            	 		}
            	 		break;
            	 	}
            	 	case C_SCHEME: {
                        HttpScheme scheme = HttpScheme.CACHE.get(value);
                        if (scheme != null) {
                            entry = new StaticEntry(i, new StaticTableHttpField(header,name,value,scheme));
                        }
                        break;
                    }
            	 	case C_STATUS: {
                        entry = new StaticEntry(i,new StaticTableHttpField(header,name,value,Integer.valueOf(value)));
                        break;
                    }
            	 	default:
            	 		break;
            	 }
             }
             
             
             if (entry == null) {
                 entry = new StaticEntry(i, header == null ? new HttpField(STATIC_TABLE[i][0], value) : new HttpField(header, name, value));
             }
             staticTable[i] = entry;
             
             if (entry.field.getValue() != null) {
                 staticFieldMap.put(entry.field,entry);
             }
             if (!added.contains(entry.field.getName())) {
                 added.add(entry.field.getName());
                 staticNameMap.put(entry.field.getName(),entry);
                 if (staticNameMap.get(entry.field.getName()) == null)
                     throw new IllegalStateException("name trie too small");
             }
    	 }
    	 
    	 for (HttpHeader h : HttpHeader.values()) {
             StaticEntry entry = staticNameMap.get(h.asString());
             if (entry!=null)
                 staticTableByHeader[h.ordinal()] = entry;
         }
    	
    }
    
    private int maxDynamicTableSizeInBytes;
    private int dynamicTableSizeInBytes;
    private final DynamicTable dynamicTable;
    private final Map<HttpField,Entry> fieldMap = new HashMap<>();
    private final Map<String,Entry> nameMap = new HashMap<>();
    
    public HpackContext(int maxDynamicTableSize) {
        this.maxDynamicTableSizeInBytes = maxDynamicTableSize;
        int guesstimateEntries = 10 + maxDynamicTableSize / (32 + 10 + 10);
        dynamicTable = new DynamicTable(guesstimateEntries,guesstimateEntries + 10);
        if(log.isDebugEnable())
        	log.debug("HdrTbl[{}] created max={}", hashCode(), maxDynamicTableSize);
    }
    
    public void resize(int newMaxDynamicTableSize) {
        if (log.isDebugEnable()) {
            log.debug("HdrTbl[{}] resized max={}->{}", hashCode(), maxDynamicTableSizeInBytes, newMaxDynamicTableSize);
        }
        maxDynamicTableSizeInBytes = newMaxDynamicTableSize;
        int guesstimateEntries = 10 + newMaxDynamicTableSize / (32 + 10 + 10);
        evict();
        dynamicTable.resizeUnsafe(guesstimateEntries);
    }
    
    public Entry get(HttpField field) {
        Entry entry = fieldMap.get(field);
        if (entry == null)
            entry = staticFieldMap.get(field);
        return entry;
    }
    
    public Entry get(String name) {
        Entry entry = staticNameMap.get(name);
        if (entry != null)
            return entry;
        return nameMap.get(StringUtils.asciiToLowerCase(name));
    }
    
    public Entry get(int index) {
        if (index < staticTable.length)
            return staticTable[index];
            
        int d = dynamicTable.size() - index + staticTable.length - 1;

        if (d >= 0) 
            return dynamicTable.getUnsafe(d);      
        return null;
    }
    
    public Entry get(HttpHeader header) {
        Entry e = staticTableByHeader[header.ordinal()];
        if (e == null)
            return get(header.asString());
        return e;
    }

    public static Entry getStatic(HttpHeader header) {
        return staticTableByHeader[header.ordinal()];
    }
    
    public Entry add(HttpField field) {
        int slot = dynamicTable.getNextSlotUnsafe();
        Entry entry = new Entry(slot,field);
        int size = entry.getSize();
        if (size > maxDynamicTableSizeInBytes) {
            if (log.isDebugEnable())
                log.debug("HdrTbl[{}] !added size {}>{}",hashCode(), size, maxDynamicTableSizeInBytes);
            return null;
        }
        dynamicTableSizeInBytes += size;
        dynamicTable.addUnsafe(entry);
        fieldMap.put(field,entry);
        nameMap.put(StringUtils.asciiToLowerCase(field.getName()),entry);

        if (log.isDebugEnable()) {
            log.debug("HdrTbl[{}] added {}", hashCode(), entry);
        }
        evict();
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
    public int getMaxDynamicTableSize()  {
        return maxDynamicTableSizeInBytes;
    }

    public int index(Entry entry) {
        if (entry.slot < 0)
            return 0;
        if (entry.isStatic())
            return entry.slot;

        return dynamicTable.index(entry) + staticTable.length - 1;
    }
    
    public static int staticIndex(HttpHeader header) {
        if (header == null)
            return 0;
        Entry entry = staticNameMap.get(header.asString());
        if (entry == null)
            return 0;
        return entry.getSlot();
    }
    
    private void evict() {
        while (dynamicTableSizeInBytes > maxDynamicTableSizeInBytes) {
            Entry entry = dynamicTable.pollUnsafe();
            if (log.isDebugEnable()) {
                log.debug("HdrTbl[{}] evict {}", hashCode(), entry);
            }
            dynamicTableSizeInBytes -= entry.getSize();
            entry.slot = -1;
            fieldMap.remove(entry.getHttpField());
            String lc = StringUtils.asciiToLowerCase(entry.getHttpField().getName());
            if (entry == nameMap.get(lc))
                nameMap.remove(lc);
        }
        if (log.isDebugEnable())
            log.debug("HdrTbl[{}] entries={}, size={}, max={}", hashCode(), dynamicTable.size(), dynamicTableSizeInBytes, maxDynamicTableSizeInBytes);
    }
    
    @Override
    public String toString() {
        return String.format("HpackContext@%x{entries=%d,size=%d,max=%d}", hashCode(), dynamicTable.size(), dynamicTableSizeInBytes, maxDynamicTableSizeInBytes);
    }
    

	private class DynamicTable extends ArrayQueue<HpackContext.Entry> {

        private DynamicTable(int initCapacity, int growBy) {
            super(initCapacity,growBy);
        }

        @Override
        protected void resizeUnsafe(int newCapacity) {
            // Relay on super.growUnsafe to pack all entries 0 to _nextSlot
            super.resizeUnsafe(newCapacity);
            for (int s=0;s<_nextSlot;s++)
                ((Entry)_elements[s]).slot=s;
        }

        @Override
        public boolean enqueue(Entry e) {
            return super.enqueue(e);
        }

        @Override
        public Entry dequeue() {
            return super.dequeue();
        }

		private int index(Entry entry) {
            return entry.slot >= _nextE ? _size - entry.slot + _nextE : _nextSlot - entry.slot;
        }

    }
	
	
	public static class Entry {
		final HttpField field;
        int slot;
        
        Entry(int index,String name, String value) {    
            slot = index;
            field = new HttpField(name,value);
        }
        
        Entry(int slot, HttpField field) {    
            this.slot = slot;
            this.field = field;
        }
        
        public int getSize() {
            return 32 + field.getName().length() + field.getValue().length();
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
        
        public int getSlot() {
            return slot;
        }
        
        @Override
        public String toString() {
            return String.format("{%s,%d,%s,%x}", isStatic() ? "S" : "D", slot, field, hashCode());
        }
	}
	
	public static class StaticEntry extends Entry {
        private final byte[] huffmanValue;
        private final byte encodedField;
        
        StaticEntry(int index, HttpField field) {    
            super(index,field);
            String value = field.getValue();
            if (value != null && value.length() > 0) {
                int huffmanLen = Huffman.octetsNeeded(value);
                int lenLen = NBitInteger.octectsNeeded(7, huffmanLen);
                huffmanValue = new byte[1 + lenLen + huffmanLen];
                ByteBuffer buffer = ByteBuffer.wrap(huffmanValue); 
                        
                // Indicate Huffman
                buffer.put((byte)0x80);
                // Add huffman length
                NBitInteger.encode(buffer,7,huffmanLen);
                // Encode value
                Huffman.encode(buffer,value);       
            } else {
                huffmanValue = null;
            }
            encodedField = (byte)(0x80|index);
        }

        @Override
        public boolean isStatic() {
            return true;
        }
        
        @Override
        public byte[] getStaticHuffmanValue() {
            return huffmanValue;
        }
        
        public byte getEncodedField() {
            return encodedField;
        }
    }
}
