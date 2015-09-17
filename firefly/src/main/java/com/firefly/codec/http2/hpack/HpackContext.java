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
import com.firefly.utils.collection.ArrayQueue;
import com.firefly.utils.collection.ArrayTernaryTrie;
import com.firefly.utils.collection.Trie;

public class HpackContext {
	
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
    private static final StaticEntry[] staticTable=new StaticEntry[STATIC_TABLE.length];
    
    static {
    	Set<String> added = new HashSet<>();
    	 for (int i = 1; i < STATIC_TABLE.length; i++) {
    		 StaticEntry entry = null;

             String name  = STATIC_TABLE[i][0];
             String value = STATIC_TABLE[i][1];
             HttpHeader header = HttpHeader.CACHE.get(name);
             if (header!=null && value!=null) {
            	 switch (header) {
            	 	case C_METHOD: {
            	 		HttpMethod method = HttpMethod.CACHE.get(value);
            	 		if (method!=null) {
            	 			entry=new StaticEntry(i, new StaticTableHttpField(header,name,value,method));
            	 		}
            	 		break;
            	 	}
            	 	case C_SCHEME: {
                        HttpScheme scheme = HttpScheme.CACHE.get(value);
                        if (scheme!=null)
                            entry=new StaticEntry(i, new StaticTableHttpField(header,name,value,scheme));
                        break;
                    }
            	 	default:
            	 		break;
            	 }
             }
    	 }
    	
    }
	
	@SuppressWarnings("unused")
	private class DynamicTable extends ArrayQueue<HpackContext.Entry> {

        private DynamicTable(int initCapacity, int growBy) {
            super(initCapacity,growBy);
        }

        @Override
        protected void resizeUnsafe(int newCapacity) {
            // Relay on super.growUnsafe to pack all entries 0 to _nextSlot
            super.resizeUnsafe(newCapacity);
            for (int s=0;s<_nextSlot;s++)
                ((Entry)_elements[s])._slot=s;
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
            return entry._slot >= _nextE ? _size - entry._slot + _nextE : _nextSlot - entry._slot;
        }

    }
	
	
	public static class Entry {
		final HttpField _field;
        int _slot;
        
        Entry(int index,String name, String value) {    
            _slot=index;
            _field=new HttpField(name,value);
        }
        
        Entry(int slot, HttpField field) {    
            _slot=slot;
            _field=field;
        }
        
        public int getSize() {
            return 32+_field.getName().length()+_field.getValue().length();
        }
        
        public HttpField getHttpField() {
            return _field;
        }
        
        public boolean isStatic() {
            return false;
        }
        
        public byte[] getStaticHuffmanValue() {
            return null;
        }
        
        public int getSlot() {
            return _slot;
        }
        
        @Override
        public String toString() {
            return String.format("{%s,%d,%s,%x}",isStatic()?"S":"D",_slot,_field,hashCode());
        }
	}
	
	public static class StaticEntry extends Entry {
        private final byte[] _huffmanValue;
        private final byte _encodedField;
        
        StaticEntry(int index,HttpField field) {    
            super(index,field);
            String value = field.getValue();
            if (value!=null && value.length()>0)
            {
                int huffmanLen = Huffman.octetsNeeded(value);
                int lenLen = NBitInteger.octectsNeeded(7,huffmanLen);
                _huffmanValue = new byte[1+lenLen+huffmanLen];
                ByteBuffer buffer = ByteBuffer.wrap(_huffmanValue); 
                        
                // Indicate Huffman
                buffer.put((byte)0x80);
                // Add huffman length
                NBitInteger.encode(buffer,7,huffmanLen);
                // Encode value
                Huffman.encode(buffer,value);       
            }
            else
                _huffmanValue=null;
            
            _encodedField=(byte)(0x80|index);
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
}
