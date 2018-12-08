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
class TestHttpScheme {

    static Stream<Arguments> testParametersProvider() {
        return Arrays.stream(HttpScheme.values()).map(m -> arguments(m, m.getValue()));
    }

    @ParameterizedTest
    @MethodSource("testParametersProvider")
    void test(HttpScheme scheme, String name) {
        assertEquals(scheme, HttpScheme.from(name));
        assertTrue(scheme.is(name.toLowerCase()));
    }

}
