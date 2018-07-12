package com.firefly.codec.http2.hpack;

import com.firefly.codec.http2.model.*;

public class MetaDataBuilder {
    private final int _maxSize;
    private int _size;
    private int _status;
    private String _method;
    private HttpScheme _scheme;
    private HostPortHttpField _authority;
    private String _path;
    private long _contentLength = Long.MIN_VALUE;
    private HttpFields _fields = new HttpFields(10);

    /**
     * @param maxHeadersSize The maximum size of the headers, expressed as total name and value characters.
     */
    MetaDataBuilder(int maxHeadersSize) {
        _maxSize = maxHeadersSize;
    }

    /**
     * Get the maxSize.
     *
     * @return the maxSize
     */
    public int getMaxSize() {
        return _maxSize;
    }

    /**
     * Get the size.
     *
     * @return the current size in bytes
     */
    public int getSize() {
        return _size;
    }

    public void emit(HttpField field) {
        HttpHeader header = field.getHeader();
        String name = field.getName();
        String value = field.getValue();
        int field_size = name.length() + (value == null ? 0 : value.length());
        _size += field_size + 32;
        if (_size > _maxSize)
            throw new BadMessageException(HttpStatus.REQUEST_HEADER_FIELDS_TOO_LARGE_431, "Header size " + _size + ">" + _maxSize);

        if (field instanceof StaticTableHttpField) {
            StaticTableHttpField staticField = (StaticTableHttpField) field;
            switch (header) {
                case C_STATUS:
                    _status = (Integer) staticField.getStaticValue();
                    break;

                case C_METHOD:
                    _method = value;
                    break;

                case C_SCHEME:
                    _scheme = (HttpScheme) staticField.getStaticValue();
                    break;

                default:
                    throw new IllegalArgumentException(name);
            }
        } else if (header != null) {
            switch (header) {
                case C_STATUS:
                    _status = field.getIntValue();
                    break;

                case C_METHOD:
                    _method = value;
                    break;

                case C_SCHEME:
                    if (value != null)
                        _scheme = HttpScheme.CACHE.get(value);
                    break;

                case C_AUTHORITY:
                    if (field instanceof HostPortHttpField)
                        _authority = (HostPortHttpField) field;
                    else if (value != null)
                        _authority = new AuthorityHttpField(value);
                    break;

                case HOST:
                    // :authority fields must come first.  If we have one, ignore the host header as far as authority goes.
                    if (_authority == null) {
                        if (field instanceof HostPortHttpField)
                            _authority = (HostPortHttpField) field;
                        else if (value != null)
                            _authority = new AuthorityHttpField(value);
                    }
                    _fields.add(field);
                    break;

                case C_PATH:
                    _path = value;
                    break;

                case CONTENT_LENGTH:
                    _contentLength = field.getLongValue();
                    _fields.add(field);
                    break;

                default:
                    if (name.charAt(0) != ':')
                        _fields.add(field);
                    break;
            }
        } else {
            if (name.charAt(0) != ':')
                _fields.add(field);
        }
    }

    public MetaData build() {
        try {
            HttpFields fields = _fields;
            _fields = new HttpFields(Math.max(10, fields.size() + 5));

            if (_method != null)
                return new MetaData.Request(_method, _scheme, _authority, _path, HttpVersion.HTTP_2, fields, _contentLength);
            if (_status != 0)
                return new MetaData.Response(HttpVersion.HTTP_2, _status, fields, _contentLength);
            if (_path != null)
                fields.put(HttpHeader.C_PATH, _path);
            if (_authority != null)
                fields.put(HttpHeader.HOST, _authority.getValue());

            return new MetaData(HttpVersion.HTTP_2, fields, _contentLength);
        } finally {
            _status = 0;
            _method = null;
            _scheme = null;
            _authority = null;
            _path = null;
            _size = 0;
            _contentLength = Long.MIN_VALUE;
        }
    }

    /**
     * Check that the max size will not be exceeded.
     *
     * @param length  the length
     * @param huffman the huffman name
     */
    public void checkSize(int length, boolean huffman) {
        // Apply a huffman fudge factor
        if (huffman)
            length = (length * 4) / 3;
        if ((_size + length) > _maxSize)
            throw new BadMessageException(HttpStatus.REQUEST_HEADER_FIELDS_TOO_LARGE_431, "Header size " + (_size + length) + ">" + _maxSize);
    }
}
