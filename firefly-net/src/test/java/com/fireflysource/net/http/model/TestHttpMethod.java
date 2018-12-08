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
class TestHttpMethod {

    static Stream<Arguments> testParametersProvider() {
        return Arrays.stream(HttpMethod.values()).map(m -> arguments(m, m.getValue()));
    }

    @ParameterizedTest
    @MethodSource("testParametersProvider")
    void test(HttpMethod method, String name) {
        assertEquals(method, HttpMethod.from(name));
        assertTrue(method.is(name.toLowerCase()));
    }

}
