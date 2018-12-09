package com.fireflysource.net.http.common.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class HttpStatusCodeTest {

    @Test
    void testInvalidGetCode() {
        assertNull(HttpStatus.getCode(800), "Invalid code: 800");
        assertNull(HttpStatus.getCode(190), "Invalid code: 190");
    }


    @Test
    void testImATeapot() {
        assertEquals("I'm a Teapot", HttpStatus.getMessage(418));
        assertEquals("Expectation Failed", HttpStatus.getMessage(417));
    }

}
