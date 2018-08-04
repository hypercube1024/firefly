package test.codec.http2.hpack;

import com.firefly.codec.http2.hpack.HpackContext;
import com.firefly.codec.http2.hpack.HpackEncoder;
import com.firefly.codec.http2.model.HttpField;
import com.firefly.codec.http2.model.HttpFields;
import com.firefly.codec.http2.model.HttpVersion;
import com.firefly.codec.http2.model.MetaData;
import com.firefly.utils.io.BufferUtils;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;

import java.nio.ByteBuffer;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

public class HpackEncoderTest {
    @Test
    public void testUnknownFieldsContextManagement() {
        HpackEncoder encoder = new HpackEncoder(38 * 5);
        HttpFields fields = new HttpFields();


        HttpField[] field =
                {
                        new HttpField("fo0", "b0r"),
                        new HttpField("fo1", "b1r"),
                        new HttpField("fo2", "b2r"),
                        new HttpField("fo3", "b3r"),
                        new HttpField("fo4", "b4r"),
                        new HttpField("fo5", "b5r"),
                        new HttpField("fo6", "b6r"),
                        new HttpField("fo7", "b7r"),
                        new HttpField("fo8", "b8r"),
                        new HttpField("fo9", "b9r"),
                        new HttpField("foA", "bAr"),
                };

        // Add 4 entries
        for (int i = 0; i <= 3; i++)
            fields.add(field[i]);

        // encode them
        ByteBuffer buffer = BufferUtils.allocate(4096);
        int pos = BufferUtils.flipToFill(buffer);
        encoder.encode(buffer, new MetaData(HttpVersion.HTTP_2, fields));
        BufferUtils.flipToFlush(buffer, pos);

        // something was encoded!
        assertThat(buffer.remaining(), Matchers.greaterThan(0));

        // All are in the dynamic table
        Assert.assertEquals(4, encoder.getHpackContext().size());

        // encode exact same fields again!
        BufferUtils.clearToFill(buffer);
        encoder.encode(buffer, new MetaData(HttpVersion.HTTP_2, fields));
        BufferUtils.flipToFlush(buffer, 0);

        // All are in the dynamic table
        Assert.assertEquals(4, encoder.getHpackContext().size());

        // Add 4 more fields
        for (int i = 4; i <= 7; i++)
            fields.add(field[i]);

        // encode
        BufferUtils.clearToFill(buffer);
        encoder.encode(buffer, new MetaData(HttpVersion.HTTP_2, fields));
        BufferUtils.flipToFlush(buffer, 0);

        // something was encoded!
        assertThat(buffer.remaining(), Matchers.greaterThan(0));

        // max dynamic table size reached
        Assert.assertEquals(5, encoder.getHpackContext().size());


        // remove some fields
        for (int i = 0; i <= 7; i += 2)
            fields.remove(field[i].getName());

        // encode
        BufferUtils.clearToFill(buffer);
        encoder.encode(buffer, new MetaData(HttpVersion.HTTP_2, fields));
        BufferUtils.flipToFlush(buffer, 0);

        // something was encoded!
        assertThat(buffer.remaining(), Matchers.greaterThan(0));

        // max dynamic table size reached
        Assert.assertEquals(5, encoder.getHpackContext().size());


        // remove another fields
        fields.remove(field[1].getName());

        // encode
        BufferUtils.clearToFill(buffer);
        encoder.encode(buffer, new MetaData(HttpVersion.HTTP_2, fields));
        BufferUtils.flipToFlush(buffer, 0);

        // something was encoded!
        assertThat(buffer.remaining(), Matchers.greaterThan(0));

        // max dynamic table size reached
        Assert.assertEquals(5, encoder.getHpackContext().size());


        // re add the field

        fields.add(field[1]);

        // encode
        BufferUtils.clearToFill(buffer);
        encoder.encode(buffer, new MetaData(HttpVersion.HTTP_2, fields));
        BufferUtils.flipToFlush(buffer, 0);

        // something was encoded!
        assertThat(buffer.remaining(), Matchers.greaterThan(0));

        // max dynamic table size reached
        Assert.assertEquals(5, encoder.getHpackContext().size());

    }


    @Test
    public void testNeverIndexSetCookie() {
        HpackEncoder encoder = new HpackEncoder(38 * 5);
        ByteBuffer buffer = BufferUtils.allocate(4096);

        HttpFields fields = new HttpFields();
        fields.put("set-cookie", "some cookie value");

        // encode
        BufferUtils.clearToFill(buffer);
        encoder.encode(buffer, new MetaData(HttpVersion.HTTP_2, fields));
        BufferUtils.flipToFlush(buffer, 0);

        // something was encoded!
        assertThat(buffer.remaining(), Matchers.greaterThan(0));

        // empty dynamic table
        Assert.assertEquals(0, encoder.getHpackContext().size());


        // encode again
        BufferUtils.clearToFill(buffer);
        encoder.encode(buffer, new MetaData(HttpVersion.HTTP_2, fields));
        BufferUtils.flipToFlush(buffer, 0);

        // something was encoded!
        assertThat(buffer.remaining(), Matchers.greaterThan(0));

        // empty dynamic table
        Assert.assertEquals(0, encoder.getHpackContext().size());

    }


    @Test
    public void testFieldLargerThanTable() {
        HttpFields fields = new HttpFields();

        HpackEncoder encoder = new HpackEncoder(128);
        ByteBuffer buffer0 = BufferUtils.allocate(4096);
        int pos = BufferUtils.flipToFill(buffer0);
        encoder.encode(buffer0, new MetaData(HttpVersion.HTTP_2, fields));
        BufferUtils.flipToFlush(buffer0, pos);

        encoder = new HpackEncoder(128);
        fields.add(new HttpField("user-agent", "jetty/test"));
        ByteBuffer buffer1 = BufferUtils.allocate(4096);
        pos = BufferUtils.flipToFill(buffer1);
        encoder.encode(buffer1, new MetaData(HttpVersion.HTTP_2, fields));
        BufferUtils.flipToFlush(buffer1, pos);

        encoder = new HpackEncoder(128);
        fields.add(new HttpField(":path",
                "This is a very large field, whose size is larger than the dynamic table so it should not be indexed as it will not fit in the table ever!" +
                        "XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX " +
                        "YYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYY " +
                        "ZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZ "));
        ByteBuffer buffer2 = BufferUtils.allocate(4096);
        pos = BufferUtils.flipToFill(buffer2);
        encoder.encode(buffer2, new MetaData(HttpVersion.HTTP_2, fields));
        BufferUtils.flipToFlush(buffer2, pos);

        encoder = new HpackEncoder(128);
        fields.add(new HttpField("host", "somehost"));
        ByteBuffer buffer = BufferUtils.allocate(4096);
        pos = BufferUtils.flipToFill(buffer);
        encoder.encode(buffer, new MetaData(HttpVersion.HTTP_2, fields));
        BufferUtils.flipToFlush(buffer, pos);

        //System.err.println(BufferUtils.toHexString(buffer0));
        //System.err.println(BufferUtils.toHexString(buffer1));
        //System.err.println(BufferUtils.toHexString(buffer2));
        //System.err.println(BufferUtils.toHexString(buffer));

        // something was encoded!
        assertThat(buffer.remaining(), Matchers.greaterThan(0));

        // check first field is static index name and dynamic index body
        assertThat((buffer.get(buffer0.remaining()) & 0xFF) >> 6, equalTo(1));

        // check first field is static index name and literal body
        assertThat((buffer.get(buffer1.remaining()) & 0xFF) >> 4, equalTo(0));

        // check first field is static index name and dynamic index body
        assertThat((buffer.get(buffer2.remaining()) & 0xFF) >> 6, equalTo(1));

        // Only first and third fields are put in the table
        HpackContext context = encoder.getHpackContext();
        assertThat(context.size(), equalTo(2));
        assertThat(context.get(HpackContext.STATIC_SIZE + 1).getHttpField().getName(), equalTo("host"));
        assertThat(context.get(HpackContext.STATIC_SIZE + 2).getHttpField().getName(), equalTo("user-agent"));
        assertThat(context.getDynamicTableSize(), equalTo(
                context.get(HpackContext.STATIC_SIZE + 1).getSize() + context.get(HpackContext.STATIC_SIZE + 2).getSize()));

    }

    @Test
    public void testResize() {
        HttpFields fields = new HttpFields();
        fields.add("host", "localhost0");
        fields.add("cookie", "abcdefghij");

        HpackEncoder encoder = new HpackEncoder(4096);

        ByteBuffer buffer = BufferUtils.allocate(4096);
        int pos = BufferUtils.flipToFill(buffer);
        encoder.encodeMaxDynamicTableSize(buffer, 0);
        encoder.setRemoteMaxDynamicTableSize(50);
        encoder.encode(buffer, new MetaData(HttpVersion.HTTP_2, fields));
        BufferUtils.flipToFlush(buffer, pos);

        HpackContext context = encoder.getHpackContext();

        Assert.assertThat(context.getMaxDynamicTableSize(), Matchers.is(50));
        Assert.assertThat(context.size(), Matchers.is(1));
    }
}
