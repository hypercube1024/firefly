package com.fireflysource.net.http.common.v2.hpack;

import com.fireflysource.net.http.common.model.*;

public class MetaDataBuilder {
    private final int maxSize;
    private int size;
    private Integer status;
    private String method;
    private HttpScheme scheme;
    private HostPortHttpField authority;
    private String path;
    private long contentLength = Long.MIN_VALUE;
    private HttpFields fields = new HttpFields();
    private HpackException.StreamException streamException;
    private boolean request;
    private boolean response;

    /**
     * @param maxHeadersSize The maximum size of the headers, expressed as total name and value characters.
     */
    protected MetaDataBuilder(int maxHeadersSize) {
        maxSize = maxHeadersSize;
    }

    /**
     * Get the maxSize.
     *
     * @return the maxSize
     */
    public int getMaxSize() {
        return maxSize;
    }

    /**
     * Get the size.
     *
     * @return the current size in bytes
     */
    public int getSize() {
        return size;
    }

    public void emit(HttpField field) throws HpackException.SessionException {
        HttpHeader header = field.getHeader();
        String name = field.getName();
        String value = field.getValue();
        int fieldSize = name.length() + (value == null ? 0 : value.length());
        size += fieldSize + 32;
        if (size > maxSize)
            throw new HpackException.SessionException("Header Size %d > %d", size, maxSize);

        if (field instanceof StaticTableHttpField) {
            StaticTableHttpField staticField = (StaticTableHttpField) field;
            switch (header) {
                case C_STATUS:
                    if (checkPseudoHeader(header, status))
                        status = (Integer) staticField.getStaticValue();
                    response = true;
                    break;

                case C_METHOD:
                    if (checkPseudoHeader(header, method))
                        method = value;
                    request = true;
                    break;

                case C_SCHEME:
                    if (checkPseudoHeader(header, scheme))
                        scheme = (HttpScheme) staticField.getStaticValue();
                    request = true;
                    break;

                default:
                    throw new IllegalArgumentException(name);
            }
        } else if (header != null) {
            switch (header) {
                case C_STATUS:
                    if (checkPseudoHeader(header, status))
                        status = field.getIntValue();
                    response = true;
                    break;

                case C_METHOD:
                    if (checkPseudoHeader(header, method))
                        method = value;
                    request = true;
                    break;

                case C_SCHEME:
                    if (checkPseudoHeader(header, scheme) && value != null)
                        scheme = HttpScheme.from(value);
                    request = true;
                    break;

                case C_AUTHORITY:
                    if (checkPseudoHeader(header, authority)) {
                        if (field instanceof HostPortHttpField)
                            authority = (HostPortHttpField) field;
                        else if (value != null)
                            authority = new AuthorityHttpField(value);
                    }
                    request = true;
                    break;

                case HOST:
                    // :authority fields must come first.  If we have one, ignore the host header as far as authority goes.
                    if (authority == null) {
                        if (field instanceof HostPortHttpField)
                            authority = (HostPortHttpField) field;
                        else if (value != null)
                            authority = new AuthorityHttpField(value);
                    }
                    fields.add(field);
                    break;

                case C_PATH:
                    if (checkPseudoHeader(header, path)) {
                        if (value != null && value.length() > 0)
                            path = value;
                        else
                            streamException("No Path");
                    }
                    request = true;
                    break;

                case CONTENT_LENGTH:
                    contentLength = field.getLongValue();
                    fields.add(field);
                    break;

                case TE:
                    if ("trailers".equalsIgnoreCase(value))
                        fields.add(field);
                    else
                        streamException("Unsupported TE value '%s'", value);
                    break;

                case CONNECTION:
                    if ("TE".equalsIgnoreCase(value))
                        fields.add(field);
                    else
                        streamException("Connection specific field '%s'", header);
                    break;

                default:
                    if (name.charAt(0) == ':')
                        streamException("Unknown pseudo header '%s'", name);
                    else
                        fields.add(field);
                    break;
            }
        } else {
            if (name.charAt(0) == ':')
                streamException("Unknown pseudo header '%s'", name);
            else
                fields.add(field);
        }
    }

    protected void streamException(String messageFormat, Object... args) {
        HpackException.StreamException stream = new HpackException.StreamException(messageFormat, args);
        if (streamException == null)
            streamException = stream;
        else
            streamException.addSuppressed(stream);
    }

    protected boolean checkPseudoHeader(HttpHeader header, Object value) {
        if (fields.size() > 0) {
            streamException("Pseudo header %s after fields", header.getValue());
            return false;
        }
        if (value == null)
            return true;
        streamException("Duplicate pseudo header %s", header.getValue());
        return false;
    }

    public MetaData build() throws HpackException.StreamException {
        if (streamException != null) {
            streamException.addSuppressed(new Throwable());
            throw streamException;
        }

        if (request && response)
            throw new HpackException.StreamException("Request and Response headers");

        HttpFields fields = this.fields;
        try {
            if (request) {
                if (method == null)
                    throw new HpackException.StreamException("No Method");
                if (scheme == null)
                    throw new HpackException.StreamException("No Scheme");
                if (path == null)
                    throw new HpackException.StreamException("No Path");
                return new MetaData.Request(method, scheme, authority, path, HttpVersion.HTTP_2, fields, contentLength);
            }
            if (response) {
                if (status == null)
                    throw new HpackException.StreamException("No Status");
                return new MetaData.Response(HttpVersion.HTTP_2, status, fields, contentLength);
            }

            return new MetaData(HttpVersion.HTTP_2, fields, contentLength);
        } finally {
            this.fields = new HttpFields(Math.max(16, fields.size() + 5));
            request = false;
            response = false;
            status = null;
            method = null;
            scheme = null;
            authority = null;
            path = null;
            size = 0;
            contentLength = Long.MIN_VALUE;
        }
    }

    /**
     * Check that the max size will not be exceeded.
     *
     * @param length  the length
     * @param huffman the huffman name
     * @throws HpackException.SessionException in case of size errors
     */
    public void checkSize(int length, boolean huffman) throws HpackException.SessionException {
        // Apply a huffman fudge factor
        if (huffman)
            length = (length * 4) / 3;
        if ((size + length) > maxSize)
            throw new HpackException.SessionException("Header too large %d > %d", size + length, maxSize);
    }
}
