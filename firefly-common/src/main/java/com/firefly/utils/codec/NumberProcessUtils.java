package com.firefly.utils.codec;

abstract public class NumberProcessUtils {

    public static int toUnsignedInteger(byte i) {
        return i & 0xff;
    }

    public static int toUnsignedInteger(short i) {
        return i & 0xff_ff;
    }

    public static long toUnsignedLong(int i) {
        return i & 0xff_ff_ff_ffL;
    }

    public static int toUnsigned24bitsInteger(byte highOrder, short lowOrder) {
        int x = toUnsignedInteger(highOrder);
        x <<= 16;
        x += toUnsignedInteger(lowOrder);
        return x;
    }

    public static short toUnsigned15bitsShort(short i) {
        return (short) (i & 0x7F_FF);
    }

    public static int toUnsigned31bitsInteger(int i) {
        return i & 0x7F_FF_FF_FF;
    }
}
