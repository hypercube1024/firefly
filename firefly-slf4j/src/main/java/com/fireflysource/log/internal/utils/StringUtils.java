package com.fireflysource.log.internal.utils;

import java.util.AbstractCollection;
import java.util.Arrays;

/**
 * @author Pengtao Qiu
 */
abstract public class StringUtils {

    public static boolean hasText(String str) {
        return hasText((CharSequence) str);
    }

    public static boolean hasText(CharSequence str) {
        if (!hasLength(str)) {
            return false;
        }
        int strLen = str.length();
        for (int i = 0; i < strLen; i++) {
            if (!Character.isWhitespace(str.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasLength(CharSequence str) {
        return (str != null && str.length() > 0);
    }

    public static boolean hasLength(String str) {
        return hasLength((CharSequence) str);
    }

    public static String replace(String s, Object... objs) {
        if (objs == null || objs.length == 0)
            return s;
        if (!s.contains("{}"))
            return s;

        StringBuilder ret = new StringBuilder((int) (s.length() * 1.5));
        int cursor = 0;
        int index = 0;
        for (int start; (start = s.indexOf("{}", cursor)) != -1; ) {
            ret.append(s, cursor, start);
            if (index < objs.length) {
                Object obj = objs[index];
                try {
                    if (obj != null) {
                        if (obj instanceof AbstractCollection) {
                            ret.append(Arrays.toString(((AbstractCollection<?>) obj).toArray()));
                        } else {
                            ret.append(obj);
                        }
                    } else {
                        ret.append("null");
                    }
                } catch (Throwable ignored) {
                }
            } else {
                ret.append("{}");
            }
            cursor = start + 2;
            index++;
        }
        ret.append(s, cursor, s.length());
        return ret.toString();
    }
}
