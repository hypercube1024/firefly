package com.firefly.wechat.utils;

import java.util.concurrent.ThreadLocalRandom;

/**
 * @author Pengtao Qiu
 */
abstract public class NonceUtils {

    private static final String str = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    public static String generateNonce() {
        int size = 16;
        StringBuilder nonce = new StringBuilder(size);
        for (int i = 0; i < size; i++) {
            nonce.append(str.charAt(ThreadLocalRandom.current().nextInt(str.length())));
        }
        return nonce.toString();
    }

    public static void main(String[] args) {
        for (int i = 0; i < 10; i++) {
            System.out.println(generateNonce());
        }
    }
}
