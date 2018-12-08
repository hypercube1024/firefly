package com.fireflysource.net.http.v2.hpack;

import com.fireflysource.common.io.BufferUtils;
import com.fireflysource.common.object.TypeUtils;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

class TestHuffman {

    String[][] tests = {
            {"D.4.1", "f1e3c2e5f23a6ba0ab90f4ff", "www.example.com"},
            {"D.4.2", "a8eb10649cbf", "no-cache"},
            {"D.6.1k", "6402", "302"},
            {"D.6.1v", "aec3771a4b", "private"},
            {"D.6.1d", "d07abe941054d444a8200595040b8166e082a62d1bff", "Mon, 21 Oct 2013 20:13:21 GMT"},
            {"D.6.1l", "9d29ad171863c78f0b97c8e9ae82ae43d3", "https://www.example.com"},
            {"D.6.2te", "640cff", "303"},
    };

    @Test
    void testDecode() throws Exception {
        for (String[] test : tests) {
            byte[] encoded = TypeUtils.fromHexString(test[1]);
            String decoded = Huffman.decode(ByteBuffer.wrap(encoded));
            assertEquals(decoded, test[2]);
        }
    }

    @Test
    void testEncode() {
        for (String[] test : tests) {
            ByteBuffer buf = ByteBuffer.allocate(1024);
            Huffman.encode(buf, test[2]);
            buf.flip();
            String encoded = TypeUtils.toHexString(BufferUtils.toArray(buf)).toLowerCase(Locale.ENGLISH);
            assertEquals(encoded, test[1]);
            assertEquals(test[1].length() / 2, Huffman.octetsNeeded(test[2]));
        }
    }

    @Test
    void testEncode8859Only() {
        char bad[] = {(char) 128, (char) 0, (char) -1, ' ' - 1};
        for (int i = 0; i < bad.length; i++) {
            String s = "bad '" + bad[i] + "'";

            try {
                Huffman.octetsNeeded(s);
                fail("i=" + i);
            } catch (IllegalArgumentException ignored) {
            }

            try {
                Huffman.encode(ByteBuffer.allocate(32), s);
                fail("i=" + i);
            } catch (IllegalArgumentException ignored) {
            }
        }
    }


}
