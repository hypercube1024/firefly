package test.codec.http2.hpack;

import com.firefly.codec.http2.hpack.HpackContext;
import com.firefly.codec.http2.hpack.HpackDecoder;
import com.firefly.codec.http2.hpack.HpackEncoder;
import com.firefly.codec.http2.hpack.HpackException;
import com.firefly.codec.http2.model.*;
import com.firefly.codec.http2.model.MetaData.Response;
import com.firefly.utils.io.BufferUtils;
import org.junit.Assert;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class HpackTest {

    final static HttpField ServerFirefly = new PreEncodedHttpField(HttpHeader.SERVER, "firefly");
    final static HttpField XPowerFirefly = new PreEncodedHttpField(HttpHeader.X_POWERED_BY, "firefly");
    final static HttpField Date = new PreEncodedHttpField(HttpHeader.DATE, DateGenerator.formatDate(TimeUnit.NANOSECONDS.toMillis(System.nanoTime())));

    @Test
    public void encodeDecodeResponseTest() throws Exception {
        HpackEncoder encoder = new HpackEncoder();
        HpackDecoder decoder = new HpackDecoder(4096, 8192);
        ByteBuffer buffer = BufferUtils.allocate(16 * 1024);

        HttpFields fields0 = new HttpFields();
        fields0.add(HttpHeader.CONTENT_TYPE, "text/html");
        fields0.add(HttpHeader.CONTENT_LENGTH, "1024");
        fields0.add(new HttpField(HttpHeader.CONTENT_ENCODING, (String) null));
        fields0.add(ServerFirefly);
        fields0.add(XPowerFirefly);
        fields0.add(Date);
        fields0.add(HttpHeader.SET_COOKIE, "abcdefghijklmnopqrstuvwxyz");
        fields0.add("custom-key", "custom-value");
        Response original0 = new MetaData.Response(HttpVersion.HTTP_2, 200, fields0);

        BufferUtils.clearToFill(buffer);
        encoder.encode(buffer, original0);
        BufferUtils.flipToFlush(buffer, 0);
        Response decoded0 = (Response) decoder.decode(buffer);
        original0.getFields().put(new HttpField(HttpHeader.CONTENT_ENCODING, ""));
        assertMetadataSame(original0, decoded0);

        // Same again?
        BufferUtils.clearToFill(buffer);
        encoder.encode(buffer, original0);
        BufferUtils.flipToFlush(buffer, 0);
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
        BufferUtils.clearToFill(buffer);
        encoder.encode(buffer, original1);
        BufferUtils.flipToFlush(buffer, 0);
        Response decoded1 = (Response) decoder.decode(buffer);

        assertMetadataSame(original1, decoded1);
        Assert.assertEquals("custom-key", decoded1.getFields().getField("Custom-Key").getName());
    }

    @Test
    public void encodeDecodeTooLargeTest() throws Exception {
        HpackEncoder encoder = new HpackEncoder();
        HpackDecoder decoder = new HpackDecoder(4096, 164);
        ByteBuffer buffer = BufferUtils.allocate(16 * 1024);

        HttpFields fields0 = new HttpFields();
        fields0.add("1234567890", "1234567890123456789012345678901234567890");
        fields0.add("Cookie", "abcdeffhijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQR");
        MetaData original0 = new MetaData(HttpVersion.HTTP_2, fields0);

        BufferUtils.clearToFill(buffer);
        encoder.encode(buffer, original0);
        BufferUtils.flipToFlush(buffer, 0);
        MetaData decoded0 = (MetaData) decoder.decode(buffer);

        assertMetadataSame(original0, decoded0);

        HttpFields fields1 = new HttpFields();
        fields1.add("1234567890", "1234567890123456789012345678901234567890");
        fields1.add("Cookie", "abcdeffhijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQR");
        fields1.add("x", "y");
        MetaData original1 = new MetaData(HttpVersion.HTTP_2, fields1);

        BufferUtils.clearToFill(buffer);
        encoder.encode(buffer, original1);
        BufferUtils.flipToFlush(buffer, 0);
        try {
            decoder.decode(buffer);
            Assert.fail();
        } catch (HpackException.SessionException e) {
            assertThat(e.getMessage(), containsString("Header too large"));
        }
    }

    @Test
    public void evictReferencedFieldTest() throws Exception {
        HpackEncoder encoder = new HpackEncoder(200, 200);
        HpackDecoder decoder = new HpackDecoder(200, 1024);
        ByteBuffer buffer = BufferUtils.allocate(16 * 1024);

        HttpFields fields0 = new HttpFields();
        fields0.add("123456789012345678901234567890123456788901234567890", "value");
        fields0.add("foo", "abcdeffhijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQR");
        MetaData original0 = new MetaData(HttpVersion.HTTP_2, fields0);

        BufferUtils.clearToFill(buffer);
        encoder.encode(buffer, original0);
        BufferUtils.flipToFlush(buffer, 0);
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

        BufferUtils.clearToFill(buffer);
        encoder.encode(buffer, original1);
        BufferUtils.flipToFlush(buffer, 0);
        MetaData decoded1 = (MetaData) decoder.decode(buffer);
        assertMetadataSame(original1, decoded1);

        assertEquals(2, encoder.getHpackContext().size());
        assertEquals(2, decoder.getHpackContext().size());
        assertEquals("x", encoder.getHpackContext().get(HpackContext.STATIC_TABLE.length + 0).getHttpField().getName());
        assertEquals("foo", encoder.getHpackContext().get(HpackContext.STATIC_TABLE.length + 1).getHttpField().getName());
    }

    private void assertMetadataSame(MetaData.Response expected, MetaData.Response actual) {
        assertThat("Response.status", actual.getStatus(), is(expected.getStatus()));
        assertThat("Response.reason", actual.getReason(), is(expected.getReason()));
        assertMetadataSame((MetaData) expected, (MetaData) actual);
    }

    private void assertMetadataSame(MetaData expected, MetaData actual) {
        assertThat("Metadata.contentLength", actual.getContentLength(), is(expected.getContentLength()));
        assertThat("Metadata.version" + ".version", actual.getHttpVersion(), is(expected.getHttpVersion()));
        assertHttpFieldsSame("Metadata.fields", expected.getFields(), actual.getFields());
    }

    private void assertHttpFieldsSame(String msg, HttpFields expected, HttpFields actual) {
        assertThat(msg + ".size", actual.size(), is(expected.size()));

        for (HttpField actualField : actual) {
            if ("DATE".equalsIgnoreCase(actualField.getName())) {
                // skip comparison on Date, as these values can often differ by 1 second
                // during testing.
                continue;
            }
            assertThat(msg + ".contains(" + actualField + ")", expected.contains(actualField), is(true));
        }
    }
}
