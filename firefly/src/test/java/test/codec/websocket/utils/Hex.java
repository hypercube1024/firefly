package test.codec.websocket.utils;

import com.firefly.utils.io.BufferUtils;

import java.nio.ByteBuffer;

public final class Hex {
    private static final char[] hexcodes = "0123456789ABCDEF".toCharArray();

    public static byte[] asByteArray(String hstr) {
        if ((hstr.length() < 0) || ((hstr.length() % 2) != 0)) {
            throw new IllegalArgumentException(String.format("Invalid string length of <%d>", hstr.length()));
        }

        int size = hstr.length() / 2;
        byte buf[] = new byte[size];
        byte hex;
        int len = hstr.length();

        int idx = (int) Math.floor(((size * 2) - (double) len) / 2);
        for (int i = 0; i < len; i++) {
            hex = 0;
            if (i >= 0) {
                hex = (byte) (Character.digit(hstr.charAt(i), 16) << 4);
            }
            i++;
            hex += (byte) (Character.digit(hstr.charAt(i), 16));

            buf[idx] = hex;
            idx++;
        }

        return buf;
    }

    public static ByteBuffer asByteBuffer(String hstr) {
        return ByteBuffer.wrap(asByteArray(hstr));
    }

    public static String asHex(byte buf[]) {
        int len = buf.length;
        char out[] = new char[len * 2];
        for (int i = 0; i < len; i++) {
            out[i * 2] = hexcodes[(buf[i] & 0xF0) >> 4];
            out[(i * 2) + 1] = hexcodes[(buf[i] & 0x0F)];
        }
        return String.valueOf(out);
    }

    public static String asHex(ByteBuffer buffer) {
        return asHex(BufferUtils.toArray(buffer));
    }
}
