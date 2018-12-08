package com.fireflysource.common.string;

import java.nio.charset.StandardCharsets;
import java.util.*;

public class StringUtils {

    public static final String EMPTY = "";
    public static final String[] EMPTY_STRING_ARRAY = new String[0];

    private static final String FOLDER_SEPARATOR = "/";
    private static final char EXTENSION_SEPARATOR = '.';

    /**
     * <p>
     * Splits the provided text into an array, using whitespace as the
     * separator. Whitespace is defined by {@link Character#isWhitespace(char)}.
     * </p>
     * <p>
     * <p>
     * The separator is not included in the returned String array. Adjacent
     * separators are treated as one separator. For more control over the split
     * use the StrTokenizer class.
     * </p>
     * <p>
     * <p>
     * A <code>null</code> input String returns <code>null</code>.
     * </p>
     * <p>
     * <pre>
     * StringUtils.split(null)       = null
     * StringUtils.split("")         = []
     * StringUtils.split("abc def")  = ["abc", "def"]
     * StringUtils.split("abc  def") = ["abc", "def"]
     * StringUtils.split(" abc ")    = ["abc"]
     * </pre>
     *
     * @param str the String to parse, may be null
     * @return an array of parsed Strings, <code>null</code> if null String
     * input
     */
    public static String[] split(String str) {
        return split(str, null, -1);
    }

    /**
     * <p>
     * Splits the provided text into an array, separators specified. This is an
     * alternative to using StringTokenizer.
     * </p>
     * <p>
     * <p>
     * The separator is not included in the returned String array. Adjacent
     * separators are treated as one separator. For more control over the split
     * use the StrTokenizer class.
     * </p>
     * <p>
     * <p>
     * A <code>null</code> input String returns <code>null</code>. A
     * <code>null</code> separatorChars splits on whitespace.
     * </p>
     * <p>
     * <pre>
     * StringUtils.split(null, *)         = null
     * StringUtils.split("", *)           = []
     * StringUtils.split("abc def", null) = ["abc", "def"]
     * StringUtils.split("abc def", " ")  = ["abc", "def"]
     * StringUtils.split("abc  def", " ") = ["abc", "def"]
     * StringUtils.split("ab:cd:ef", ":") = ["ab", "cd", "ef"]
     * </pre>
     *
     * @param str            the String to parse, may be null
     * @param separatorChars the characters used as the delimiters, <code>null</code>
     *                       splits on whitespace
     * @return an array of parsed Strings, <code>null</code> if null String
     * input
     */
    public static String[] split(String str, String separatorChars) {
        return splitWorker(str, separatorChars, -1, false);
    }

    /**
     * <p>
     * Splits the provided text into an array, separator specified. This is an
     * alternative to using StringTokenizer.
     * </p>
     * <p>
     * <p>
     * The separator is not included in the returned String array. Adjacent
     * separators are treated as one separator. For more control over the split
     * use the StrTokenizer class.
     * </p>
     * <p>
     * <p>
     * A <code>null</code> input String returns <code>null</code>.
     * </p>
     * <p>
     * <pre>
     * StringUtils.split(null, *)         = null
     * StringUtils.split("", *)           = []
     * StringUtils.split("a.b.c", '.')    = ["a", "b", "c"]
     * StringUtils.split("a..b.c", '.')   = ["a", "b", "c"]
     * StringUtils.split("a:b:c", '.')    = ["a:b:c"]
     * StringUtils.split("a b c", ' ')    = ["a", "b", "c"]
     * </pre>
     *
     * @param str           the String to parse, may be null
     * @param separatorChar the character used as the delimiter
     * @return an array of parsed Strings, <code>null</code> if null String
     * input
     * @since 2.0
     */
    public static String[] split(String str, char separatorChar) {
        return splitWorker(str, separatorChar, false);
    }

    /**
     * <p>
     * Splits the provided text into an array with a maximum length, separators
     * specified.
     * </p>
     * <p>
     * <p>
     * The separator is not included in the returned String array. Adjacent
     * separators are treated as one separator.
     * </p>
     * <p>
     * <p>
     * A <code>null</code> input String returns <code>null</code>. A
     * <code>null</code> separatorChars splits on whitespace.
     * </p>
     * <p>
     * <p>
     * If more than <code>max</code> delimited substrings are found, the last
     * returned string includes all characters after the first
     * <code>max - 1</code> returned strings (including separator characters).
     * </p>
     * <p>
     * <pre>
     * StringUtils.split(null, *, *)            = null
     * StringUtils.split("", *, *)              = []
     * StringUtils.split("ab de fg", null, 0)   = ["ab", "cd", "ef"]
     * StringUtils.split("ab   de fg", null, 0) = ["ab", "cd", "ef"]
     * StringUtils.split("ab:cd:ef", ":", 0)    = ["ab", "cd", "ef"]
     * StringUtils.split("ab:cd:ef", ":", 2)    = ["ab", "cd:ef"]
     * </pre>
     *
     * @param str            the String to parse, may be null
     * @param separatorChars the characters used as the delimiters, <code>null</code>
     *                       splits on whitespace
     * @param max            the maximum number of elements to include in the array. A zero
     *                       or negative value implies no limit
     * @return an array of parsed Strings, <code>null</code> if null String
     * input
     */
    public static String[] split(String str, String separatorChars, int max) {
        return splitWorker(str, separatorChars, max, false);
    }

    /**
     * Performs the logic for the <code>split</code> and
     * <code>splitPreserveAllTokens</code> methods that return a maximum array
     * length.
     *
     * @param str               the String to parse, may be <code>null</code>
     * @param separatorChars    the separate character
     * @param max               the maximum number of elements to include in the array. A zero
     *                          or negative value implies no limit.
     * @param preserveAllTokens if <code>true</code>, adjacent separators are treated as empty
     *                          token separators; if <code>false</code>, adjacent separators
     *                          are treated as one separator.
     * @return an array of parsed Strings, <code>null</code> if null String
     * input
     */
    private static String[] splitWorker(String str, String separatorChars, int max, boolean preserveAllTokens) {
        // Performance tuned for 2.0 (JDK1.4)
        // Direct code is quicker than StringTokenizer.
        // Also, StringTokenizer uses isSpace() not isWhitespace()

        if (str == null) {
            return null;
        }
        int len = str.length();
        if (len == 0) {
            return EMPTY_STRING_ARRAY;
        }
        List<String> list = new ArrayList<>();
        int sizePlus1 = 1;
        int i = 0, start = 0;
        boolean match = false;
        boolean lastMatch = false;
        if (separatorChars == null) {
            // Null separator means use whitespace
            while (i < len) {
                if (Character.isWhitespace(str.charAt(i))) {
                    if (match || preserveAllTokens) {
                        lastMatch = true;
                        if (sizePlus1++ == max) {
                            i = len;
                            lastMatch = false;
                        }
                        list.add(str.substring(start, i));
                        match = false;
                    }
                    start = ++i;
                    continue;
                }
                lastMatch = false;
                match = true;
                i++;
            }
        } else if (separatorChars.length() == 1) {
            // Optimise 1 character case
            char sep = separatorChars.charAt(0);
            while (i < len) {
                if (str.charAt(i) == sep) {
                    if (match || preserveAllTokens) {
                        lastMatch = true;
                        if (sizePlus1++ == max) {
                            i = len;
                            lastMatch = false;
                        }
                        list.add(str.substring(start, i));
                        match = false;
                    }
                    start = ++i;
                    continue;
                }
                lastMatch = false;
                match = true;
                i++;
            }
        } else {
            // standard case
            while (i < len) {
                if (separatorChars.indexOf(str.charAt(i)) >= 0) {
                    if (match || preserveAllTokens) {
                        lastMatch = true;
                        if (sizePlus1++ == max) {
                            i = len;
                            lastMatch = false;
                        }
                        list.add(str.substring(start, i));
                        match = false;
                    }
                    start = ++i;
                    continue;
                }
                lastMatch = false;
                match = true;
                i++;
            }
        }
        if (match || (preserveAllTokens && lastMatch)) {
            list.add(str.substring(start, i));
        }
        return list.toArray(EMPTY_STRING_ARRAY);
    }

    /**
     * Performs the logic for the <code>split</code> and
     * <code>splitPreserveAllTokens</code> methods that do not return a maximum
     * array length.
     *
     * @param str               the String to parse, may be <code>null</code>
     * @param separatorChar     the separate character
     * @param preserveAllTokens if <code>true</code>, adjacent separators are treated as empty
     *                          token separators; if <code>false</code>, adjacent separators
     *                          are treated as one separator.
     * @return an array of parsed Strings, <code>null</code> if null String
     * input
     */
    private static String[] splitWorker(String str, char separatorChar, boolean preserveAllTokens) {
        // Performance tuned for 2.0 (JDK1.4)

        if (str == null) {
            return null;
        }
        int len = str.length();
        if (len == 0) {
            return EMPTY_STRING_ARRAY;
        }
        List<String> list = new ArrayList<>();
        int i = 0, start = 0;
        boolean match = false;
        boolean lastMatch = false;
        while (i < len) {
            if (str.charAt(i) == separatorChar) {
                if (match || preserveAllTokens) {
                    list.add(str.substring(start, i));
                    match = false;
                    lastMatch = true;
                }
                start = ++i;
                continue;
            }
            lastMatch = false;
            match = true;
            i++;
        }
        if (match || (preserveAllTokens && lastMatch)) {
            list.add(str.substring(start, i));
        }
        return list.toArray(EMPTY_STRING_ARRAY);
    }

    /**
     * <p>
     * Splits the provided text into an array, separator string specified.
     * </p>
     * <p>
     * <p>
     * The separator(s) will not be included in the returned String array.
     * Adjacent separators are treated as one separator.
     * </p>
     * <p>
     * <p>
     * A <code>null</code> input String returns <code>null</code>. A
     * <code>null</code> separator splits on whitespace.
     * </p>
     * <p>
     * <pre>
     * StringUtils.splitByWholeSeparator(null, *)               = null
     * StringUtils.splitByWholeSeparator("", *)                 = []
     * StringUtils.splitByWholeSeparator("ab de fg", null)      = ["ab", "de", "fg"]
     * StringUtils.splitByWholeSeparator("ab   de fg", null)    = ["ab", "de", "fg"]
     * StringUtils.splitByWholeSeparator("ab:cd:ef", ":")       = ["ab", "cd", "ef"]
     * StringUtils.splitByWholeSeparator("ab-!-cd-!-ef", "-!-") = ["ab", "cd", "ef"]
     * </pre>
     *
     * @param str       the String to parse, may be null
     * @param separator String containing the String to be used as a delimiter,
     *                  <code>null</code> splits on whitespace
     * @return an array of parsed Strings, <code>null</code> if null String was
     * input
     */
    public static String[] splitByWholeSeparator(String str, String separator) {
        return splitByWholeSeparatorWorker(str, separator, -1, false);
    }

    /**
     * <p>
     * Splits the provided text into an array, separator string specified.
     * Returns a maximum of <code>max</code> substrings.
     * </p>
     * <p>
     * <p>
     * The separator(s) will not be included in the returned String array.
     * Adjacent separators are treated as one separator.
     * </p>
     * <p>
     * <p>
     * A <code>null</code> input String returns <code>null</code>. A
     * <code>null</code> separator splits on whitespace.
     * </p>
     * <p>
     * <pre>
     * StringUtils.splitByWholeSeparator(null, *, *)               = null
     * StringUtils.splitByWholeSeparator("", *, *)                 = []
     * StringUtils.splitByWholeSeparator("ab de fg", null, 0)      = ["ab", "de", "fg"]
     * StringUtils.splitByWholeSeparator("ab   de fg", null, 0)    = ["ab", "de", "fg"]
     * StringUtils.splitByWholeSeparator("ab:cd:ef", ":", 2)       = ["ab", "cd:ef"]
     * StringUtils.splitByWholeSeparator("ab-!-cd-!-ef", "-!-", 5) = ["ab", "cd", "ef"]
     * StringUtils.splitByWholeSeparator("ab-!-cd-!-ef", "-!-", 2) = ["ab", "cd-!-ef"]
     * </pre>
     *
     * @param str       the String to parse, may be null
     * @param separator String containing the String to be used as a delimiter,
     *                  <code>null</code> splits on whitespace
     * @param max       the maximum number of elements to include in the returned
     *                  array. A zero or negative value implies no limit.
     * @return an array of parsed Strings, <code>null</code> if null String was
     * input
     */
    public static String[] splitByWholeSeparator(String str, String separator, int max) {
        return splitByWholeSeparatorWorker(str, separator, max, false);
    }

    /**
     * Performs the logic for the
     * <code>splitByWholeSeparatorPreserveAllTokens</code> methods.
     *
     * @param str               the String to parse, may be <code>null</code>
     * @param separator         String containing the String to be used as a delimiter,
     *                          <code>null</code> splits on whitespace
     * @param max               the maximum number of elements to include in the returned
     *                          array. A zero or negative value implies no limit.
     * @param preserveAllTokens if <code>true</code>, adjacent separators are treated as empty
     *                          token separators; if <code>false</code>, adjacent separators
     *                          are treated as one separator.
     * @return an array of parsed Strings, <code>null</code> if null String
     * input
     * @since 2.4
     */
    private static String[] splitByWholeSeparatorWorker(String str, String separator, int max,
                                                        boolean preserveAllTokens) {
        if (str == null) {
            return null;
        }

        int len = str.length();

        if (len == 0) {
            return EMPTY_STRING_ARRAY;
        }

        if ((separator == null) || (EMPTY.equals(separator))) {
            // Split on whitespace.
            return splitWorker(str, null, max, preserveAllTokens);
        }

        int separatorLength = separator.length();

        ArrayList<String> substrings = new ArrayList<>();
        int numberOfSubstrings = 0;
        int beg = 0;
        int end = 0;
        while (end < len) {
            end = str.indexOf(separator, beg);

            if (end > -1) {
                if (end > beg) {
                    numberOfSubstrings += 1;

                    if (numberOfSubstrings == max) {
                        end = len;
                        substrings.add(str.substring(beg));
                    } else {
                        // The following is OK, because String.substring( beg,
                        // end ) excludes
                        // the character at the position 'end'.
                        // System.out.println("sub " + beg + "|" + end +"|" +
                        // str.substring(beg, end));
                        substrings.add(str.substring(beg, end));

                        // Set the starting point for the next search.
                        // The following is equivalent to beg = end +
                        // (separatorLength - 1) + 1,
                        // which is the right calculation:
                        beg = end + separatorLength;
                    }
                } else {
                    // We found a consecutive occurrence of the separator, so
                    // skip it.
                    if (preserveAllTokens) {
                        numberOfSubstrings += 1;
                        if (numberOfSubstrings == max) {
                            end = len;
                            substrings.add(str.substring(beg));
                        } else {
                            substrings.add(EMPTY);
                        }
                    }
                    beg = end + separatorLength;
                }
            } else {
                // String.substring( beg ) goes from 'beg' to the end of the
                // String.
                // System.out.println("sub~~ " + beg + "|" + end +"|" +
                // str.substring(beg));
                String t = str.substring(beg);
                if (!t.equals(EMPTY))
                    substrings.add(str.substring(beg));
                end = len;
            }
        }

        return substrings.toArray(EMPTY_STRING_ARRAY);
    }

    public static boolean hasText(String str) {
        return hasText((CharSequence) str);
    }

    public static boolean hasText(CharSequence str) {
        if (!hasLength(str)) {
            return false;
        }
        int strLen = str.length();
        for (int i = 0; i < strLen; i++) {
            if (!Character.isWhitespace(str.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasLength(CharSequence str) {
        return (str != null && str.length() > 0);
    }

    public static boolean hasLength(String str) {
        return hasLength((CharSequence) str);
    }

    /**
     * Replace the pattern using a map, such as a pattern, such as A pattern is
     * "hello ${foo}" and the map is {"foo" : "world"}, when you execute this
     * function, the result is "hello world"
     *
     * @param s   The pattern string.
     * @param map The key-value
     * @return The string replaced.
     */
    public static String replace(String s, Map<String, Object> map) {
        StringBuilder ret = new StringBuilder((int) (s.length() * 1.5));
        int cursor = 0;
        for (int start, end; (start = s.indexOf("${", cursor)) != -1 && (end = s.indexOf("}", start)) != -1; ) {
            ret.append(s, cursor, start).append(map.get(s.substring(start + 2, end)));
            cursor = end + 1;
        }
        ret.append(s, cursor, s.length());
        return ret.toString();
    }

    public static String replace(String s, Object... objs) {
        if (objs == null || objs.length == 0)
            return s;
        if (!s.contains("{}"))
            return s;

        StringBuilder ret = new StringBuilder((int) (s.length() * 1.5));
        int cursor = 0;
        int index = 0;
        for (int start; (start = s.indexOf("{}", cursor)) != -1; ) {
            ret.append(s, cursor, start);
            if (index < objs.length) {
                Object obj = objs[index];
                try {
                    if (obj != null) {
                        if (obj instanceof AbstractCollection) {
                            ret.append(Arrays.toString(((AbstractCollection<?>) obj).toArray()));
                        } else {
                            ret.append(obj);
                        }
                    } else {
                        ret.append("null");
                    }
                } catch (Throwable ignored) {
                }
            } else {
                ret.append("{}");
            }
            cursor = start + 2;
            index++;
        }
        ret.append(s, cursor, s.length());
        return ret.toString();
    }

    public static String escapeXML(String str) {
        if (str == null)
            return "";

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < str.length(); ++i) {
            char c = str.charAt(i);
            switch (c) {
                case '\u00FF':
                case '\u0024':
                    break;
                case '&':
                    sb.append("&amp;");
                    break;
                case '<':
                    sb.append("&lt;");
                    break;
                case '>':
                    sb.append("&gt;");
                    break;
                case '\"':
                    sb.append("&quot;");
                    break;
                case '\'':
                    sb.append("&apos;");
                    break;
                default:
                    if (c >= '\u0000' && c <= '\u001F')
                        break;
                    if (c >= '\uE000' && c <= '\uF8FF')
                        break;
                    if (c >= '\uFFF0' && c <= '\uFFFF')
                        break;
                    sb.append(c);
                    break;
            }
        }
        return sb.toString();
    }

    /**
     * Convert a string that is unicode form to a normal string.
     *
     * @param s The unicode form of a string, e.g. "\\u8001\\u9A6C"
     * @return Normal string
     */
    public static String unicodeToString(String s) {
        StringBuilder sb = new StringBuilder();
        StringTokenizer st = new StringTokenizer(s, "\\u");
        while (st.hasMoreTokens()) {
            String token = st.nextToken();
            if (token.length() > 4) {
                sb.append((char) Integer.parseInt(token.substring(0, 4), 16));
                sb.append(token.substring(4));
            } else {
                sb.append((char) Integer.parseInt(token, 16));
            }
        }
        return sb.toString();
    }


    /**
     * Extract the filename extension from the given Java resource path,
     * e.g. "mypath/myfile.txt" -&gt; "txt".
     *
     * @param path the file path (may be {@code null})
     * @return the extracted filename extension, or {@code null} if none
     */
    public static String getFilenameExtension(String path) {
        if (path == null) {
            return null;
        }
        int extIndex = path.lastIndexOf(EXTENSION_SEPARATOR);
        if (extIndex == -1) {
            return null;
        }
        int folderIndex = path.lastIndexOf(FOLDER_SEPARATOR);
        if (folderIndex > extIndex) {
            return null;
        }
        return path.substring(extIndex + 1);
    }

    /**
     * Extract the filename from the given Java resource path,
     * e.g. {@code "mypath/myfile.txt" -&gt; "myfile.txt"}.
     *
     * @param path the file path (may be {@code null})
     * @return the extracted filename, or {@code null} if none
     */
    public static String getFilename(String path) {
        if (path == null) {
            return null;
        }
        int separatorIndex = path.lastIndexOf(FOLDER_SEPARATOR);
        return (separatorIndex != -1 ? path.substring(separatorIndex + 1) : path);
    }

    public static byte[] getUtf8Bytes(String string) {
        return string.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Convert String to an integer. Parses up to the first non-numeric
     * character. If no number is found an IllegalArgumentException is thrown
     *
     * @param string A String containing an integer.
     * @param from   The index to start parsing from
     * @return an int
     */
    public static int toInt(String string, int from) {
        int val = 0;
        boolean started = false;
        boolean minus = false;

        for (int i = from; i < string.length(); i++) {
            char b = string.charAt(i);
            if (b <= ' ') {
                if (started)
                    break;
            } else if (b >= '0' && b <= '9') {
                val = val * 10 + (b - '0');
                started = true;
            } else if (b == '-' && !started) {
                minus = true;
            } else
                break;
        }

        if (started)
            return minus ? (-val) : val;
        throw new NumberFormatException(string);
    }

    /**
     * Append substring to StringBuilder
     *
     * @param buf    StringBuilder to append to
     * @param s      String to append from
     * @param offset The offset of the substring
     * @param length The length of the substring
     */
    public static void append(StringBuilder buf, String s, int offset, int length) {
        synchronized (buf) {
            int end = offset + length;
            for (int i = offset; i < end; i++) {
                if (i >= s.length())
                    break;
                buf.append(s.charAt(i));
            }
        }
    }

    /**
     * append hex digit
     *
     * @param buf  the buffer to append to
     * @param b    the byte to append
     * @param base the base of the hex output (almost always 16).
     */
    public static void append(StringBuilder buf, byte b, int base) {
        int bi = 0xff & b;
        int c = '0' + (bi / base) % base;
        if (c > '9')
            c = 'a' + (c - '0' - 10);
        buf.append((char) c);
        c = '0' + bi % base;
        if (c > '9')
            c = 'a' + (c - '0' - 10);
        buf.append((char) c);
    }
}
