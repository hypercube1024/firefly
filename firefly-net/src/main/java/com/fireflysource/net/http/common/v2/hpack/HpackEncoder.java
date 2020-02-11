package com.fireflysource.net.http.common.v2.hpack;

import com.fireflysource.common.object.TypeUtils;
import com.fireflysource.common.slf4j.LazyLogger;
import com.fireflysource.common.string.StringUtils;
import com.fireflysource.common.sys.SystemLogger;
import com.fireflysource.net.http.common.codec.PreEncodedHttpField;
import com.fireflysource.net.http.common.model.*;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import static com.fireflysource.net.http.common.v2.hpack.HpackContext.Entry;
import static com.fireflysource.net.http.common.v2.hpack.HpackContext.StaticEntry;

public class HpackEncoder {
    private static final LazyLogger LOG = SystemLogger.create(HpackEncoder.class);

    private static final HttpField[] STATUSES = new HttpField[599];
    static final EnumSet<HttpHeader> DO_NOT_HUFFMAN = EnumSet.of(
            HttpHeader.AUTHORIZATION,
            HttpHeader.CONTENT_MD5,
            HttpHeader.PROXY_AUTHENTICATE,
            HttpHeader.PROXY_AUTHORIZATION);
    static final EnumSet<HttpHeader> DO_NOT_INDEX = EnumSet.of(
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
    static final EnumSet<HttpHeader> NEVER_INDEX = EnumSet.of(
            HttpHeader.AUTHORIZATION,
            HttpHeader.SET_COOKIE,
            HttpHeader.SET_COOKIE2);
    private static final EnumSet<HttpHeader> IGNORED_HEADERS = EnumSet.of(HttpHeader.CONNECTION, HttpHeader.KEEP_ALIVE,
            HttpHeader.PROXY_CONNECTION, HttpHeader.TRANSFER_ENCODING, HttpHeader.UPGRADE);
    private static final PreEncodedHttpField TE_TRAILERS = new PreEncodedHttpField(HttpHeader.TE, "trailers");
    private static final PreEncodedHttpField C_SCHEME_HTTP = new PreEncodedHttpField(HttpHeader.C_SCHEME, "http");
    private static final PreEncodedHttpField C_SCHEME_HTTPS = new PreEncodedHttpField(HttpHeader.C_SCHEME, "https");
    private static final EnumMap<HttpMethod, PreEncodedHttpField> C_METHODS = new EnumMap<>(HttpMethod.class);

    static {
        for (HttpStatus.Code code : HttpStatus.Code.values()) {
            STATUSES[code.getCode()] = new PreEncodedHttpField(HttpHeader.C_STATUS, Integer.toString(code.getCode()));
        }
        for (HttpMethod method : HttpMethod.values()) {
            C_METHODS.put(method, new PreEncodedHttpField(HttpHeader.C_METHOD, method.getValue()));
        }
    }

    private final HpackContext context;
    private final boolean debug;
    private int remoteMaxDynamicTableSize;
    private int localMaxDynamicTableSize;
    private int maxHeaderListSize;
    private int headerListSize;
    private boolean validateEncoding = true;

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
        debug = LOG.isDebugEnabled();
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

    public boolean isValidateEncoding() {
        return validateEncoding;
    }

    public void setValidateEncoding(boolean validateEncoding) {
        this.validateEncoding = validateEncoding;
    }

    public void encode(ByteBuffer buffer, MetaData metadata) throws HpackException {
        try {
            if (LOG.isDebugEnabled())
                LOG.debug(String.format("CtxTbl[%x] encoding", context.hashCode()));

            HttpFields fields = metadata.getFields();
            // Verify that we can encode without errors.
            if (isValidateEncoding() && fields != null) {
                for (HttpField field : fields) {
                    String name = field.getName();
                    char firstChar = name.charAt(0);
                    if (firstChar <= ' ' || firstChar == ':')
                        throw new HpackException.StreamException("Invalid header name: '%s'", name);
                }
            }

            headerListSize = 0;
            int pos = buffer.position();

            // Check the dynamic table sizes!
            int maxDynamicTableSize = Math.min(remoteMaxDynamicTableSize, localMaxDynamicTableSize);
            if (maxDynamicTableSize != context.getMaxDynamicTableSize())
                encodeMaxDynamicTableSize(buffer, maxDynamicTableSize);

            // Add Request/response meta fields
            if (!metadata.isOnlyTrailer()) {
                if (metadata.isRequest()) {
                    MetaData.Request request = (MetaData.Request) metadata;

                    String scheme = request.getURI().getScheme();
                    encode(buffer, HttpScheme.HTTPS.is(scheme) ? C_SCHEME_HTTPS : C_SCHEME_HTTP);
                    String method = request.getMethod();
                    HttpMethod httpMethod = method == null ? null : HttpMethod.from(method);
                    HttpField methodField = C_METHODS.get(httpMethod);
                    encode(buffer, methodField == null ? new HttpField(HttpHeader.C_METHOD, method) : methodField);
                    encode(buffer, new HttpField(HttpHeader.C_AUTHORITY, request.getURI().getAuthority()));
                    encode(buffer, new HttpField(HttpHeader.C_PATH, request.getURI().getPathQuery()));
                } else if (metadata.isResponse()) {
                    MetaData.Response response = (MetaData.Response) metadata;
                    int code = response.getStatus();
                    HttpField status = code < STATUSES.length ? STATUSES[code] : null;
                    if (status == null)
                        status = new HttpField.IntValueHttpField(HttpHeader.C_STATUS, code);
                    encode(buffer, status);
                }
            }

            // Remove fields as specified in RFC 7540, 8.1.2.2.
            if (fields != null) {
                // For example: Connection: Close, TE, Upgrade, Custom.
                Set<String> hopHeaders = null;
                for (String value : fields.getCSV(HttpHeader.CONNECTION, false)) {
                    if (hopHeaders == null)
                        hopHeaders = new HashSet<>();
                    hopHeaders.add(StringUtils.asciiToLowerCase(value));
                }
                for (HttpField field : fields) {
                    HttpHeader header = field.getHeader();
                    if (header != null && IGNORED_HEADERS.contains(header))
                        continue;
                    if (header == HttpHeader.TE) {
                        if (field.contains("trailers"))
                            encode(buffer, TE_TRAILERS);
                        continue;
                    }
                    String name = field.getLowerCaseName();
                    if (hopHeaders != null && hopHeaders.contains(name))
                        continue;
                    encode(buffer, field);
                }
            }

            // Check size
            if (maxHeaderListSize > 0 && headerListSize > maxHeaderListSize) {
                LOG.warn("Header list size too large {} > {} for {}", headerListSize, maxHeaderListSize);
                if (LOG.isDebugEnabled())
                    LOG.debug("metadata={}", metadata);
            }

            if (LOG.isDebugEnabled())
                LOG.debug(String.format("CtxTbl[%x] encoded %d octets", context.hashCode(), buffer.position() - pos));
        } catch (HpackException x) {
            throw x;
        } catch (Throwable x) {
            HpackException.SessionException failure = new HpackException.SessionException("Could not hpack encode %s", metadata);
            failure.initCause(x);
            throw failure;
        }
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

        int fieldSize = field.getName().length() + field.getValue().length();
        headerListSize += fieldSize + 32;

        final int p = debug ? buffer.position() : -1;

        String encoding = null;

        // Is there an entry for the field?
        Entry entry = context.get(field);
        if (entry != null) {
            // Known field entry, so encode it as indexed
            if (entry.isStatic()) {
                buffer.put(((StaticEntry) entry).getEncodedField());
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
                Entry name = context.get(field.getName());

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
                Entry name = context.get(header);

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
                    boolean neverIndex = NEVER_INDEX.contains(header);
                    boolean huffman = !DO_NOT_HUFFMAN.contains(header);
                    encodeName(buffer, neverIndex ? (byte) 0x10 : (byte) 0x00, 4, header.getValue(), name);
                    encodeValue(buffer, huffman, field.getValue());

                    if (debug)
                        encoding = "Lit" +
                                ((name == null) ? "HuffN" : ("IdxN" + (name.isStatic() ? "S" : "") + (1 + NBitInteger.octectsNeeded(4, context.index(name))))) +
                                (huffman ? "HuffV" : "LitV") +
                                (neverIndex ? "!!Idx" : "!Idx");
                } else if (fieldSize >= context.getMaxDynamicTableSize() || header == HttpHeader.CONTENT_LENGTH && field.getValue().length() > 2) {
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
            if (LOG.isDebugEnabled())
                LOG.debug("encode {}:'{}' to '{}'", encoding, field, TypeUtils.toHexString(buffer.array(), buffer.arrayOffset() + p, e - p));
        }
    }

    private void encodeName(ByteBuffer buffer, byte mask, int bits, String name, Entry entry) {
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

    static void encodeValue(ByteBuffer buffer, boolean huffman, String value) {
        if (huffman) {
            // huffman literal value
            buffer.put((byte) 0x80);

            int needed = Huffman.octetsNeeded(value);
            if (needed >= 0) {
                NBitInteger.encode(buffer, 7, needed);
                Huffman.encode(buffer, value);
            } else {
                // Not iso_8859_1
                byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
                NBitInteger.encode(buffer, 7, Huffman.octetsNeeded(bytes));
                Huffman.encode(buffer, bytes);
            }
        } else {
            // add literal assuming iso_8859_1
            buffer.put((byte) 0x00).mark();
            NBitInteger.encode(buffer, 7, value.length());
            for (int i = 0; i < value.length(); i++) {
                char c = value.charAt(i);
                if (c < ' ' || c > 127) {
                    // Not iso_8859_1, so re-encode as UTF-8
                    buffer.reset();
                    byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
                    NBitInteger.encode(buffer, 7, bytes.length);
                    buffer.put(bytes, 0, bytes.length);
                    return;
                }
                buffer.put((byte) c);
            }
        }
    }
}
