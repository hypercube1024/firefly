package com.firefly.utils;

abstract public class VerifyUtils {

    public static boolean isNumeric(String str) {
        if (isEmpty(str))
            return false;

        char first = str.charAt(0);
        int i = first == '-' ? 1 : 0;
        for (; i < str.length(); i++) {
            if (!isDigit(str.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    public static boolean isInteger(String str) {
        if (isEmpty(str))
            return false;

        char first = str.charAt(0);
        int i = first == '-' ? 1 : 0;
        for (; i < str.length(); i++) {
            if (!isDigit(str.charAt(i))) {
                return false;
            }
        }

        Long t = Long.parseLong(str);
        return t <= Integer.MAX_VALUE && t >= Integer.MIN_VALUE;
    }

    public static boolean isLong(String str) {
        if (isEmpty(str))
            return false;

        char first = str.charAt(0);
        char end = str.charAt(str.length() - 1);
        boolean j = end == 'l' || end == 'L';
        int i = first == '-' ? 1 : 0;
        int len = j ? str.length() - 1 : str.length();
        for (; i < len; i++) {
            if (!isDigit(str.charAt(i))) {
                return false;
            }
        }

        if (!j) {
            Long t = Long.parseLong(str);
            return t > Integer.MAX_VALUE || t < Integer.MIN_VALUE;
        } else {
            return true;
        }
    }

    public static boolean isFloat(String str) {
        if (isEmpty(str))
            return false;

        char end = str.charAt(str.length() - 1);
        if (!(end == 'f' || end == 'F'))
            return false;

        int point = 0;
        int i = str.charAt(0) == '-' ? 1 : 0;
        for (; i < str.length() - 1; i++) {
            char c = str.charAt(i);
            if (c == '.') {
                point++;
            } else if (!isDigit(c)) {
                return false;
            }
        }
        return point == 1 || point == 0;
    }

    public static boolean isDouble(String str) {
        if (isEmpty(str))
            return false;

        int point = 0;
        int i = str.charAt(0) == '-' ? 1 : 0;
        for (; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c == '.') {
                point++;
            } else if (!isDigit(c)) {
                return false;
            }
        }

        return point == 1;
    }

    public static boolean isDigit(char ch) {
        return ch >= '0' && ch <= '9';
    }

    public static boolean isNotEmpty(Long o) {
        return o != null && StringUtils.hasText(o.toString());
    }

    public static boolean isNotEmpty(Integer o) {
        return o != null && StringUtils.hasText(o.toString());
    }

    public static boolean isNotEmpty(String o) {
        return StringUtils.hasText(o);
    }

    public static boolean isEmpty(Long o) {
        return !isNotEmpty(o);
    }

    public static boolean isEmpty(Integer o) {
        return !isNotEmpty(o);
    }

    public static boolean isEmpty(String o) {
        return !isNotEmpty(o);
    }
}
