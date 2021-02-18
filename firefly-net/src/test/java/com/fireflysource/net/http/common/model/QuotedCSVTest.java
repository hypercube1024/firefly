package com.fireflysource.net.http.common.model;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


class QuotedCSVTest {

    @SafeVarargs
    static <T> void assertContains(List<T> src, T... dest) {
        Arrays.stream(dest).forEach(e -> assertTrue(src.contains(e)));
    }

    @Test
    void testOWS() {
        QuotedCSV values = new QuotedCSV();
        values.addValue("  value 0.5  ;  pqy = vwz  ;  q =0.5  ,  value 1.0 ,  other ; param ");
        assertContains(values.getValues(), "value 0.5;pqy=vwz;q=0.5", "value 1.0", "other;param");
    }

    @Test
    void testEmpty() {
        QuotedCSV values = new QuotedCSV();
        values.addValue(",aaaa,  , bbbb ,,cccc,");
        assertContains(values.getValues(), "aaaa", "bbbb", "cccc");
    }

    @Test
    void testQuoted() {
        QuotedCSV values = new QuotedCSV();
        values.addValue("A;p=\"v\",B,\"C, D\"");
        assertContains(values.getValues(), "A;p=\"v\"", "B", "\"C, D\"");
    }

    @Test
    void testOpenQuote() {
        QuotedCSV values = new QuotedCSV();
        values.addValue("value;p=\"v");
        assertContains(values.getValues(), "value;p=\"v");
    }

    @Test
    void testQuotedNoQuotes() {
        QuotedCSV values = new QuotedCSV(false);
        values.addValue("A;p=\"v\",B,\"C, D\"");
        assertContains(values.getValues(), "A;p=v", "B", "C, D");
    }

    @Test
    void testOpenQuoteNoQuotes() {
        QuotedCSV values = new QuotedCSV(false);
        values.addValue("value;p=\"v");
        assertContains(values.getValues(), "value;p=v");
    }

    @Test
    void testParamsOnly() {
        QuotedCSV values = new QuotedCSV(false);
        values.addValue("for=192.0.2.43, for=\"[2001:db8:cafe::17]\", for=unknown");
        assertContains(values.getValues(), "for=192.0.2.43", "for=[2001:db8:cafe::17]", "for=unknown");
    }

    @Test
    void testMutation() {
        QuotedCSV values = new QuotedCSV(false) {

            @Override
            protected void parsedValue(StringBuilder buffer) {
                if (buffer.toString().contains("DELETE")) {
                    String s = buffer.toString().replace("DELETE", "");
                    buffer.setLength(0);
                    buffer.append(s);
                }
                if (buffer.toString().contains("APPEND")) {
                    String s = buffer.toString().replace("APPEND", "Append") + "!";
                    buffer.setLength(0);
                    buffer.append(s);
                }
            }

            @Override
            protected void parsedParam(StringBuilder buffer, int valueLength, int paramName, int paramValue) {
                String name = paramValue > 0 ? buffer.substring(paramName, paramValue - 1) : buffer.substring(paramName);
                if ("IGNORE".equals(name))
                    buffer.setLength(paramName - 1);
            }

        };

        values.addValue("normal;param=val, testAPPENDandDELETEvalue ; n=v; IGNORE = this; x=y ");

        assertContains(values.getValues(),
                "normal;param=val",
                "testAppendandvalue!;n=v;x=y");
    }


    @Test
    void testUnQuote() {
        assertEquals("", QuotedCSV.unquote(""));
        assertEquals("", QuotedCSV.unquote("\"\""));
        assertEquals("foo", QuotedCSV.unquote("foo"));
        assertEquals("foo", QuotedCSV.unquote("\"foo\""));
        assertEquals("foo", QuotedCSV.unquote("f\"o\"o"));
        assertEquals("\"foo", QuotedCSV.unquote("\"\\\"foo\""));
        assertEquals("\\foo", QuotedCSV.unquote("\\foo"));
    }

}
