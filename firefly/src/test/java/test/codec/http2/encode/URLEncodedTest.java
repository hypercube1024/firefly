
package test.codec.http2.encode;

import com.firefly.codec.http2.encode.UrlEncoded;
import com.firefly.utils.StringUtils;
import com.firefly.utils.collection.MultiMap;
import com.firefly.utils.lang.TypeUtils;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

/**
 * Util meta Tests.
 */
public class URLEncodedTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void testUrlEncoded() {

        UrlEncoded url_encoded = new UrlEncoded();
        assertEquals("Initially not empty", 0, url_encoded.size());

        url_encoded.clear();
        url_encoded.decode("");
        assertEquals("Not empty after decode(\"\")", 0, url_encoded.size());

        url_encoded.clear();
        url_encoded.decode("Name1=Value1");
        assertEquals("simple param size", 1, url_encoded.size());
        assertEquals("simple encode", "Name1=Value1", url_encoded.encode());
        assertEquals("simple get", "Value1", url_encoded.getString("Name1"));

        url_encoded.clear();
        url_encoded.decode("Name2=");
        assertEquals("dangling param size", 1, url_encoded.size());
        assertEquals("dangling encode", "Name2", url_encoded.encode());
        assertEquals("dangling get", "", url_encoded.getString("Name2"));

        url_encoded.clear();
        url_encoded.decode("Name3");
        assertEquals("noValue param size", 1, url_encoded.size());
        assertEquals("noValue encode", "Name3", url_encoded.encode());
        assertEquals("noValue get", "", url_encoded.getString("Name3"));

        url_encoded.clear();
        url_encoded.decode("Name4=V\u0629lue+4%21");
        assertEquals("encoded param size", 1, url_encoded.size());
        assertEquals("encoded encode", "Name4=V%D8%A9lue+4%21", url_encoded.encode());
        assertEquals("encoded get", "V\u0629lue 4!", url_encoded.getString("Name4"));

        url_encoded.clear();
        url_encoded.decode("Name4=Value%2B4%21");
        assertEquals("encoded param size", 1, url_encoded.size());
        assertEquals("encoded encode", "Name4=Value%2B4%21", url_encoded.encode());
        assertEquals("encoded get", "Value+4!", url_encoded.getString("Name4"));

        url_encoded.clear();
        url_encoded.decode("Name4=Value+4%21%20%214");
        assertEquals("encoded param size", 1, url_encoded.size());
        assertEquals("encoded encode", "Name4=Value+4%21+%214", url_encoded.encode());
        assertEquals("encoded get", "Value 4! !4", url_encoded.getString("Name4"));


        url_encoded.clear();
        url_encoded.decode("Name5=aaa&Name6=bbb");
        assertEquals("multi param size", 2, url_encoded.size());
        assertTrue("multi encode " + url_encoded.encode(),
                url_encoded.encode().equals("Name5=aaa&Name6=bbb") ||
                        url_encoded.encode().equals("Name6=bbb&Name5=aaa")
        );
        assertEquals("multi get", "aaa", url_encoded.getString("Name5"));
        assertEquals("multi get", "bbb", url_encoded.getString("Name6"));

        url_encoded.clear();
        url_encoded.decode("Name7=aaa&Name7=b%2Cb&Name7=ccc");
        assertEquals("multi encode", "Name7=aaa&Name7=b%2Cb&Name7=ccc", url_encoded.encode());
        assertEquals("list get all", url_encoded.getString("Name7"), "aaa,b,b,ccc");
        assertEquals("list get", "aaa", url_encoded.getValues("Name7").get(0));
        assertEquals("list get", url_encoded.getValues("Name7").get(1), "b,b");
        assertEquals("list get", "ccc", url_encoded.getValues("Name7").get(2));

        url_encoded.clear();
        url_encoded.decode("Name8=xx%2C++yy++%2Czz");
        assertEquals("encoded param size", 1, url_encoded.size());
        assertEquals("encoded encode", "Name8=xx%2C++yy++%2Czz", url_encoded.encode());
        assertEquals("encoded get", url_encoded.getString("Name8"), "xx,  yy  ,zz");
    }

    /* -------------------------------------------------------------- */
    @Test
    public void testUrlEncodedStream()
            throws Exception {
        String[][] charsets = new String[][]
                {
                        {StringUtils.__UTF8, null, "%30"},
                        {StringUtils.__ISO_8859_1, StringUtils.__ISO_8859_1, "%30"},
                        {StringUtils.__UTF8, StringUtils.__UTF8, "%30"},
                        {StringUtils.__UTF16, StringUtils.__UTF16, "%00%30"},
                };

        // Note: "%30" -> decode -> "0"

        for (int i = 0; i < charsets.length; i++) {
            ByteArrayInputStream in = new ByteArrayInputStream(("name\n=value+" + charsets[i][2] + "&name1=&name2&n\u00e3me3=value+3").getBytes(charsets[i][0]));
            MultiMap<String> m = new MultiMap<>();
            UrlEncoded.decodeTo(in, m, charsets[i][1] == null ? null : Charset.forName(charsets[i][1]), -1, -1);
            assertEquals(charsets[i][1] + " stream length", 4, m.size());
            assertThat(charsets[i][1] + " stream name\\n", m.getString("name\n"), is("value 0"));
            assertThat(charsets[i][1] + " stream name1", m.getString("name1"), is(""));
            assertThat(charsets[i][1] + " stream name2", m.getString("name2"), is(""));
            assertThat(charsets[i][1] + " stream n\u00e3me3", m.getString("n\u00e3me3"), is("value 3"));
        }


        if (java.nio.charset.Charset.isSupported("Shift_JIS")) {
            ByteArrayInputStream in2 = new ByteArrayInputStream("name=%83e%83X%83g".getBytes(StandardCharsets.ISO_8859_1));
            MultiMap<String> m2 = new MultiMap<>();
            UrlEncoded.decodeTo(in2, m2, Charset.forName("Shift_JIS"), -1, -1);
            assertEquals("stream length", 1, m2.size());
            assertEquals("stream name", "\u30c6\u30b9\u30c8", m2.getString("name"));
        } else
            assertTrue("Charset Shift_JIS not supported by jvm", true);

    }

    /* -------------------------------------------------------------- */
    @Test
    public void testUtf8()
            throws Exception {
        UrlEncoded url_encoded = new UrlEncoded();
        assertEquals("Empty", 0, url_encoded.size());

        url_encoded.clear();
        url_encoded.decode("text=%E0%B8%9F%E0%B8%AB%E0%B8%81%E0%B8%A7%E0%B8%94%E0%B8%B2%E0%B9%88%E0%B8%81%E0%B8%9F%E0%B8%A7%E0%B8%AB%E0%B8%AA%E0%B8%94%E0%B8%B2%E0%B9%88%E0%B8%AB%E0%B8%9F%E0%B8%81%E0%B8%A7%E0%B8%94%E0%B8%AA%E0%B8%B2%E0%B8%9F%E0%B8%81%E0%B8%AB%E0%B8%A3%E0%B8%94%E0%B9%89%E0%B8%9F%E0%B8%AB%E0%B8%99%E0%B8%81%E0%B8%A3%E0%B8%94%E0%B8%B5&Action=Submit");

        String hex = "E0B89FE0B8ABE0B881E0B8A7E0B894E0B8B2E0B988E0B881E0B89FE0B8A7E0B8ABE0B8AAE0B894E0B8B2E0B988E0B8ABE0B89FE0B881E0B8A7E0B894E0B8AAE0B8B2E0B89FE0B881E0B8ABE0B8A3E0B894E0B989E0B89FE0B8ABE0B899E0B881E0B8A3E0B894E0B8B5";
        String expected = new String(TypeUtils.fromHexString(hex), "utf-8");
        Assert.assertEquals(expected, url_encoded.getString("text"));
    }

    @Test
    public void testUtf8_MultiByteCodePoint() {
        String input = "text=test%C3%A4";
        UrlEncoded url_encoded = new UrlEncoded();
        url_encoded.decode(input);

        // http://www.ltg.ed.ac.uk/~richard/utf-8.cgi?input=00e4&mode=hex
        // Should be "testä"
        // "test" followed by a LATIN SMALL LETTER A WITH DIAERESIS

        String expected = "test\u00e4";
        assertThat(url_encoded.getString("text"), is(expected));
    }
}
