package com.fireflysource.net.http.common.model;


import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;


class QuotedQualityCSVTest {

    private static final String[] preferBrotli = {"br", "gzip"};
    private static final String[] preferGzip = {"gzip", "br"};
    private static final String[] noFormats = {};

    @SafeVarargs
    static <T> void assertContains(List<T> src, T... dest) {
        Arrays.stream(dest).forEach(e -> assertTrue(src.contains(e)));
    }

    @Test
    void test7231_5_3_2_example1() {
        QuotedQualityCSV values = new QuotedQualityCSV();
        values.addValue(" audio/*; q=0.2, audio/basic");
        assertContains(values.getValues(), "audio/basic", "audio/*");
    }

    @Test
    void test7231_5_3_2_example2() {
        QuotedQualityCSV values = new QuotedQualityCSV();
        values.addValue("text/plain; q=0.5, text/html,");
        values.addValue("text/x-dvi; q=0.8, text/x-c");
        assertContains(values.getValues(), "text/html", "text/x-c", "text/x-dvi", "text/plain");
    }

    @Test
    void test7231_5_3_2_example3() {
        QuotedQualityCSV values = new QuotedQualityCSV();
        values.addValue("text/*, text/plain, text/plain;format=flowed, */*");

        // Note this sort is only on quality and not the most specific type as per 5.3.2
        assertContains(values.getValues(), "text/*", "text/plain", "text/plain;format=flowed", "*/*");
    }

    @Test
    void test7231_5_3_2_example3_most_specific() {
        QuotedQualityCSV values = new QuotedQualityCSV(QuotedQualityCSV.MOST_SPECIFIC);
        values.addValue("text/*, text/plain, text/plain;format=flowed, */*");

        assertContains(values.getValues(), "text/plain;format=flowed", "text/plain", "text/*", "*/*");
    }

    @Test
    void test7231_5_3_2_example4() {
        QuotedQualityCSV values = new QuotedQualityCSV();
        values.addValue("text/*;q=0.3, text/html;q=0.7, text/html;level=1,");
        values.addValue("text/html;level=2;q=0.4, */*;q=0.5");
        assertContains(values.getValues(),
                "text/html;level=1",
                "text/html",
                "*/*",
                "text/html;level=2",
                "text/*"
        );
    }

    @Test
    void test7231_5_3_4_example1() {
        QuotedQualityCSV values = new QuotedQualityCSV();
        values.addValue("compress, gzip");
        values.addValue("");
        values.addValue("*");
        values.addValue("compress;q=0.5, gzip;q=1.0");
        values.addValue("gzip;q=1.0, identity; q=0.5, *;q=0");

        assertContains(values.getValues(),
                "compress",
                "gzip",
                "*",
                "gzip",
                "gzip",
                "compress",
                "identity"
        );
    }

    @Test
    void testOWS() {
        QuotedQualityCSV values = new QuotedQualityCSV();
        values.addValue("  value 0.5  ;  p = v  ;  q =0.5  ,  value 1.0 ");
        assertContains(values.getValues(),
                "value 1.0",
                "value 0.5;p=v");
    }

    @Test
    void testEmpty() {
        QuotedQualityCSV values = new QuotedQualityCSV();
        values.addValue(",aaaa,  , bbbb ,,cccc,");
        assertContains(values.getValues(),
                "aaaa",
                "bbbb",
                "cccc");
    }

    @Test
    void testQuoted() {
        QuotedQualityCSV values = new QuotedQualityCSV();
        values.addValue("  value 0.5  ;  p = \"v  ;  q = \\\"0.5\\\"  ,  value 1.0 \"  ");
        assertContains(values.getValues(),
                "value 0.5;p=\"v  ;  q = \\\"0.5\\\"  ,  value 1.0 \"");
    }

    @Test
    void testOpenQuote() {
        QuotedQualityCSV values = new QuotedQualityCSV();
        values.addValue("value;p=\"v");
        assertContains(values.getValues(),
                "value;p=\"v");
    }

    /* ------------------------------------------------------------ */

    @Test
    void testQuotedQuality() {
        QuotedQualityCSV values = new QuotedQualityCSV();
        values.addValue("  value 0.5  ;  p = v  ;  q = \"0.5\"  ,  value 1.0 ");
        assertContains(values.getValues(),
                "value 1.0",
                "value 0.5;p=v");
    }

    @Test
    void testBadQuality() {
        QuotedQualityCSV values = new QuotedQualityCSV();
        values.addValue("value0.5;p=v;q=0.5,value1.0,valueBad;q=X");
        assertContains(values.getValues(),
                "value1.0",
                "value0.5;p=v");
    }

    @Test
    void testBad() {
        QuotedQualityCSV values = new QuotedQualityCSV();


        // None of these should throw exceptions
        values.addValue(null);
        values.addValue("");

        values.addValue(";");
        values.addValue("=");
        values.addValue(",");

        values.addValue(";;");
        values.addValue(";=");
        values.addValue(";,");
        values.addValue("=;");
        values.addValue("==");
        values.addValue("=,");
        values.addValue(",;");
        values.addValue(",=");
        values.addValue(",,");

        values.addValue(";;;");
        values.addValue(";;=");
        values.addValue(";;,");
        values.addValue(";=;");
        values.addValue(";==");
        values.addValue(";=,");
        values.addValue(";,;");
        values.addValue(";,=");
        values.addValue(";,,");

        values.addValue("=;;");
        values.addValue("=;=");
        values.addValue("=;,");
        values.addValue("==;");
        values.addValue("===");
        values.addValue("==,");
        values.addValue("=,;");
        values.addValue("=,=");
        values.addValue("=,,");

        values.addValue(",;;");
        values.addValue(",;=");
        values.addValue(",;,");
        values.addValue(",=;");
        values.addValue(",==");
        values.addValue(",=,");
        values.addValue(",,;");
        values.addValue(",,=");
        values.addValue(",,,");

        values.addValue("x;=1");
        values.addValue("=1");
        values.addValue("q=x");
        values.addValue("q=0");
        values.addValue("q=");
        values.addValue("q=,");
        values.addValue("q=;");

    }

    @Test
    void testFirefoxContentEncodingWithBrotliPreference() {
        QuotedQualityCSV values = new QuotedQualityCSV(preferBrotli);
        values.addValue("gzip, deflate, br");
        assertContains(values.getValues(), "br", "gzip", "deflate");
    }

    @Test
    void testFirefoxContentEncodingWithGzipPreference() {
        QuotedQualityCSV values = new QuotedQualityCSV(preferGzip);
        values.addValue("gzip, deflate, br");
        assertContains(values.getValues(), "gzip", "br", "deflate");
    }

    @Test
    void testFirefoxContentEncodingWithNoPreference() {
        QuotedQualityCSV values = new QuotedQualityCSV(noFormats);
        values.addValue("gzip, deflate, br");
        assertContains(values.getValues(), "gzip", "deflate", "br");
    }

    @Test
    void testChromeContentEncodingWithBrotliPreference() {
        QuotedQualityCSV values = new QuotedQualityCSV(preferBrotli);
        values.addValue("gzip, deflate, sdch, br");
        assertContains(values.getValues(), "br", "gzip", "deflate", "sdch");
    }

    @Test
    void testComplexEncodingWithGzipPreference() {
        QuotedQualityCSV values = new QuotedQualityCSV(preferGzip);
        values.addValue("gzip;q=0.9, identity;q=0.1, *;q=0.01, deflate;q=0.9, sdch;q=0.7, br;q=0.9");
        assertContains(values.getValues(), "gzip", "br", "deflate", "sdch", "identity", "*");
    }

    @Test
    void testComplexEncodingWithBrotliPreference() {
        QuotedQualityCSV values = new QuotedQualityCSV(preferBrotli);
        values.addValue("gzip;q=0.9, identity;q=0.1, *;q=0, deflate;q=0.9, sdch;q=0.7, br;q=0.99");
        assertContains(values.getValues(), "br", "gzip", "deflate", "sdch", "identity");
    }

    @Test
    void testStarEncodingWithGzipPreference() {
        QuotedQualityCSV values = new QuotedQualityCSV(preferGzip);
        values.addValue("br, *");
        assertContains(values.getValues(), "*", "br");
    }

    @Test
    void testStarEncodingWithBrotliPreference() {
        QuotedQualityCSV values = new QuotedQualityCSV(preferBrotli);
        values.addValue("gzip, *");
        assertContains(values.getValues(), "*", "gzip");
    }


    @Test
    void testSameQuality() {
        QuotedQualityCSV values = new QuotedQualityCSV();
        values.addValue("one;q=0.5,two;q=0.5,three;q=0.5");
        assertContains(values.getValues(), "one", "two", "three");
    }

    @Test
    void testNoQuality() {
        QuotedQualityCSV values = new QuotedQualityCSV();
        values.addValue("one,two;,three;x=y");
        assertContains(values.getValues(), "one", "two", "three;x=y");
    }


    @Test
    void testQuality() {
        List<String> results = new ArrayList<>();

        QuotedQualityCSV values = new QuotedQualityCSV() {
            @Override
            protected void parsedValue(StringBuilder buffer) {
                results.add("parsedValue: " + buffer.toString());

                super.parsedValue(buffer);
            }

            @Override
            protected void parsedParam(StringBuilder buffer, int valueLength, int paramName, int paramValue) {
                String param = buffer.substring(paramName, buffer.length());
                results.add("parsedParam: " + param);

                super.parsedParam(buffer, valueLength, paramName, paramValue);
            }
        };


        // The provided string is not legal according to some RFCs ( not a token because of = and not a parameter because not preceded by ; )
        // The string is legal according to RFC7239 which allows for just parameters (called forwarded-pairs)
        values.addValue("p=0.5,q=0.5");


        // The QuotedCSV implementation is lenient and adopts the later interpretation and thus sees q=0.5 and p=0.5 both as parameters
        assertContains(results, "parsedValue: ", "parsedParam: p=0.5",
                "parsedValue: ", "parsedParam: q=0.5");


        // However the QuotedQualityCSV only handles the q parameter and that is consumed from the parameter string.
        assertContains(values.getValues(), "p=0.5", "");

    }
}
