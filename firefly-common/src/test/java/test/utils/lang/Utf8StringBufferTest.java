package test.utils.lang;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

import org.junit.Test;

import com.firefly.utils.lang.Utf8Appendable;
import com.firefly.utils.lang.Utf8StringBuffer;

public class Utf8StringBufferTest {
    @Test
    public void testUtfStringBuffer() throws Exception {
        String source = "abcd012345\n\r\u0000\u00a4\u10fb\ufffdjetty";
        byte[] bytes = source.getBytes(StandardCharsets.UTF_8);
        Utf8StringBuffer buffer = new Utf8StringBuffer();
        for (byte aByte : bytes)
            buffer.append(aByte);
        assertEquals(source, buffer.toString());
        assertTrue(buffer.toString().endsWith("jetty"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUtf8WithMissingByte() throws Exception {
        String source = "abc\u10fb";
        byte[] bytes = source.getBytes(StandardCharsets.UTF_8);
        Utf8StringBuffer buffer = new Utf8StringBuffer();
        for (int i = 0; i < bytes.length - 1; i++)
            buffer.append(bytes[i]);
        buffer.toString();
    }

    @Test(expected = Utf8Appendable.NotUtf8Exception.class)
    public void testUtf8WithAdditionalByte() throws Exception {
        String source = "abcXX";
        byte[] bytes = source.getBytes(StandardCharsets.UTF_8);
        bytes[3] = (byte) 0xc0;
        bytes[4] = (byte) 0x00;

        Utf8StringBuffer buffer = new Utf8StringBuffer();
        for (byte aByte : bytes)
            buffer.append(aByte);
    }

    @Test
    public void testUTF32codes() throws Exception {
        String source = "\uD842\uDF9F";
        byte[] bytes = source.getBytes(StandardCharsets.UTF_8);

        String jvmcheck = new String(bytes, 0, bytes.length, StandardCharsets.UTF_8);
        assertEquals(source, jvmcheck);

        Utf8StringBuffer buffer = new Utf8StringBuffer();
        buffer.append(bytes, 0, bytes.length);
        String result = buffer.toString();
        assertEquals(source, result);
    }

    @Test
    public void testGermanUmlauts() throws Exception {
        byte[] bytes = new byte[6];
        bytes[0] = (byte) 0xC3;
        bytes[1] = (byte) 0xBC;
        bytes[2] = (byte) 0xC3;
        bytes[3] = (byte) 0xB6;
        bytes[4] = (byte) 0xC3;
        bytes[5] = (byte) 0xA4;

        Utf8StringBuffer buffer = new Utf8StringBuffer();
        for (int i = 0; i < bytes.length; i++)
            buffer.append(bytes[i]);

        assertEquals("\u00FC\u00F6\u00E4", buffer.toString());
    }

    @Test(expected = Utf8Appendable.NotUtf8Exception.class)
    public void testInvalidUTF8() throws UnsupportedEncodingException {
        Utf8StringBuffer buffer = new Utf8StringBuffer();
        buffer.append((byte) 0xC2);
        buffer.append((byte) 0xC2);
    }
}
