package com.fireflysource.net.http.common.codec;

import com.fireflysource.common.collection.map.MultiMap;
import com.fireflysource.common.object.TypeUtils;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Util meta Tests.
 */
class URLEncodedTest {

    private static final String __ISO_8859_1 = "iso-8859-1";
    private static final String __UTF8 = "utf-8";
    private static final String __UTF16 = "utf-16";

    @Test
    void testUrlEncoded() {

        UrlEncoded url_encoded = new UrlEncoded();
        assertEquals(0, url_encoded.size());

        url_encoded.clear();
        url_encoded.decode("");
        assertEquals(0, url_encoded.size());

        url_encoded.clear();
        url_encoded.decode("Name1=Value1");
        assertEquals(1, url_encoded.size());
        assertEquals("Name1=Value1", url_encoded.encode());
        assertEquals("Value1", url_encoded.getString("Name1"));

        url_encoded.clear();
        url_encoded.decode("Name2=");
        assertEquals(1, url_encoded.size());
        assertEquals("Name2", url_encoded.encode());
        assertEquals("", url_encoded.getString("Name2"));

        url_encoded.clear();
        url_encoded.decode("Name3");
        assertEquals(1, url_encoded.size());
        assertEquals("Name3", url_encoded.encode());
        assertEquals("", url_encoded.getString("Name3"));

        url_encoded.clear();
        url_encoded.decode("Name4=V\u0629lue+4%21");
        assertEquals(1, url_encoded.size());
        assertEquals("Name4=V%D8%A9lue+4%21", url_encoded.encode());
        assertEquals("V\u0629lue 4!", url_encoded.getString("Name4"));

        url_encoded.clear();
        url_encoded.decode("Name4=Value%2B4%21");
        assertEquals(1, url_encoded.size());
        assertEquals("Name4=Value%2B4%21", url_encoded.encode());
        assertEquals("Value+4!", url_encoded.getString("Name4"));

        url_encoded.clear();
        url_encoded.decode("Name4=Value+4%21%20%214");
        assertEquals(1, url_encoded.size());
        assertEquals("Name4=Value+4%21+%214", url_encoded.encode());
        assertEquals("Value 4! !4", url_encoded.getString("Name4"));


        url_encoded.clear();
        url_encoded.decode("Name5=aaa&Name6=bbb");
        assertEquals(2, url_encoded.size());
        assertTrue(url_encoded.encode().equals("Name5=aaa&Name6=bbb") ||
                url_encoded.encode().equals("Name6=bbb&Name5=aaa")
        );
        assertEquals("aaa", url_encoded.getString("Name5"));
        assertEquals("bbb", url_encoded.getString("Name6"));

        url_encoded.clear();
        url_encoded.decode("Name7=aaa&Name7=b%2Cb&Name7=ccc");
        assertEquals("Name7=aaa&Name7=b%2Cb&Name7=ccc", url_encoded.encode());
        assertEquals("aaa,b,b,ccc", url_encoded.getString("Name7"));
        assertEquals("aaa", url_encoded.getValues("Name7").get(0));
        assertEquals("b,b", url_encoded.getValues("Name7").get(1));
        assertEquals("ccc", url_encoded.getValues("Name7").get(2));

        url_encoded.clear();
        url_encoded.decode("Name8=xx%2C++yy++%2Czz");
        assertEquals(1, url_encoded.size());
        assertEquals("Name8=xx%2C++yy++%2Czz", url_encoded.encode());
        assertEquals("xx,  yy  ,zz", url_encoded.getString("Name8"));
    }

    @Test
    void testUrlEncodedStream()
            throws Exception {
        @SuppressWarnings("InjectedReferences") String[][] charsets = new String[][]
                {
                        {__UTF8, null, "%30"},
                        {__ISO_8859_1, __ISO_8859_1, "%30"},
                        {__UTF8, __UTF8, "%30"},
                        {__UTF16, __UTF16, "%00%30"},
                };

        // Note: "%30" -> decode -> "0"

        for (int i = 0; i < charsets.length; i++) {
            ByteArrayInputStream in = new ByteArrayInputStream(("name\n=value+" + charsets[i][2] + "&name1=&name2&n\u00e3me3=value+3").getBytes(charsets[i][0]));
            MultiMap<String> m = new MultiMap<>();
            UrlEncoded.decodeTo(in, m, charsets[i][1] == null ? null : Charset.forName(charsets[i][1]), -1, -1);
            assertEquals(4, m.size());
            assertEquals("value 0", m.getString("name\n"));
            assertEquals("", m.getString("name1"));
            assertEquals("", m.getString("name2"));
            assertEquals("value 3", m.getString("n\u00e3me3"));
        }


        if (Charset.isSupported("Shift_JIS")) {
            ByteArrayInputStream in2 = new ByteArrayInputStream("name=%83e%83X%83g".getBytes(StandardCharsets.ISO_8859_1));
            MultiMap<String> m2 = new MultiMap<>();
            UrlEncoded.decodeTo(in2, m2, Charset.forName("Shift_JIS"), -1, -1);
            assertEquals(1, m2.size());
            assertEquals("\u30c6\u30b9\u30c8", m2.getString("name"));
        } else
            assertTrue(true);

    }

    @Test
    void testUtf8()
            throws Exception {
        UrlEncoded url_encoded = new UrlEncoded();
        assertEquals(0, url_encoded.size());

        url_encoded.clear();
        url_encoded.decode("text=%E0%B8%9F%E0%B8%AB%E0%B8%81%E0%B8%A7%E0%B8%94%E0%B8%B2%E0%B9%88%E0%B8%81%E0%B8%9F%E0%B8%A7%E0%B8%AB%E0%B8%AA%E0%B8%94%E0%B8%B2%E0%B9%88%E0%B8%AB%E0%B8%9F%E0%B8%81%E0%B8%A7%E0%B8%94%E0%B8%AA%E0%B8%B2%E0%B8%9F%E0%B8%81%E0%B8%AB%E0%B8%A3%E0%B8%94%E0%B9%89%E0%B8%9F%E0%B8%AB%E0%B8%99%E0%B8%81%E0%B8%A3%E0%B8%94%E0%B8%B5&Action=Submit");

        String hex = "E0B89FE0B8ABE0B881E0B8A7E0B894E0B8B2E0B988E0B881E0B89FE0B8A7E0B8ABE0B8AAE0B894E0B8B2E0B988E0B8ABE0B89FE0B881E0B8A7E0B894E0B8AAE0B8B2E0B89FE0B881E0B8ABE0B8A3E0B894E0B989E0B89FE0B8ABE0B899E0B881E0B8A3E0B894E0B8B5";
        String expected = new String(TypeUtils.fromHexString(hex), StandardCharsets.UTF_8);
        assertEquals(expected, url_encoded.getString("text"));
    }

    @Test
    void testUtf8_MultiByteCodePoint() {
        String input = "text=test%C3%A4";
        UrlEncoded url_encoded = new UrlEncoded();
        url_encoded.decode(input);

        // http://www.ltg.ed.ac.uk/~richard/utf-8.cgi?input=00e4&mode=hex
        // Should be "testä"
        // "test" followed by a LATIN SMALL LETTER A WITH DIAERESIS

        String expected = "test\u00e4";
        assertEquals(expected, url_encoded.getString("text"));
    }
}
