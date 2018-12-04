package com.fireflysource.common.string;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TestStringUtils {

    @Test
    void testAsciiToLowerCase() {
        String lc = "\u0690bc def 1\u06903";
        assertEquals(StringUtils.asciiToLowerCase("\u0690Bc DeF 1\u06903"), lc);
        assertEquals(StringUtils.asciiToLowerCase(lc), lc);
    }

    @Test
    void testAppend() {
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
    void testSplit() {
        String byteRangeSet = "500-";
        String[] byteRangeSets = StringUtils.split(byteRangeSet, ',');
        System.out.println(Arrays.toString(byteRangeSets));
        assertEquals(byteRangeSets.length, 1);

        byteRangeSet = "500-,";
        byteRangeSets = StringUtils.split(byteRangeSet, ',');
        System.out.println(Arrays.toString(byteRangeSets));
        assertEquals(byteRangeSets.length, 1);

        byteRangeSet = ",500-,";
        byteRangeSets = StringUtils.split(byteRangeSet, ',');
        System.out.println(Arrays.toString(byteRangeSets));
        assertEquals(byteRangeSets.length, 1);

        byteRangeSet = ",500-,";
        byteRangeSets = StringUtils.split(byteRangeSet, ",");
        System.out.println(Arrays.toString(byteRangeSets));
        assertEquals(byteRangeSets.length, 1);

        byteRangeSet = ",500-";
        byteRangeSets = StringUtils.split(byteRangeSet, ',');
        System.out.println(Arrays.toString(byteRangeSets));
        assertEquals(byteRangeSets.length, 1);

        byteRangeSet = "500-700,601-999,";
        byteRangeSets = StringUtils.split(byteRangeSet, ',');
        assertEquals(byteRangeSets.length, 2);

        byteRangeSet = "500-700,,601-999,";
        byteRangeSets = StringUtils.split(byteRangeSet, ',');
        assertEquals(byteRangeSets.length, 2);

        String tmp = "hello#$world#%test#$eee";
        String[] tmps = StringUtils.splitByWholeSeparator(tmp, "#$");
        System.out.println(Arrays.toString(tmps));
        assertEquals(tmps.length, 3);

        tmp = "hello#$";
        tmps = StringUtils.splitByWholeSeparator(tmp, "#$");
        System.out.println(Arrays.toString(tmps));
        assertEquals(tmps.length, 1);

        tmp = "#$hello#$";
        tmps = StringUtils.splitByWholeSeparator(tmp, "#$");
        System.out.println(Arrays.toString(tmps));
        assertEquals(tmps.length, 1);

        tmp = "#$hello";
        tmps = StringUtils.splitByWholeSeparator(tmp, "#$");
        System.out.println(Arrays.toString(tmps));
        assertEquals(tmps.length, 1);

        tmp = "#$hello#$world#$";
        tmps = StringUtils.splitByWholeSeparator(tmp, "#$");
        System.out.println(Arrays.toString(tmps));
        assertEquals(tmps.length, 2);

        tmp = "#$hello#$#$world#$";
        tmps = StringUtils.splitByWholeSeparator(tmp, "#$");
        System.out.println(Arrays.toString(tmps));
        assertEquals(tmps.length, 2);

    }

    @Test
    void testHasText() {
        String str = "\r\n\t\t";
        assertTrue(StringUtils.hasLength(str));
        assertFalse(StringUtils.hasText(str));
        str = null;
        assertFalse(StringUtils.hasText(str));
    }

    @Test
    void testReplace() {
        String str = "hello ${t1} and ${t2} s";
        Map<String, Object> map = new HashMap<>();
        map.put("t1", "foo");
        map.put("t2", "bar");
        String ret = StringUtils.replace(str, map);
        assertEquals(ret, "hello foo and bar s");

        map = new HashMap<>();
        map.put("t1", "foo");
        map.put("t2", "${dddd}");
        ret = StringUtils.replace(str, map);
        assertEquals(ret, "hello foo and ${dddd} s");

        map = new HashMap<>();
        map.put("t1", null);
        map.put("t2", "${dddd}");
        ret = StringUtils.replace(str, map);
        assertEquals(ret, "hello null and ${dddd} s");

        map = new HashMap<>();
        map.put("t1", 33);
        map.put("t2", 42L);
        ret = StringUtils.replace(str, map);
        assertEquals(ret, "hello 33 and 42 s");
    }

    @Test
    void testReplace2() {
        String str2 = "hello {{{{} and {} mm";
        String ret2 = StringUtils.replace(str2, "foo", "bar");
        assertEquals(ret2, "hello {{{foo and bar mm");

        ret2 = StringUtils.replace(str2, "foo");
        assertEquals(ret2, "hello {{{foo and {} mm");

        ret2 = StringUtils.replace(str2, "foo", "bar", "foo2");
        assertEquals(ret2, "hello {{{foo and bar mm");

        ret2 = StringUtils.replace(str2, 12, 23L, 33);
        assertEquals(ret2, "hello {{{12 and 23 mm");
    }

}
