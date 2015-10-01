
package test.codec.http2.encode;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.Assert;
import org.junit.Test;

import com.firefly.codec.http2.encode.UrlEncoded;
import com.firefly.utils.collection.MultiMap;
import com.firefly.utils.lang.Utf8Appendable;

public class UrlEncodedUtf8Test {

	@Test
	public void testIncompleteSequestAtTheEnd() throws Exception {
		byte[] bytes = { 97, 98, 61, 99, -50 };
		String test = new String(bytes, StandardCharsets.UTF_8);
		String expected = "c" + Utf8Appendable.REPLACEMENT;

		fromString(test, test, "ab", expected, false);
		fromInputStream(test, bytes, "ab", expected, false);
	}

	@Test
	public void testIncompleteSequestAtTheEnd2() throws Exception {
		byte[] bytes = { 97, 98, 61, -50 };
		String test = new String(bytes, StandardCharsets.UTF_8);
		String expected = "" + Utf8Appendable.REPLACEMENT;

		fromString(test, test, "ab", expected, false);
		fromInputStream(test, bytes, "ab", expected, false);

	}

	@Test
	public void testIncompleteSequestInName() throws Exception {
		byte[] bytes = { 101, -50, 61, 102, 103, 38, 97, 98, 61, 99, 100 };
		String test = new String(bytes, StandardCharsets.UTF_8);
		String name = "e" + Utf8Appendable.REPLACEMENT;
		String value = "fg";

		fromString(test, test, name, value, false);
		fromInputStream(test, bytes, name, value, false);
	}

	@Test
	public void testIncompleteSequestInValue() throws Exception {
		byte[] bytes = { 101, 102, 61, 103, -50, 38, 97, 98, 61, 99, 100 };
		String test = new String(bytes, StandardCharsets.UTF_8);
		String name = "ef";
		String value = "g" + Utf8Appendable.REPLACEMENT;

		fromString(test, test, name, value, false);
		fromInputStream(test, bytes, name, value, false);
	}

	@Test
	public void testCorrectUnicode() throws Exception {
		String chars = "a=%u0061";
		byte[] bytes = chars.getBytes(StandardCharsets.UTF_8);
		String test = new String(bytes, StandardCharsets.UTF_8);
		String name = "a";
		String value = "a";

		fromString(test, test, name, value, false);
		fromInputStream(test, bytes, name, value, false);
	}

	@Test
	public void testIncompleteUnicode() throws Exception {
		String chars = "a=%u0";
		byte[] bytes = chars.getBytes(StandardCharsets.UTF_8);
		String test = new String(bytes, StandardCharsets.UTF_8);
		String name = "a";
		String value = "" + Utf8Appendable.REPLACEMENT;

		fromString(test, test, name, value, false);
		fromInputStream(test, bytes, name, value, false);
	}

	@Test
	public void testIncompletePercent() throws Exception {
		String chars = "a=%A";
		byte[] bytes = chars.getBytes(StandardCharsets.UTF_8);
		String test = new String(bytes, StandardCharsets.UTF_8);
		String name = "a";
		String value = "" + Utf8Appendable.REPLACEMENT;

		fromString(test, test, name, value, false);
		fromInputStream(test, bytes, name, value, false);
	}

	static void fromString(String test, String s, String field, String expected, boolean thrown) throws Exception {
		MultiMap<String> values = new MultiMap<>();
		try {
			UrlEncoded.decodeUtf8To(s, 0, s.length(), values);
			if (thrown)
				Assert.fail();
			Assert.assertEquals(test, expected, values.getString(field));
		} catch (Exception e) {
			if (!thrown)
				throw e;
		}
	}

	static void fromInputStream(String test, byte[] b, String field, String expected, boolean thrown) throws Exception {
		InputStream is = new ByteArrayInputStream(b);
		MultiMap<String> values = new MultiMap<>();
		try {
			UrlEncoded.decodeUtf8To(is, values, 1000000, -1);
			if (thrown)
				Assert.fail();
			Assert.assertEquals(test, expected, values.getString(field));
		} catch (Exception e) {
			if (!thrown)
				throw e;
		}
	}

}
