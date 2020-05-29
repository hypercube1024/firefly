package com.fireflysource.net.websocket.utils;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test QuoteUtil.quote(), and QuoteUtil.dequote()
 */
public class QuoteUtilQuoteTest {
    public static Stream<Arguments> data() {
        // The various quoting of a String
        List<Object[]> data = new ArrayList<>();

        data.add(new Object[]{"Hi", "\"Hi\""});
        data.add(new Object[]{"Hello World", "\"Hello World\""});
        data.add(new Object[]{"9.0.0", "\"9.0.0\""});
        data.add(new Object[]{
                "Something \"Special\"",
                "\"Something \\\"Special\\\"\""
        });
        data.add(new Object[]{
                "A Few\n\"Good\"\tMen",
                "\"A Few\\n\\\"Good\\\"\\tMen\""
        });

        return data.stream().map(Arguments::of);
    }

    @ParameterizedTest
    @MethodSource("data")
    public void testDequoting(final String unquoted, final String quoted) {
        String actual = QuoteUtil.dequote(quoted);
        actual = QuoteUtil.unescape(actual);
        assertEquals(unquoted, actual);
    }

    @ParameterizedTest
    @MethodSource("data")
    public void testQuoting(final String unquoted, final String quoted) {
        StringBuilder buf = new StringBuilder();
        QuoteUtil.quote(buf, unquoted);

        String actual = buf.toString();
        assertEquals(quoted, actual);
    }
}
