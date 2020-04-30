package com.fireflysource.net.http.common.codec;

import com.fireflysource.common.io.BufferUtils;
import com.fireflysource.net.http.common.model.ContentEncoding;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

public class ContentEncodedTest {

    static Stream<Arguments> testParametersProvider() {
        return Stream.of(
                arguments(ContentEncoding.GZIP),
                arguments(ContentEncoding.DEFLATE)
        );
    }

    @ParameterizedTest
    @MethodSource("testParametersProvider")
    @DisplayName("should encode and decode content successfully.")
    void test(ContentEncoding encoding) throws Exception {
        ByteBuffer buffer = BufferUtils.toBuffer("测试hello", StandardCharsets.UTF_8);
        byte[] encodedBytes = ContentEncoded.encode(BufferUtils.toArray(buffer), encoding);
        byte[] decodedBytes = ContentEncoded.decode(encodedBytes, encoding);
        assertEquals("测试hello", new String(decodedBytes, StandardCharsets.UTF_8));
    }
}
