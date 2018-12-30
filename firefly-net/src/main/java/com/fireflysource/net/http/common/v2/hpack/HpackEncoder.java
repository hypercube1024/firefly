package com.fireflysource.net.http.common.v2.hpack;

import com.fireflysource.common.collection.trie.ArrayTrie;
import com.fireflysource.common.collection.trie.Trie;
import com.fireflysource.common.object.TypeUtils;
import com.fireflysource.common.slf4j.LazyLogger;
import com.fireflysource.common.string.StringUtils;
import com.fireflysource.common.sys.SystemLogger;
import com.fireflysource.net.http.common.codec.PreEncodedHttpField;
import com.fireflysource.net.http.common.model.*;

import java.nio.ByteBuffer;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

public class HpackEncoder {

    final static EnumSet<HttpHeader> DO_NOT_HUFFMAN =
            EnumSet.of(
                    HttpHeader.AUTHORIZATION,
                    HttpHeader.CONTENT_MD5,
                    HttpHeader.PROXY_AUTHENTICATE,
                    HttpHeader.PROXY_AUTHORIZATION);
    final static EnumSet<HttpHeader> DO_NOT_INDEX =
            EnumSet.of(
                    // HttpHeader.C_PATH,  // TODO more data needed
                    // HttpHeader.DATE,    // TODO more data needed
                    HttpHeader.AUTHORIZATION,
                    HttpHeader.CONTENT_MD5,
                    HttpHeader.CONTENT_RANGE,
                    HttpHeader.ETAG,
                    HttpHeader.IF_MODIFIED_SINCE,
                    HttpHeader.IF_UNMODIFIED_SINCE,
                    HttpHeader.IF_NONE_MATCH,
                    HttpHeader.IF_RANGE,
                    HttpHeader.IF_MATCH,
                    HttpHeader.LOCATION,
                    HttpHeader.RANGE,
                    HttpHeader.RETRY_AFTER,
                    // HttpHeader.EXPIRES,
                    HttpHeader.LAST_MODIFIED,
                    HttpHeader.SET_COOKIE,
                    HttpHeader.SET_COOKIE2);
    final static EnumSet<HttpHeader> __NEVER_INDEX =
            EnumSet.of(
                    HttpHeader.AUTHORIZATION,
                    HttpHeader.SET_COOKIE,
                    HttpHeader.SET_COOKIE2);
    private static final LazyLogger log = SystemLogger.create(HpackEncoder.class);
    private final static HttpField[] HTTP_STATUS = new HttpField[599];
    private static final PreEncodedHttpField CONNECTION_TE = new PreEncodedHttpField(HttpHeader.CONNECTION, "te");
    private static final PreEncodedHttpField TE_TRAILERS = new PreEncodedHttpField(HttpHeader.TE, "trailers");
    private static final Trie<Boolean> specialHopHeaders = new ArrayTrie<>(6);

    static {
        for (HttpStatus.Code code : HttpStatus.Code.values())
            HTTP_STATUS[code.getCode()] = new PreEncodedHttpField(HttpHeader.C_STATUS, Integer.toString(code.getCode()));
        specialHopHeaders.put("close", true);
        specialHopHeaders.put("te", true);
    }

    private final HpackContext context;
    private final boolean debug;
    private int remoteMaxDynamicTableSize;
    private int localMaxDynamicTableSize;
    private int maxHeaderListSize;
    private int headerListSize;

    public HpackEncoder() {
        this(4096, 4096, -1);
    }

    public HpackEncoder(int localMaxDynamicTableSize) {
        this(localMaxDynamicTableSize, 4096, -1);
    }

    public HpackEncoder(int localMaxDynamicTableSize, int remoteMaxDynamicTableSize) {
        this(localMaxDynamicTableSize, remoteMaxDynamicTableSize, -1);
    }

    public HpackEncoder(int localMaxDynamicTableSize, int remoteMaxDynamicTableSize, int maxHeaderListSize) {
        context = new HpackContext(remoteMaxDynamicTableSize);
        this.remoteMaxDynamicTableSize = remoteMaxDynamicTableSize;
        this.localMaxDynamicTableSize = localMaxDynamicTableSize;
        this.maxHeaderListSize = maxHeaderListSize;
        debug = log.isDebugEnabled();
    }

    static void encodeValue(ByteBuffer buffer, boolean huffman, String value) {
        if (huffman) {
            // huffman literal value
            buffer.put((byte) 0x80);
            NBitInteger.encode(buffer, 7, Huffman.octetsNeeded(value));
            Huffman.encode(buffer, value);
        } else {
            // add literal assuming iso_8859_1
            buffer.put((byte) 0x00);
            NBitInteger.encode(buffer, 7, value.length());
            for (int i = 0; i < value.length(); i++) {
                char c = value.charAt(i);
                if (c < ' ' || c > 127)
                    throw new IllegalArgumentException();
                buffer.put((byte) c);
            }
        }
    }

    public int getMaxHeaderListSize() {
        return maxHeaderListSize;
    }

    public void setMaxHeaderListSize(int maxHeaderListSize) {
        this.maxHeaderListSize = maxHeaderListSize;
    }

    public HpackContext getHpackContext() {
        return context;
    }

    public void setRemoteMaxDynamicTableSize(int remoteMaxDynamicTableSize) {
        this.remoteMaxDynamicTableSize = remoteMaxDynamicTableSize;
    }

    public void setLocalMaxDynamicTableSize(int localMaxDynamicTableSize) {
        this.localMaxDynamicTableSize = localMaxDynamicTableSize;
    }

    public void encode(ByteBuffer buffer, MetaData metadata) {
        if (log.isDebugEnabled())
            log.debug(String.format("CtxTbl[%x] encoding", context.hashCode()));

        headerListSize = 0;
        int pos = buffer.position();

        // Check the dynamic table sizes!
        int maxDynamicTableSize = Math.min(remoteMaxDynamicTableSize, localMaxDynamicTableSize);
        if (maxDynamicTableSize != context.getMaxDynamicTableSize())
            encodeMaxDynamicTableSize(buffer, maxDynamicTableSize);

        // Add Request/response meta fields
        if (metadata.isRequest()) {
            MetaData.Request request = (MetaData.Request) metadata;

            // TODO optimise these to avoid HttpField creation
            String scheme = request.getURI().getScheme();
            encode(buffer, new HttpField(HttpHeader.C_SCHEME, scheme == null ? HttpScheme.HTTP.getValue() : scheme));
            encode(buffer, new HttpField(HttpHeader.C_METHOD, request.getMethod()));
            encode(buffer, new HttpField(HttpHeader.C_AUTHORITY, request.getURI().getAuthority()));
            encode(buffer, new HttpField(HttpHeader.C_PATH, request.getURI().getPathQuery()));
        } else if (metadata.isResponse()) {
            MetaData.Response response = (MetaData.Response) metadata;
            int code = response.getStatus();
            HttpField status = code < HTTP_STATUS.length ? HTTP_STATUS[code] : null;
            if (status == null)
                status = new HttpField.IntValueHttpField(HttpHeader.C_STATUS, code);
            encode(buffer, status);
        }

        // Add all non-connection fields.
        HttpFields fields = metadata.getFields();
        if (fields != null) {
            Set<String> hopHeaders = fields.getCSV(HttpHeader.CONNECTION, false).stream()
                                           .filter(v -> specialHopHeaders.get(v) == Boolean.TRUE)
                                           .map(StringUtils::asciiToLowerCase)
                                           .collect(Collectors.toSet());
            for (HttpField field : fields) {
                if (field.getHeader() == HttpHeader.CONNECTION)
                    continue;
                if (!hopHeaders.isEmpty() && hopHeaders.contains(StringUtils.asciiToLowerCase(field.getName())))
                    continue;
                if (field.getHeader() == HttpHeader.TE) {
                    if (!field.contains("trailers"))
                        continue;
                    encode(buffer, CONNECTION_TE);
                    encode(buffer, TE_TRAILERS);
                }
                encode(buffer, field);
            }
        }

        // Check size
        if (maxHeaderListSize > 0 && headerListSize > maxHeaderListSize) {
            log.warn("Header list size too large {} > {} for {}", headerListSize, maxHeaderListSize);
            if (log.isDebugEnabled())
                log.debug("metadata={}", metadata);
        }

        if (log.isDebugEnabled())
            log.debug(String.format("CtxTbl[%x] encoded %d octets", context.hashCode(), buffer.position() - pos));
    }

    public void encodeMaxDynamicTableSize(ByteBuffer buffer, int maxDynamicTableSize) {
        if (maxDynamicTableSize > remoteMaxDynamicTableSize)
            throw new IllegalArgumentException();
        buffer.put((byte) 0x20);
        NBitInteger.encode(buffer, 5, maxDynamicTableSize);
        context.resize(maxDynamicTableSize);
    }

    public void encode(ByteBuffer buffer, HttpField field) {
        if (field.getValue() == null)
            field = new HttpField(field.getHeader(), field.getName(), "");

        int field_size = field.getName().length() + field.getValue().length();
        headerListSize += field_size + 32;

        final int p = debug ? buffer.position() : -1;

        String encoding = null;

        // Is there an entry for the field?
        HpackContext.Entry entry = context.get(field);
        if (entry != null) {
            // Known field entry, so encode it as indexed
            if (entry.isStatic()) {
                buffer.put(((HpackContext.StaticEntry) entry).getEncodedField());
                if (debug)
                    encoding = "IdxFieldS1";
            } else {
                int index = context.index(entry);
                buffer.put((byte) 0x80);
                NBitInteger.encode(buffer, 7, index);
                if (debug)
                    encoding = "IdxField" + (entry.isStatic() ? "S" : "") + (1 + NBitInteger.octectsNeeded(7, index));
            }
        } else {
            // Unknown field entry, so we will have to send literally.
            final boolean indexed;

            // But do we know it's name?
            HttpHeader header = field.getHeader();

            // Select encoding strategy
            if (header == null) {
                // Select encoding strategy for unknown header names
                HpackContext.Entry name = context.get(field.getName());

                if (field instanceof PreEncodedHttpField) {
                    int i = buffer.position();
                    ((PreEncodedHttpField) field).putTo(buffer, HttpVersion.HTTP_2);
                    byte b = buffer.get(i);
                    indexed = b < 0 || b >= 0x40;
                    if (debug)
                        encoding = indexed ? "PreEncodedIdx" : "PreEncoded";
                }
                // has the custom header name been seen before?
                else if (name == null) {
                    // unknown name and value, so let's index this just in case it is
                    // the first time we have seen a custom name or a custom field.
                    // unless the name is changing, this is worthwhile
                    indexed = true;
                    encodeName(buffer, (byte) 0x40, 6, field.getName(), null);
                    encodeValue(buffer, true, field.getValue());
                    if (debug)
                        encoding = "LitHuffNHuffVIdx";
                } else {
                    // known custom name, but unknown value.
                    // This is probably a custom field with changing value, so don't index.
                    indexed = false;
                    encodeName(buffer, (byte) 0x00, 4, field.getName(), null);
                    encodeValue(buffer, true, field.getValue());
                    if (debug)
                        encoding = "LitHuffNHuffV!Idx";
                }
            } else {
                // Select encoding strategy for known header names
                HpackContext.Entry name = context.get(header);

                if (field instanceof PreEncodedHttpField) {
                    // Preencoded field
                    int i = buffer.position();
                    ((PreEncodedHttpField) field).putTo(buffer, HttpVersion.HTTP_2);
                    byte b = buffer.get(i);
                    indexed = b < 0 || b >= 0x40;
                    if (debug)
                        encoding = indexed ? "PreEncodedIdx" : "PreEncoded";
                } else if (DO_NOT_INDEX.contains(header)) {
                    // Non indexed field
                    indexed = false;
                    boolean never_index = __NEVER_INDEX.contains(header);
                    boolean huffman = !DO_NOT_HUFFMAN.contains(header);
                    encodeName(buffer, never_index ? (byte) 0x10 : (byte) 0x00, 4, header.getValue(), name);
                    encodeValue(buffer, huffman, field.getValue());

                    if (debug)
                        encoding = "Lit" +
                                ((name == null) ? "HuffN" : ("IdxN" + (name.isStatic() ? "S" : "") + (1 + NBitInteger.octectsNeeded(4, context.index(name))))) +
                                (huffman ? "HuffV" : "LitV") +
                                (never_index ? "!!Idx" : "!Idx");
                } else if (field_size >= context.getMaxDynamicTableSize() || header == HttpHeader.CONTENT_LENGTH && field.getValue().length() > 2) {
                    // Non indexed if field too large or a content length for 3 digits or more
                    indexed = false;
                    encodeName(buffer, (byte) 0x00, 4, header.getValue(), name);
                    encodeValue(buffer, true, field.getValue());
                    if (debug)
                        encoding = "LitIdxNS" + (1 + NBitInteger.octectsNeeded(4, context.index(name))) + "HuffV!Idx";
                } else {
                    // indexed
                    indexed = true;
                    boolean huffman = !DO_NOT_HUFFMAN.contains(header);
                    encodeName(buffer, (byte) 0x40, 6, header.getValue(), name);
                    encodeValue(buffer, huffman, field.getValue());
                    if (debug)
                        encoding = ((name == null) ? "LitHuffN" : ("LitIdxN" + (name.isStatic() ? "S" : "") + (1 + NBitInteger.octectsNeeded(6, context.index(name))))) +
                                (huffman ? "HuffVIdx" : "LitVIdx");
                }
            }

            // If we want the field referenced, then we add it to our table and reference set.
            if (indexed)
                context.add(field);
        }

        if (debug) {
            int e = buffer.position();
            if (log.isDebugEnabled())
                log.debug("encode {}:'{}' to '{}'", encoding, field, TypeUtils.toHexString(buffer.array(), buffer.arrayOffset() + p, e - p));
        }
    }

    private void encodeName(ByteBuffer buffer, byte mask, int bits, String name, HpackContext.Entry entry) {
        buffer.put(mask);
        if (entry == null) {
            // leave name index bits as 0
            // Encode the name always with lowercase huffman
            buffer.put((byte) 0x80);
            NBitInteger.encode(buffer, 7, Huffman.octetsNeededLC(name));
            Huffman.encodeLC(buffer, name);
        } else {
            NBitInteger.encode(buffer, bits, context.index(entry));
        }
    }
}
