package com.fireflysource.common.string;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TestStringUtils {

    static final String WHITESPACE;
    static final String NON_WHITESPACE;
    static final String HARD_SPACE;
    static final String TRIMMABLE;
    static final String NON_TRIMMABLE;

    static {
        String ws = "";
        String nws = "";
        final String hs = String.valueOf(((char) 160));
        String tr = "";
        String ntr = "";
        for (int i = 0; i < Character.MAX_VALUE; i++) {
            if (Character.isWhitespace((char) i)) {
                ws += String.valueOf((char) i);
                if (i > 32) {
                    ntr += String.valueOf((char) i);
                }
            } else if (i < 40) {
                nws += String.valueOf((char) i);
            }
        }
        for (int i = 0; i <= 32; i++) {
            tr += String.valueOf((char) i);
        }
        WHITESPACE = ws;
        NON_WHITESPACE = nws;
        HARD_SPACE = hs;
        TRIMMABLE = tr;
        NON_TRIMMABLE = ntr;
    }

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
    void testSplit_String() {
        assertNull(StringUtils.split(null));
        assertEquals(0, StringUtils.split("").length);

        String str = "a b  .c";
        String[] res = StringUtils.split(str);
        assertEquals(3, res.length);
        assertEquals("a", res[0]);
        assertEquals("b", res[1]);
        assertEquals(".c", res[2]);

        str = " a ";
        res = StringUtils.split(str);
        assertEquals(1, res.length);
        assertEquals("a", res[0]);

        str = "a" + WHITESPACE + "b" + NON_WHITESPACE + "c";
        res = StringUtils.split(str);
        assertEquals(2, res.length);
        assertEquals("a", res[0]);
        assertEquals("b" + NON_WHITESPACE + "c", res[1]);
    }

    @Test
    void testSplit_StringChar() {
        assertNull(StringUtils.split(null, '.'));
        assertEquals(0, StringUtils.split("", '.').length);

        String str = "a.b.. c";
        String[] res = StringUtils.split(str, '.');
        assertEquals(3, res.length);
        assertEquals("a", res[0]);
        assertEquals("b", res[1]);
        assertEquals(" c", res[2]);

        str = ".a.";
        res = StringUtils.split(str, '.');
        assertEquals(1, res.length);
        assertEquals("a", res[0]);

        str = "a b c";
        res = StringUtils.split(str, ' ');
        assertEquals(3, res.length);
        assertEquals("a", res[0]);
        assertEquals("b", res[1]);
        assertEquals("c", res[2]);
    }

    @Test
    void testSplit_StringString_StringStringInt() {
        assertNull(StringUtils.split(null, "."));
        assertNull(StringUtils.split(null, ".", 3));

        assertEquals(0, StringUtils.split("", ".").length);
        assertEquals(0, StringUtils.split("", ".", 3).length);

        innerTestSplit('.', ".", ' ');
        innerTestSplit('.', ".", ',');
        innerTestSplit('.', ".,", 'x');
        for (int i = 0; i < WHITESPACE.length(); i++) {
            for (int j = 0; j < NON_WHITESPACE.length(); j++) {
                innerTestSplit(WHITESPACE.charAt(i), null, NON_WHITESPACE.charAt(j));
                innerTestSplit(WHITESPACE.charAt(i), String.valueOf(WHITESPACE.charAt(i)), NON_WHITESPACE.charAt(j));
            }
        }

        String[] results;
        final String[] expectedResults = {"ab", "de fg"};
        results = StringUtils.split("ab   de fg", null, 2);
        assertEquals(expectedResults.length, results.length);
        for (int i = 0; i < expectedResults.length; i++) {
            assertEquals(expectedResults[i], results[i]);
        }

        final String[] expectedResults2 = {"ab", "cd:ef"};
        results = StringUtils.split("ab:cd:ef", ":", 2);
        assertEquals(expectedResults2.length, results.length);
        for (int i = 0; i < expectedResults2.length; i++) {
            assertEquals(expectedResults2[i], results[i]);
        }
    }

    private void innerTestSplit(final char separator, final String sepStr, final char noMatch) {
        final String msg = "Failed on separator hex(" + Integer.toHexString(separator) +
                "), noMatch hex(" + Integer.toHexString(noMatch) + "), sepStr(" + sepStr + ")";

        final String str = "a" + separator + "b" + separator + separator + noMatch + "c";
        String[] res;
        // (str, sepStr)
        res = StringUtils.split(str, sepStr);
        assertEquals(3, res.length, msg);
        assertEquals("a", res[0]);
        assertEquals("b", res[1]);
        assertEquals(noMatch + "c", res[2]);

        final String str2 = separator + "a" + separator;
        res = StringUtils.split(str2, sepStr);
        assertEquals(1, res.length, msg);
        assertEquals("a", res[0], msg);

        res = StringUtils.split(str, sepStr, -1);
        assertEquals(3, res.length, msg);
        assertEquals("a", res[0], msg);
        assertEquals("b", res[1], msg);
        assertEquals(noMatch + "c", res[2], msg);

        res = StringUtils.split(str, sepStr, 0);
        assertEquals(3, res.length, msg);
        assertEquals("a", res[0], msg);
        assertEquals("b", res[1], msg);
        assertEquals(noMatch + "c", res[2], msg);

        res = StringUtils.split(str, sepStr, 1);
        assertEquals(1, res.length, msg);
        assertEquals(str, res[0], msg);

        res = StringUtils.split(str, sepStr, 2);
        assertEquals(2, res.length, msg);
        assertEquals("a", res[0], msg);
        assertEquals(str.substring(2), res[1], msg);
    }

    @Test
    void testSplitByWholeString_StringStringBoolean() {
        assertArrayEquals(null, StringUtils.splitByWholeSeparator(null, "."));

        assertEquals(0, StringUtils.splitByWholeSeparator("", ".").length);

        final String stringToSplitOnNulls = "ab   de fg";
        final String[] splitOnNullExpectedResults = {"ab", "de", "fg"};

        final String[] splitOnNullResults = StringUtils.splitByWholeSeparator(stringToSplitOnNulls, null);
        assertEquals(splitOnNullExpectedResults.length, splitOnNullResults.length);
        for (int i = 0; i < splitOnNullExpectedResults.length; i += 1) {
            assertEquals(splitOnNullExpectedResults[i], splitOnNullResults[i]);
        }

        final String stringToSplitOnCharactersAndString = "abstemiouslyaeiouyabstemiously";

        final String[] splitOnStringExpectedResults = {"abstemiously", "abstemiously"};
        final String[] splitOnStringResults = StringUtils.splitByWholeSeparator(stringToSplitOnCharactersAndString, "aeiouy");
        assertEquals(splitOnStringExpectedResults.length, splitOnStringResults.length);
        for (int i = 0; i < splitOnStringExpectedResults.length; i += 1) {
            assertEquals(splitOnStringExpectedResults[i], splitOnStringResults[i]);
        }

        final String[] splitWithMultipleSeparatorExpectedResults = {"ab", "cd", "ef"};
        final String[] splitWithMultipleSeparator = StringUtils.splitByWholeSeparator("ab:cd::ef", ":");
        assertEquals(splitWithMultipleSeparatorExpectedResults.length, splitWithMultipleSeparator.length);
        for (int i = 0; i < splitWithMultipleSeparatorExpectedResults.length; i++) {
            assertEquals(splitWithMultipleSeparatorExpectedResults[i], splitWithMultipleSeparator[i]);
        }
    }

    @Test
    void testSplitByWholeString_StringStringBooleanInt() {
        assertArrayEquals(null, StringUtils.splitByWholeSeparator(null, ".", 3));

        assertEquals(0, StringUtils.splitByWholeSeparator("", ".", 3).length);

        final String stringToSplitOnNulls = "ab   de fg";
        final String[] splitOnNullExpectedResults = {"ab", "de fg"};
        //String[] splitOnNullExpectedResults = { "ab", "de" } ;

        final String[] splitOnNullResults = StringUtils.splitByWholeSeparator(stringToSplitOnNulls, null, 2);
        assertEquals(splitOnNullExpectedResults.length, splitOnNullResults.length);
        for (int i = 0; i < splitOnNullExpectedResults.length; i += 1) {
            assertEquals(splitOnNullExpectedResults[i], splitOnNullResults[i]);
        }

        final String stringToSplitOnCharactersAndString = "abstemiouslyaeiouyabstemiouslyaeiouyabstemiously";

        final String[] splitOnStringExpectedResults = {"abstemiously", "abstemiouslyaeiouyabstemiously"};
        //String[] splitOnStringExpectedResults = { "abstemiously", "abstemiously" } ;
        final String[] splitOnStringResults = StringUtils.splitByWholeSeparator(stringToSplitOnCharactersAndString, "aeiouy", 2);
        assertEquals(splitOnStringExpectedResults.length, splitOnStringResults.length);
        for (int i = 0; i < splitOnStringExpectedResults.length; i++) {
            assertEquals(splitOnStringExpectedResults[i], splitOnStringResults[i]);
        }
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
