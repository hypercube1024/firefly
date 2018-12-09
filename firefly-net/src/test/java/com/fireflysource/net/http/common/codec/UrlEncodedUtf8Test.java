package com.fireflysource.net.http.common.codec;

import com.fireflysource.common.collection.map.MultiMap;
import com.fireflysource.common.string.Utf8Appendable;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

class UrlEncodedUtf8Test {

    static void fromString(String test, String s, String field, String expected, boolean thrown) {
        MultiMap<String> values = new MultiMap<>();
        try {
            UrlEncoded.decodeUtf8To(s, 0, s.length(), values);
            if (thrown)
                fail();
            assertEquals(expected, values.getString(field), test);
        } catch (Exception e) {
            if (!thrown)
                throw e;
        }
    }

    static void fromInputStream(String test, byte[] b, String field, String expected, boolean thrown) throws Exception {
        InputStream is = new ByteArrayInputStream(b);
        MultiMap<String> values = new MultiMap<>();
        try {
            UrlEncoded.decodeUtf8To(is, values, 1000000, -1);
            if (thrown)
                fail();
            assertEquals(expected, values.getString(field), test);
        } catch (Exception e) {
            if (!thrown)
                throw e;
        }
    }

    @Test
    void testIncompleteSequestAtTheEnd() throws Exception {
        byte[] bytes = {97, 98, 61, 99, -50};
        String test = new String(bytes, StandardCharsets.UTF_8);
        String expected = "c" + Utf8Appendable.REPLACEMENT;

        fromString(test, test, "ab", expected, false);
        fromInputStream(test, bytes, "ab", expected, false);
    }

    @Test
    void testIncompleteSequestAtTheEnd2() throws Exception {
        byte[] bytes = {97, 98, 61, -50};
        String test = new String(bytes, StandardCharsets.UTF_8);
        String expected = "" + Utf8Appendable.REPLACEMENT;

        fromString(test, test, "ab", expected, false);
        fromInputStream(test, bytes, "ab", expected, false);

    }

    @Test
    void testIncompleteSequestInName() throws Exception {
        byte[] bytes = {101, -50, 61, 102, 103, 38, 97, 98, 61, 99, 100};
        String test = new String(bytes, StandardCharsets.UTF_8);
        String name = "e" + Utf8Appendable.REPLACEMENT;
        String value = "fg";

        fromString(test, test, name, value, false);
        fromInputStream(test, bytes, name, value, false);
    }

    @Test
    void testIncompleteSequestInValue() throws Exception {
        byte[] bytes = {101, 102, 61, 103, -50, 38, 97, 98, 61, 99, 100};
        String test = new String(bytes, StandardCharsets.UTF_8);
        String name = "ef";
        String value = "g" + Utf8Appendable.REPLACEMENT;

        fromString(test, test, name, value, false);
        fromInputStream(test, bytes, name, value, false);
    }

}
