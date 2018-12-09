package com.fireflysource.net.http.v2.hpack;


import com.fireflysource.net.http.model.HttpField;
import com.fireflysource.net.http.model.HttpFields;
import com.fireflysource.net.http.model.HttpVersion;
import com.fireflysource.net.http.model.MetaData;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


class HpackEncoderTest {

    @Test
    void testUnknownFieldsContextManagement() {
        HpackEncoder encoder = new HpackEncoder(38 * 5);
        HttpFields fields = new HttpFields();


        HttpField[] field = {
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
        for (int i = 0; i <= 3; i++) {
            fields.add(field[i]);
        }

        // encode them
        ByteBuffer buffer = ByteBuffer.allocate(4096);
        encoder.encode(buffer, new MetaData(HttpVersion.HTTP_2, fields));
        buffer.flip();

        // something was encoded!
        assertTrue(buffer.remaining() > 0);

        // All are in the dynamic table
        assertEquals(4, encoder.getHpackContext().size());

        // encode exact same fields again!
        buffer.clear();
        encoder.encode(buffer, new MetaData(HttpVersion.HTTP_2, fields));
        buffer.flip();

        // All are in the dynamic table
        assertEquals(4, encoder.getHpackContext().size());

        // Add 4 more fields
        for (int i = 4; i <= 7; i++)
            fields.add(field[i]);

        // encode
        buffer.clear();
        encoder.encode(buffer, new MetaData(HttpVersion.HTTP_2, fields));
        buffer.flip();

        // something was encoded!
        assertTrue(buffer.remaining() > 0);

        // max dynamic table size reached
        assertEquals(5, encoder.getHpackContext().size());


        // remove some fields
        for (int i = 0; i <= 7; i += 2)
            fields.remove(field[i].getName());

        // encode
        buffer.clear();
        encoder.encode(buffer, new MetaData(HttpVersion.HTTP_2, fields));
        buffer.flip();

        // something was encoded!
        assertTrue(buffer.remaining() > 0);

        // max dynamic table size reached
        assertEquals(5, encoder.getHpackContext().size());


        // remove another fields
        fields.remove(field[1].getName());

        // encode
        buffer.clear();
        encoder.encode(buffer, new MetaData(HttpVersion.HTTP_2, fields));
        buffer.flip();
        // something was encoded!
        assertTrue(buffer.remaining() > 0);

        // max dynamic table size reached
        assertEquals(5, encoder.getHpackContext().size());


        // re add the field

        fields.add(field[1]);

        // encode
        buffer.clear();
        encoder.encode(buffer, new MetaData(HttpVersion.HTTP_2, fields));
        buffer.flip();

        // something was encoded!
        assertTrue(buffer.remaining() > 0);

        // max dynamic table size reached
        assertEquals(5, encoder.getHpackContext().size());

    }


    @Test
    void testNeverIndexSetCookie() {
        HpackEncoder encoder = new HpackEncoder(38 * 5);
        ByteBuffer buffer = ByteBuffer.allocate(4096);

        HttpFields fields = new HttpFields();
        fields.put("set-cookie", "some cookie value");

        // encode
        encoder.encode(buffer, new MetaData(HttpVersion.HTTP_2, fields));
        buffer.flip();

        // something was encoded!
        assertTrue(buffer.remaining() > 0);

        // empty dynamic table
        assertEquals(0, encoder.getHpackContext().size());


        // encode again
        buffer.clear();
        encoder.encode(buffer, new MetaData(HttpVersion.HTTP_2, fields));
        buffer.flip();

        // something was encoded!
        assertTrue(buffer.remaining() > 0);

        // empty dynamic table
        assertEquals(0, encoder.getHpackContext().size());

    }


    @Test
    void testFieldLargerThanTable() {
        HttpFields fields = new HttpFields();

        HpackEncoder encoder = new HpackEncoder(128);
        ByteBuffer buffer0 = ByteBuffer.allocate(4096);
        encoder.encode(buffer0, new MetaData(HttpVersion.HTTP_2, fields));
        buffer0.flip();

        encoder = new HpackEncoder(128);
        fields.add(new HttpField("user-agent", "firefly/test"));
        ByteBuffer buffer1 = ByteBuffer.allocate(4096);
        encoder.encode(buffer1, new MetaData(HttpVersion.HTTP_2, fields));
        buffer1.flip();

        encoder = new HpackEncoder(128);
        fields.add(new HttpField(":path",
                "This is a very large field, whose size is larger than the dynamic table so it should not be indexed as it will not fit in the table ever!" +
                        "XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX " +
                        "YYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYY " +
                        "ZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZ "));
        ByteBuffer buffer2 = ByteBuffer.allocate(4096);
        encoder.encode(buffer2, new MetaData(HttpVersion.HTTP_2, fields));
        buffer2.flip();

        encoder = new HpackEncoder(128);
        fields.add(new HttpField("host", "somehost"));
        ByteBuffer buffer = ByteBuffer.allocate(4096);
        encoder.encode(buffer, new MetaData(HttpVersion.HTTP_2, fields));
        buffer.flip();

        //System.err.println(BufferUtils.toHexString(buffer0));
        //System.err.println(BufferUtils.toHexString(buffer1));
        //System.err.println(BufferUtils.toHexString(buffer2));
        //System.err.println(BufferUtils.toHexString(buffer));

        // something was encoded!
        assertTrue(buffer.remaining() > 0);

        // check first field is static index name and dynamic index body
        assertEquals(1, (buffer.get(buffer0.remaining()) & 0xFF) >> 6);

        // check first field is static index name and literal body
        assertEquals(0, (buffer.get(buffer1.remaining()) & 0xFF) >> 4);

        // check first field is static index name and dynamic index body
        assertEquals(1, (buffer.get(buffer2.remaining()) & 0xFF) >> 6);

        // Only first and third fields are put in the table
        HpackContext context = encoder.getHpackContext();
        assertEquals(2, context.size());
        assertEquals("host", context.get(HpackContext.STATIC_SIZE + 1).getHttpField().getName());
        assertEquals("user-agent", context.get(HpackContext.STATIC_SIZE + 2).getHttpField().getName());
        assertEquals(context.getDynamicTableSize(),
                context.get(HpackContext.STATIC_SIZE + 1).getSize() + context.get(HpackContext.STATIC_SIZE + 2).getSize());

    }

    @Test
    void testResize() {
        HttpFields fields = new HttpFields();
        fields.add("host", "localhost0");
        fields.add("cookie", "abcdefghij");

        HpackEncoder encoder = new HpackEncoder(4096);

        ByteBuffer buffer = ByteBuffer.allocate(4096);
        encoder.encodeMaxDynamicTableSize(buffer, 0);
        encoder.setRemoteMaxDynamicTableSize(50);
        encoder.encode(buffer, new MetaData(HttpVersion.HTTP_2, fields));
        buffer.flip();

        HpackContext context = encoder.getHpackContext();

        assertEquals(50, context.getMaxDynamicTableSize());
        assertEquals(1, context.size());
    }
}
