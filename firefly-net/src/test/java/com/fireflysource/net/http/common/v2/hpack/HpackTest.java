package com.fireflysource.net.http.common.v2.hpack;

import com.fireflysource.net.http.common.codec.DateGenerator;
import com.fireflysource.net.http.common.codec.PreEncodedHttpField;
import com.fireflysource.net.http.common.model.*;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

import static com.fireflysource.net.http.common.model.MetaData.Response;
import static org.junit.jupiter.api.Assertions.*;


class HpackTest {

    final static HttpField ServerFirefly = new PreEncodedHttpField(HttpHeader.SERVER, "firefly");
    final static HttpField XPowerFirefly = new PreEncodedHttpField(HttpHeader.X_POWERED_BY, "firefly");
    final static HttpField Date = new PreEncodedHttpField(HttpHeader.DATE, DateGenerator.formatDate(TimeUnit.NANOSECONDS.toMillis(System.nanoTime())));

    @Test
    void encodeDecodeResponseTest() {
        HpackEncoder encoder = new HpackEncoder();
        HpackDecoder decoder = new HpackDecoder(4096, 8192);
        ByteBuffer buffer = ByteBuffer.allocateDirect(16 * 1024);

        HttpFields fields0 = new HttpFields();
        fields0.add(HttpHeader.CONTENT_TYPE, "text/html");
        fields0.add(HttpHeader.CONTENT_LENGTH, "1024");
        fields0.add(new HttpField(HttpHeader.CONTENT_ENCODING, (String) null));
        fields0.add(ServerFirefly);
        fields0.add(XPowerFirefly);
        fields0.add(Date);
        fields0.add(HttpHeader.SET_COOKIE, "abcdefghijklmnopqrstuvwxyz");
        fields0.add("custom-key", "custom-value");
        MetaData.Response original0 = new MetaData.Response(HttpVersion.HTTP_2, 200, fields0);

        buffer.clear();
        encoder.encode(buffer, original0);
        buffer.flip();
        Response decoded0 = (Response) decoder.decode(buffer);
        original0.getFields().put(new HttpField(HttpHeader.CONTENT_ENCODING, ""));
        assertMetadataSame(original0, decoded0);

        // Same again?
        buffer.clear();
        encoder.encode(buffer, original0);
        buffer.flip();
        Response decoded0b = (Response) decoder.decode(buffer);

        assertMetadataSame(original0, decoded0b);

        HttpFields fields1 = new HttpFields();
        fields1.add(HttpHeader.CONTENT_TYPE, "text/plain");
        fields1.add(HttpHeader.CONTENT_LENGTH, "1234");
        fields1.add(HttpHeader.CONTENT_ENCODING, " ");
        fields1.add(ServerFirefly);
        fields1.add(XPowerFirefly);
        fields1.add(Date);
        fields1.add("Custom-Key", "Other-Value");
        Response original1 = new MetaData.Response(HttpVersion.HTTP_2, 200, fields1);

        // Same again?
        buffer.clear();
        encoder.encode(buffer, original1);
        buffer.flip();
        Response decoded1 = (Response) decoder.decode(buffer);

        assertMetadataSame(original1, decoded1);
        assertEquals("custom-key", decoded1.getFields().getField("Custom-Key").getName());
    }

    @Test
    void encodeDecodeTooLargeTest() {
        HpackEncoder encoder = new HpackEncoder();
        HpackDecoder decoder = new HpackDecoder(4096, 164);
        ByteBuffer buffer = ByteBuffer.allocateDirect(16 * 1024);

        HttpFields fields0 = new HttpFields();
        fields0.add("1234567890", "1234567890123456789012345678901234567890");
        fields0.add("Cookie", "abcdeffhijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQR");
        MetaData original0 = new MetaData(HttpVersion.HTTP_2, fields0);

        buffer.clear();
        encoder.encode(buffer, original0);
        buffer.flip();
        MetaData decoded0 = (MetaData) decoder.decode(buffer);

        assertMetadataSame(original0, decoded0);

        HttpFields fields1 = new HttpFields();
        fields1.add("1234567890", "1234567890123456789012345678901234567890");
        fields1.add("Cookie", "abcdeffhijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQR");
        fields1.add("x", "y");
        MetaData original1 = new MetaData(HttpVersion.HTTP_2, fields1);

        buffer.clear();
        encoder.encode(buffer, original1);
        buffer.flip();
        try {
            decoder.decode(buffer);
            fail();
        } catch (HpackException.SessionException e) {
            assertTrue(e.getMessage().contains("Header too large"));
        }
    }

    @Test
    void evictReferencedFieldTest() {
        HpackEncoder encoder = new HpackEncoder(200, 200);
        HpackDecoder decoder = new HpackDecoder(200, 1024);
        ByteBuffer buffer = ByteBuffer.allocate(16 * 1024);

        HttpFields fields0 = new HttpFields();
        fields0.add("123456789012345678901234567890123456788901234567890", "value");
        fields0.add("foo", "abcdeffhijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQR");
        MetaData original0 = new MetaData(HttpVersion.HTTP_2, fields0);

        buffer.clear();
        encoder.encode(buffer, original0);
        buffer.flip();
        MetaData decoded0 = (MetaData) decoder.decode(buffer);

        assertEquals(2, encoder.getHpackContext().size());
        assertEquals(2, decoder.getHpackContext().size());
        assertEquals("123456789012345678901234567890123456788901234567890", encoder.getHpackContext().get(HpackContext.STATIC_TABLE.length + 1).getHttpField().getName());
        assertEquals("foo", encoder.getHpackContext().get(HpackContext.STATIC_TABLE.length + 0).getHttpField().getName());

        assertMetadataSame(original0, decoded0);

        HttpFields fields1 = new HttpFields();
        fields1.add("123456789012345678901234567890123456788901234567890", "other_value");
        fields1.add("x", "y");
        MetaData original1 = new MetaData(HttpVersion.HTTP_2, fields1);

        buffer.clear();
        encoder.encode(buffer, original1);
        buffer.flip();
        MetaData decoded1 = (MetaData) decoder.decode(buffer);
        assertMetadataSame(original1, decoded1);

        assertEquals(2, encoder.getHpackContext().size());
        assertEquals(2, decoder.getHpackContext().size());
        assertEquals("x", encoder.getHpackContext().get(HpackContext.STATIC_TABLE.length).getHttpField().getName());
        assertEquals("foo", encoder.getHpackContext().get(HpackContext.STATIC_TABLE.length + 1).getHttpField().getName());
    }

    private void assertMetadataSame(MetaData.Response expected, MetaData.Response actual) {
        assertEquals(expected.getStatus(), actual.getStatus());
        assertEquals(expected.getReason(), actual.getReason());
        assertMetadataSame((MetaData) expected, (MetaData) actual);
    }

    private void assertMetadataSame(MetaData expected, MetaData actual) {
        assertEquals(expected.getContentLength(), actual.getContentLength());
        assertEquals(expected.getHttpVersion(), actual.getHttpVersion());
        assertHttpFieldsSame("Metadata.fields", expected.getFields(), actual.getFields());
    }

    private void assertHttpFieldsSame(String message, HttpFields expected, HttpFields actual) {
        assertEquals(expected.size(), actual.size(), message);

        for (HttpField actualField : actual) {
            if ("DATE".equalsIgnoreCase(actualField.getName())) {
                // skip comparison on Date, as these values can often differ by 1 second
                // during testing.
                continue;
            }
            assertTrue(expected.contains(actualField), message + ".contains(" + actualField + ")");
        }
    }
}
