package com.fireflysource.common.codec;

import com.fireflysource.common.codec.base64.Base64Utils;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestBase64 {

    @Test
    void test() {
        byte[] hello = "hello world".getBytes(StandardCharsets.UTF_8);
        String base64 = Base64Utils.encodeToString(hello);
        String src = new String(Base64Utils.decodeFromString(base64), StandardCharsets.UTF_8);
        assertEquals("hello world", src);
    }

    @Test
    void testSafeUrl() {
        byte[] safeUrl = "http://www.fireflysource.com/base64/test?id=测试".getBytes(StandardCharsets.UTF_8);
        String base64 = Base64Utils.encodeToUrlSafeString(safeUrl);
        System.out.println(base64);
        String src = new String(Base64Utils.decodeFromUrlSafeString(base64), StandardCharsets.UTF_8);
        assertEquals("http://www.fireflysource.com/base64/test?id=测试", src);
    }
}
