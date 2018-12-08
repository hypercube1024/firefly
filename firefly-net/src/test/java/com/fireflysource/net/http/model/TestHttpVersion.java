package com.fireflysource.net.http.model;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

/**
 * @author Pengtao Qiu
 */
class TestHttpVersion {

    static Stream<Arguments> testParametersProvider() {
        return Arrays.stream(HttpVersion.values()).map(m -> arguments(m, m.getValue()));
    }

    @ParameterizedTest
    @MethodSource("testParametersProvider")
    void test(HttpVersion version, String name) {
        assertEquals(version, HttpVersion.from(name));
        assertTrue(version.is(name.toLowerCase()));
    }
}
