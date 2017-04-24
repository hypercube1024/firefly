package com.firefly.utils.codec;

/**
 * @author Pengtao Qiu
 */
abstract public class NameCodec {

    public static String encode(String num) {
        return num.length() < 10 ? "0" + num.length() + num : num.length() + num;
    }

    public static String decode(String str) {
        return str.substring(2);
    }
}
