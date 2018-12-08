package com.fireflysource.net.http.model;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Pengtao Qiu
 */
class HostPortTest {
    private static Stream<Arguments> validAuthorityProvider() {
        return Stream.of(
                Arguments.of("", "", null),
                Arguments.of(":80", "", "80"),
                Arguments.of("host", "host", null),
                Arguments.of("host:80", "host", "80"),
                Arguments.of("10.10.10.1", "10.10.10.1", null),
                Arguments.of("10.10.10.1:80", "10.10.10.1", "80"),
                Arguments.of("[0::0::0::1]", "[0::0::0::1]", null),
                Arguments.of("[0::0::0::1]:80", "[0::0::0::1]", "80")
        );
    }

    private static Stream<Arguments> invalidAuthorityProvider() {
        return Stream.of(
                null,
                "host:",
                "127.0.0.1:",
                "[0::0::0::0::1]:",
                "host:xxx",
                "127.0.0.1:xxx",
                "[0::0::0::0::1]:xxx",
                "host:-80",
                "127.0.0.1:-80",
                "[0::0::0::0::1]:-80")
                     .map(Arguments::of);
    }

    @ParameterizedTest
    @MethodSource("validAuthorityProvider")
    void testValidAuthority(String authority, String expectedHost, Integer expectedPort) {
        try {
            HostPort hostPort = new HostPort(authority);
            assertEquals(expectedHost, hostPort.getHost(), authority);

            if (expectedPort == null)
                assertEquals(0, hostPort.getPort(), authority);
            else
                assertEquals(expectedPort, Integer.valueOf(hostPort.getPort()), authority);
        } catch (Exception e) {
            if (expectedHost != null)
                e.printStackTrace();
            assertNull(authority, expectedHost);
        }
    }

    @ParameterizedTest
    @MethodSource("invalidAuthorityProvider")
    void testInvalidAuthority(String authority) {
        assertThrows(IllegalArgumentException.class, () -> new HostPort(authority));
    }
}
