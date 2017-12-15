package com.firefly.codec.websocket.utils;

import java.util.List;

/**
 * @author Pengtao Qiu
 */
abstract public class HeaderValueGenerator {

    public static String generateHeaderValue(List<String> values) {
        // join it with commas
        boolean needsDelim = false;
        StringBuilder ret = new StringBuilder();
        for (String value : values) {
            if (needsDelim) {
                ret.append(", ");
            }
            QuoteUtil.quoteIfNeeded(ret, value, QuoteUtil.ABNF_REQUIRED_QUOTING);
            needsDelim = true;
        }
        return ret.toString();
    }
}
