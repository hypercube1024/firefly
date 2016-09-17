package test.utils;

import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.emptyArray;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.firefly.utils.StringUtils;

public class TestStringUtils {
	@Test
	public void testAsciiToLowerCase() {
		String lc = "\u0690bc def 1\u06903";
		assertEquals(StringUtils.asciiToLowerCase("\u0690Bc DeF 1\u06903"), lc);
		assertTrue(StringUtils.asciiToLowerCase(lc) == lc);
	}

	@Test
	public void testAppend() {
		StringBuilder buf = new StringBuilder();
		buf.append('a');
		StringUtils.append(buf, "abc", 1, 1);
		StringUtils.append(buf, (byte) 12, 16);
		StringUtils.append(buf, (byte) 16, 16);
		StringUtils.append(buf, (byte) -1, 16);
		StringUtils.append(buf, (byte) -16, 16);
		assertEquals("ab0c10fff0", buf.toString());
	}

	@Test
	public void testCsvSplit() {
		assertThat(StringUtils.csvSplit(null), nullValue());
		assertThat(StringUtils.csvSplit(null), nullValue());

		assertThat(StringUtils.csvSplit(""), emptyArray());
		assertThat(StringUtils.csvSplit(" \t\n"), emptyArray());

		assertThat(StringUtils.csvSplit("aaa"), arrayContaining("aaa"));
		assertThat(StringUtils.csvSplit(" \taaa\n"), arrayContaining("aaa"));
		assertThat(StringUtils.csvSplit(" \ta\n"), arrayContaining("a"));
		assertThat(StringUtils.csvSplit(" \t\u1234\n"), arrayContaining("\u1234"));

		assertThat(StringUtils.csvSplit("aaa,bbb,ccc"), arrayContaining("aaa", "bbb", "ccc"));
		assertThat(StringUtils.csvSplit("aaa,,ccc"), arrayContaining("aaa", "", "ccc"));
		assertThat(StringUtils.csvSplit(",b b,"), arrayContaining("", "b b"));
		assertThat(StringUtils.csvSplit(",,bbb,,"), arrayContaining("", "", "bbb", ""));

		assertThat(StringUtils.csvSplit(" aaa, bbb, ccc"), arrayContaining("aaa", "bbb", "ccc"));
		assertThat(StringUtils.csvSplit("aaa,\t,ccc"), arrayContaining("aaa", "", "ccc"));
		assertThat(StringUtils.csvSplit("  ,  b b  ,   "), arrayContaining("", "b b"));
		assertThat(StringUtils.csvSplit(" ,\n,bbb, , "), arrayContaining("", "", "bbb", ""));

		assertThat(StringUtils.csvSplit("\"aaa\", \" b,\\\"\",\"\""), arrayContaining("aaa", " b,\"", ""));
	}

	@Test
	public void testSplit() {
		String byteRangeSet = "500-";
		String[] byteRangeSets = StringUtils.split(byteRangeSet, ',');
		System.out.println(Arrays.toString(byteRangeSets));
		Assert.assertThat(byteRangeSets.length, is(1));

		byteRangeSet = "500-,";
		byteRangeSets = StringUtils.split(byteRangeSet, ',');
		System.out.println(Arrays.toString(byteRangeSets));
		Assert.assertThat(byteRangeSets.length, is(1));

		byteRangeSet = ",500-,";
		byteRangeSets = StringUtils.split(byteRangeSet, ',');
		System.out.println(Arrays.toString(byteRangeSets));
		Assert.assertThat(byteRangeSets.length, is(1));

		byteRangeSet = ",500-,";
		byteRangeSets = StringUtils.split(byteRangeSet, ",");
		System.out.println(Arrays.toString(byteRangeSets));
		Assert.assertThat(byteRangeSets.length, is(1));

		byteRangeSet = ",500-";
		byteRangeSets = StringUtils.split(byteRangeSet, ',');
		System.out.println(Arrays.toString(byteRangeSets));
		Assert.assertThat(byteRangeSets.length, is(1));

		byteRangeSet = "500-700,601-999,";
		byteRangeSets = StringUtils.split(byteRangeSet, ',');
		Assert.assertThat(byteRangeSets.length, is(2));

		byteRangeSet = "500-700,,601-999,";
		byteRangeSets = StringUtils.split(byteRangeSet, ',');
		Assert.assertThat(byteRangeSets.length, is(2));

		String tmp = "hello#$world#%test#$eee";
		String[] tmps = StringUtils.splitByWholeSeparator(tmp, "#$");
		System.out.println(Arrays.toString(tmps));
		Assert.assertThat(tmps.length, is(3));

		tmp = "hello#$";
		tmps = StringUtils.splitByWholeSeparator(tmp, "#$");
		System.out.println(Arrays.toString(tmps));
		Assert.assertThat(tmps.length, is(1));

		tmp = "#$hello#$";
		tmps = StringUtils.splitByWholeSeparator(tmp, "#$");
		System.out.println(Arrays.toString(tmps));
		Assert.assertThat(tmps.length, is(1));

		tmp = "#$hello";
		tmps = StringUtils.splitByWholeSeparator(tmp, "#$");
		System.out.println(Arrays.toString(tmps));
		Assert.assertThat(tmps.length, is(1));

		tmp = "#$hello#$world#$";
		tmps = StringUtils.splitByWholeSeparator(tmp, "#$");
		System.out.println(Arrays.toString(tmps));
		Assert.assertThat(tmps.length, is(2));

		tmp = "#$hello#$#$world#$";
		tmps = StringUtils.splitByWholeSeparator(tmp, "#$");
		System.out.println(Arrays.toString(tmps));
		Assert.assertThat(tmps.length, is(2));

	}

	@Test
	public void testHasText() {
		String str = "\r\n\t\t";
		Assert.assertThat(StringUtils.hasLength(str), is(true));
		Assert.assertThat(StringUtils.hasText(str), is(false));
		str = null;
		Assert.assertThat(StringUtils.hasText(str), is(false));
	}

	@Test
	public void testReplace() {
		String str = "hello ${t1} and ${t2} s";
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("t1", "foo");
		map.put("t2", "bar");
		String ret = StringUtils.replace(str, map);
		Assert.assertThat(ret, is("hello foo and bar s"));

		map = new HashMap<String, Object>();
		map.put("t1", "foo");
		map.put("t2", "${dddd}");
		ret = StringUtils.replace(str, map);
		Assert.assertThat(ret, is("hello foo and ${dddd} s"));

		map = new HashMap<String, Object>();
		map.put("t1", null);
		map.put("t2", "${dddd}");
		ret = StringUtils.replace(str, map);
		Assert.assertThat(ret, is("hello null and ${dddd} s"));

		map = new HashMap<String, Object>();
		map.put("t1", 33);
		map.put("t2", 42L);
		ret = StringUtils.replace(str, map);
		Assert.assertThat(ret, is("hello 33 and 42 s"));
	}

	@Test
	public void testReplace2() {
		String str2 = "hello {{{{} and {} mm";
		String ret2 = StringUtils.replace(str2, "foo", "bar");
		Assert.assertThat(ret2, is("hello {{{foo and bar mm"));

		ret2 = StringUtils.replace(str2, "foo");
		Assert.assertThat(ret2, is("hello {{{foo and {} mm"));

		ret2 = StringUtils.replace(str2, "foo", "bar", "foo2");
		Assert.assertThat(ret2, is("hello {{{foo and bar mm"));

		ret2 = StringUtils.replace(str2, 12, 23L, 33);
		Assert.assertThat(ret2, is("hello {{{12 and 23 mm"));	
	}

	public static void main(String[] args) {
		String str = "Replace the pattern using a map, such as a pattern, such as A pattern is 'hello ${foo}' and the map is {'foo' : 'world'}, when you execute this function, the result is 'hello world'";
		System.out.println(StringUtils.escapeXML(str));

	}

	public static void main2(String[] args) {
		String str = "hello ${t1} and ${t2}";
		Map<String, String> map = new HashMap<String, String>();
		map.put("t1", "foo");
		map.put("t2", "bar");
		String ret = StringUtils.replace(str, map);
		System.out.println(ret);

		map = new HashMap<String, String>();
		map.put("t1", "foo");
		map.put("t2", "${dddd}");
		ret = StringUtils.replace(str, map);
		System.out.println(ret);

		map = new HashMap<String, String>();
		map.put("t1", "foo");
		map.put("t2", null);
		ret = StringUtils.replace(str, map);
		System.out.println(ret);

		String str2 = "hello {{{{} and {} mm";
		String ret2 = StringUtils.replace(str2, "foo", "bar");
		System.out.println(ret2);

		ret2 = StringUtils.replace(str2, "foo");
		System.out.println(ret2);

		ret2 = StringUtils.replace(str2, "foo", "bar", "foo2");
		System.out.println(ret2);

		String r = "-500";
		System.out.println(StringUtils.split(r, '-')[0] + "|" + StringUtils.split(r, '-').length);
	}
}
