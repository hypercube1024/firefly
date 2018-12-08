package com.fireflysource.net.http.model;

import com.fireflysource.net.http.v2.hpack.HpackFieldPreEncoder;

import java.nio.ByteBuffer;
import java.util.ServiceLoader;

/**
 * Pre encoded HttpField.
 * <p>A HttpField that will be cached and used many times can be created as
 * a {@link PreEncodedHttpField}, which will use the {@link HttpFieldPreEncoder}
 * instances discovered by the {@link ServiceLoader} to pre-encode the header
 * for each version of HTTP in use.  This will save garbage
 * and CPU each time the field is encoded into a response.
 * </p>
 */
public class PreEncodedHttpField extends HttpField {
    private final static HttpFieldPreEncoder[] ENCODERS = new HttpFieldPreEncoder[]{
            new HpackFieldPreEncoder(),
            new Http1FieldPreEncoder()};

    private final byte[][] encodedField = new byte[2][];

    public PreEncodedHttpField(HttpHeader header, String name, String value) {
        super(header, name, value);

        for (HttpFieldPreEncoder e : ENCODERS) {
            encodedField[e.getHttpVersion() == HttpVersion.HTTP_2 ? 1 : 0] = e.getEncodedField(header, header.getValue(), value);
        }
    }

    public PreEncodedHttpField(HttpHeader header, String value) {
        this(header, header.getValue(), value);
    }

    public PreEncodedHttpField(String name, String value) {
        this(null, name, value);
    }

    public void putTo(ByteBuffer bufferInFillMode, HttpVersion version) {
        bufferInFillMode.put(encodedField[version == HttpVersion.HTTP_2 ? 1 : 0]);
    }
}