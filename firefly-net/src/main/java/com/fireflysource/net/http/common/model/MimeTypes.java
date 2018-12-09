package com.fireflysource.net.http.common.model;

import com.fireflysource.common.collection.trie.ArrayTrie;
import com.fireflysource.common.collection.trie.Trie;
import com.fireflysource.common.slf4j.LazyLogger;
import com.fireflysource.common.string.StringUtils;
import com.fireflysource.common.sys.SystemLogger;
import com.fireflysource.net.http.common.codec.PreEncodedHttpField;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MimeTypes {

    private static final LazyLogger log = SystemLogger.create(MimeTypes.class);

    private static final Map<String, String> DFT_MIME_MAP = new HashMap<>();
    private static final Map<String, String> INFERRED_ENCODINGS = new HashMap<>();
    private static final Map<String, String> ASSUMED_ENCODINGS = new HashMap<>();
    private static final Trie<Type> CACHE = new ArrayTrie<>(512);

    static {
        for (Type type : Type.values()) {
            CACHE.put(type.toString(), type);

            int charset = type.toString().indexOf(";charset=");
            if (charset > 0) {
                String alt = type.toString().replace(";charset=", "; charset=");
                CACHE.put(alt, type);
            }

            if (type.isAssumedCharset())
                ASSUMED_ENCODINGS.put(type.getValue(), type.getCharsetString());
        }

        String path = MimeTypes.class.getPackage().getName().replace('.', '/');
        String resourceName = path + "/mime.properties";
        try (InputStream stream = MimeTypes.class.getClassLoader().getResourceAsStream(resourceName)) {
            if (stream == null) {
                log.warn("Missing mime-type resource: {}", resourceName);
            } else {
                try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                    Properties props = new Properties();
                    props.load(reader);
                    props.stringPropertyNames().stream()
                         .filter(Objects::nonNull)
                         .forEach(x ->
                                 DFT_MIME_MAP.put(StringUtils.asciiToLowerCase(x), normalizeMimeType(props.getProperty(x))));

                    if (DFT_MIME_MAP.size() == 0) {
                        log.warn("Empty mime types at {}", resourceName);
                    } else if (DFT_MIME_MAP.size() < props.keySet().size()) {
                        log.warn("Duplicate or null mime-type extension in resource: {}", resourceName);
                    }
                } catch (IOException e) {
                    log.warn(e.toString());
                }

            }
        } catch (IOException e) {
            log.warn(e.toString());
        }

        resourceName = path + "/encoding.properties";
        try (InputStream stream = MimeTypes.class.getClassLoader().getResourceAsStream(resourceName)) {
            if (stream == null)
                log.warn("Missing encoding resource: {}", resourceName);
            else {
                try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                    Properties props = new Properties();
                    props.load(reader);
                    props.stringPropertyNames().stream()
                         .filter(Objects::nonNull)
                         .forEach(t ->
                         {
                             String charset = props.getProperty(t);
                             if (charset.startsWith("-"))
                                 ASSUMED_ENCODINGS.put(t, charset.substring(1));
                             else
                                 INFERRED_ENCODINGS.put(t, props.getProperty(t));
                         });

                    if (INFERRED_ENCODINGS.size() == 0) {
                        log.warn("Empty encodings at {}", resourceName);
                    } else if ((INFERRED_ENCODINGS.size() + ASSUMED_ENCODINGS.size()) < props.keySet().size()) {
                        log.warn("Null or duplicate encodings in resource: {}", resourceName);
                    }
                } catch (IOException e) {
                    log.warn(e.toString());
                }
            }
        } catch (IOException e) {
            log.warn(e.toString());
        }
    }

    private final Map<String, String> _mimeMap = new HashMap<>();

    /**
     * Constructor.
     */
    public MimeTypes() {
    }

    /**
     * Get the MIME type by filename extension.
     * Lookup only the static default mime map.
     *
     * @param filename A file name
     * @return MIME type matching the longest dot extension of the
     * file name.
     */
    public static String getDefaultMimeByExtension(String filename) {
        String type = null;

        if (filename != null) {
            int i = -1;
            while (type == null) {
                i = filename.indexOf(".", i + 1);

                if (i < 0 || i >= filename.length())
                    break;

                String ext = StringUtils.asciiToLowerCase(filename.substring(i + 1));
                type = DFT_MIME_MAP.get(ext);
            }
        }

        if (type == null) {
            type = DFT_MIME_MAP.get("*");
        }

        return type;
    }

    public static Set<String> getKnownMimeTypes() {
        return new HashSet<>(DFT_MIME_MAP.values());
    }

    private static String normalizeMimeType(String type) {
        Type t = CACHE.get(type);
        if (t != null)
            return t.getValue();

        return StringUtils.asciiToLowerCase(type);
    }

    public static String getCharsetFromContentType(String value) {
        if (value == null)
            return null;
        int end = value.length();
        int state = 0;
        int start = 0;
        boolean quote = false;
        int i = 0;
        for (; i < end; i++) {
            char b = value.charAt(i);

            if (quote && state != 10) {
                if ('"' == b)
                    quote = false;
                continue;
            }

            if (';' == b && state <= 8) {
                state = 1;
                continue;
            }

            switch (state) {
                case 0:
                    if ('"' == b) {
                        quote = true;
                        break;
                    }
                    break;

                case 1:
                    if ('c' == b) state = 2;
                    else if (' ' != b) state = 0;
                    break;
                case 2:
                    if ('h' == b) state = 3;
                    else state = 0;
                    break;
                case 3:
                    if ('a' == b) state = 4;
                    else state = 0;
                    break;
                case 4:
                    if ('r' == b) state = 5;
                    else state = 0;
                    break;
                case 5:
                    if ('s' == b) state = 6;
                    else state = 0;
                    break;
                case 6:
                    if ('e' == b) state = 7;
                    else state = 0;
                    break;
                case 7:
                    if ('t' == b) state = 8;
                    else state = 0;
                    break;

                case 8:
                    if ('=' == b) state = 9;
                    else if (' ' != b) state = 0;
                    break;

                case 9:
                    if (' ' == b)
                        break;
                    if ('"' == b) {
                        quote = true;
                        start = i + 1;
                        state = 10;
                        break;
                    }
                    start = i;
                    state = 10;
                    break;

                case 10:
                    if (!quote && (';' == b || ' ' == b) ||
                            (quote && '"' == b))
                        return StringUtils.normalizeCharset(value, start, i - start);
            }
        }

        if (state == 10)
            return StringUtils.normalizeCharset(value, start, i - start);

        return null;
    }

    /**
     * Access a mutable map of mime type to the charset inferred from that content type.
     * An inferred encoding is used by when encoding/decoding a stream and is
     * explicitly set in any metadata (eg Content-Type).
     *
     * @return Map of mime type to charset
     */
    public static Map<String, String> getInferredEncodings() {
        return INFERRED_ENCODINGS;
    }

    /**
     * Access a mutable map of mime type to the charset assumed for that content type.
     * An assumed encoding is used by when encoding/decoding a stream, but is not
     * explicitly set in any metadata (eg Content-Type).
     *
     * @return Map of mime type to charset
     */
    public static Map<String, String> getAssumedEncodings() {
        return INFERRED_ENCODINGS;
    }

    @Deprecated
    public static String inferCharsetFromContentType(String contentType) {
        return getCharsetAssumedFromContentType(contentType);
    }

    public static String getCharsetInferredFromContentType(String contentType) {
        return INFERRED_ENCODINGS.get(contentType);
    }

    public static String getCharsetAssumedFromContentType(String contentType) {
        return ASSUMED_ENCODINGS.get(contentType);
    }

    public static String getContentTypeWithoutCharset(String value) {
        int end = value.length();
        int state = 0;
        int start = 0;
        boolean quote = false;
        int i = 0;
        StringBuilder builder = null;
        for (; i < end; i++) {
            char b = value.charAt(i);

            if ('"' == b) {
                quote = !quote;

                switch (state) {
                    case 11:
                        builder.append(b);
                        break;
                    case 10:
                        break;
                    case 9:
                        builder = new StringBuilder();
                        builder.append(value, 0, start + 1);
                        state = 10;
                        break;
                    default:
                        start = i;
                        state = 0;
                }
                continue;
            }

            if (quote) {
                if (builder != null && state != 10)
                    builder.append(b);
                continue;
            }

            switch (state) {
                case 0:
                    if (';' == b)
                        state = 1;
                    else if (' ' != b)
                        start = i;
                    break;

                case 1:
                    if ('c' == b) state = 2;
                    else if (' ' != b) state = 0;
                    break;
                case 2:
                    if ('h' == b) state = 3;
                    else state = 0;
                    break;
                case 3:
                    if ('a' == b) state = 4;
                    else state = 0;
                    break;
                case 4:
                    if ('r' == b) state = 5;
                    else state = 0;
                    break;
                case 5:
                    if ('s' == b) state = 6;
                    else state = 0;
                    break;
                case 6:
                    if ('e' == b) state = 7;
                    else state = 0;
                    break;
                case 7:
                    if ('t' == b) state = 8;
                    else state = 0;
                    break;
                case 8:
                    if ('=' == b) state = 9;
                    else if (' ' != b) state = 0;
                    break;

                case 9:
                    if (' ' == b)
                        break;
                    builder = new StringBuilder();
                    builder.append(value, 0, start + 1);
                    state = 10;
                    break;

                case 10:
                    if (';' == b) {
                        builder.append(b);
                        state = 11;
                    }
                    break;
                case 11:
                    if (' ' != b)
                        builder.append(b);
            }
        }
        if (builder == null)
            return value;
        return builder.toString();

    }

    public static String getContentTypeMIMEType(String contentType) {
        if (contentType != null) {
            // parsing content-type
            String[] strings = StringUtils.split(contentType, ';');
            return strings[0];
        } else {
            return null;
        }
    }

    public static List<String> getAcceptMIMETypes(String accept) {
        if (accept != null) {
            List<String> list = new ArrayList<>();
            // parsing accept
            String[] strings = StringUtils.split(accept, ',');
            for (String string : strings) {
                String[] s = StringUtils.split(string, ';');
                list.add(s[0].trim());
            }
            return list;
        } else {
            return Collections.emptyList();
        }
    }

    public static List<AcceptMIMEType> parseAcceptMIMETypes(String accept) {
        return Optional.ofNullable(accept)
                       .filter(StringUtils::hasText)
                       .map(a -> StringUtils.split(a, ','))
                       .map(Arrays::stream)
                       .map(MimeTypes::apply)
                       .orElse(Collections.emptyList());
    }

    private static List<AcceptMIMEType> apply(Stream<String> stream) {
        return stream.map(String::trim)
                     .filter(StringUtils::hasText)
                     .map(type -> {
                         String[] mimeTypeAndQuality = StringUtils.split(type, ';');
                         AcceptMIMEType acceptMIMEType = new AcceptMIMEType();

                         // parse the MIME type
                         String[] mimeType = StringUtils.split(mimeTypeAndQuality[0].trim(), '/');
                         String parentType = mimeType[0].trim();
                         String childType = mimeType[1].trim();
                         acceptMIMEType.setParentType(parentType);
                         acceptMIMEType.setChildType(childType);
                         if (parentType.equals("*")) {
                             if (childType.equals("*")) {
                                 acceptMIMEType.setMatchType(AcceptMIMEMatchType.ALL);
                             } else {
                                 acceptMIMEType.setMatchType(AcceptMIMEMatchType.CHILD);
                             }
                         } else {
                             if (childType.equals("*")) {
                                 acceptMIMEType.setMatchType(AcceptMIMEMatchType.PARENT);
                             } else {
                                 acceptMIMEType.setMatchType(AcceptMIMEMatchType.EXACT);
                             }
                         }

                         // parse the quality
                         if (mimeTypeAndQuality.length > 1) {
                             String q = mimeTypeAndQuality[1];
                             String[] qualityKV = StringUtils.split(q, '=');
                             acceptMIMEType.setQuality(Float.parseFloat(qualityKV[1].trim()));
                         }

                         return acceptMIMEType;
                     })
                     .sorted((a1, a2) -> Float.compare(a2.getQuality(), a1.getQuality()))
                     .collect(Collectors.toList());
    }

    public synchronized Map<String, String> getMimeMap() {
        return _mimeMap;
    }

    /**
     * @param mimeMap A Map of file extension to mime-type.
     */
    public void setMimeMap(Map<String, String> mimeMap) {
        _mimeMap.clear();
        if (mimeMap != null) {
            for (Entry<String, String> ext : mimeMap.entrySet()) {
                _mimeMap.put(StringUtils.asciiToLowerCase(ext.getKey()), normalizeMimeType(ext.getValue()));
            }
        }
    }

    /**
     * Get the MIME type by filename extension.
     * Lookup the content and static default mime maps.
     *
     * @param filename A file name
     * @return MIME type matching the longest dot extension of the
     * file name.
     */
    public String getMimeByExtension(String filename) {
        String type = null;

        if (filename != null) {
            int i = -1;
            while (type == null) {
                i = filename.indexOf(".", i + 1);

                if (i < 0 || i >= filename.length())
                    break;

                String ext = StringUtils.asciiToLowerCase(filename.substring(i + 1));
                type = _mimeMap.get(ext);
                if (type == null)
                    type = DFT_MIME_MAP.get(ext);
            }
        }

        if (type == null) {
            type = _mimeMap.get("*");
            if (type == null) {
                type = DFT_MIME_MAP.get("*");
            }
        }

        return type;
    }

    /**
     * Set a mime mapping
     *
     * @param extension the extension
     * @param type      the mime type
     */
    public void addMimeMapping(String extension, String type) {
        _mimeMap.put(StringUtils.asciiToLowerCase(extension), normalizeMimeType(type));
    }

    public enum Type {
        FORM_ENCODED("application/x-www-form-urlencoded"),
        MESSAGE_HTTP("message/http"),
        MULTIPART_BYTERANGES("multipart/byteranges"),

        TEXT_HTML("text/html"),
        TEXT_PLAIN("text/plain"),
        TEXT_XML("text/xml"),
        TEXT_JSON("text/json", StandardCharsets.UTF_8),
        APPLICATION_JSON("application/json", StandardCharsets.UTF_8),

        TEXT_HTML_8859_1("text/html;charset=iso-8859-1", TEXT_HTML),
        TEXT_HTML_UTF_8("text/html;charset=utf-8", TEXT_HTML),

        TEXT_PLAIN_8859_1("text/plain;charset=iso-8859-1", TEXT_PLAIN),
        TEXT_PLAIN_UTF_8("text/plain;charset=utf-8", TEXT_PLAIN),

        TEXT_XML_8859_1("text/xml;charset=iso-8859-1", TEXT_XML),
        TEXT_XML_UTF_8("text/xml;charset=utf-8", TEXT_XML),

        TEXT_JSON_8859_1("text/json;charset=iso-8859-1", TEXT_JSON),
        TEXT_JSON_UTF_8("text/json;charset=utf-8", TEXT_JSON),

        APPLICATION_JSON_8859_1("application/json;charset=iso-8859-1", APPLICATION_JSON),
        APPLICATION_JSON_UTF_8("application/json;charset=utf-8", APPLICATION_JSON);


        private final String value;
        private final byte[] bytes;
        private final Type baseType;
        private final Charset charset;
        private final String charsetString;
        private final boolean assumedCharset;
        private final HttpField field;

        Type(String value) {
            this.value = value;
            bytes = StringUtils.getUtf8Bytes(value);
            baseType = this;
            charset = null;
            charsetString = null;
            assumedCharset = false;
            field = new PreEncodedHttpField(HttpHeader.CONTENT_TYPE, this.value);
        }

        Type(String value, Type baseType) {
            this.value = value;
            bytes = StringUtils.getUtf8Bytes(value);
            this.baseType = baseType;
            int i = value.indexOf(";charset=");
            charset = Charset.forName(value.substring(i + 9));
            charsetString = charset.toString().toLowerCase(Locale.ENGLISH);
            assumedCharset = false;
            field = new PreEncodedHttpField(HttpHeader.CONTENT_TYPE, this.value);
        }

        Type(String value, Charset charset) {
            this.value = value;
            bytes = StringUtils.getUtf8Bytes(value);
            baseType = this;
            this.charset = charset;
            charsetString = this.charset == null ? null : this.charset.toString().toLowerCase(Locale.ENGLISH);
            assumedCharset = true;
            field = new PreEncodedHttpField(HttpHeader.CONTENT_TYPE, this.value);
        }

        public Charset getCharset() {
            return charset;
        }

        public String getCharsetString() {
            return charsetString;
        }

        public boolean is(String s) {
            return value.equalsIgnoreCase(s);
        }

        public String getValue() {
            return value;
        }

        public byte[] getBytes() {
            return bytes;
        }

        @Override
        public String toString() {
            return value;
        }

        public boolean isAssumedCharset() {
            return assumedCharset;
        }

        public HttpField getContentTypeField() {
            return field;
        }

        public Type getBaseType() {
            return baseType;
        }
    }
}
