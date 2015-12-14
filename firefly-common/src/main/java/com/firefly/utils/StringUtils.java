package com.firefly.utils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import com.firefly.utils.collection.ArrayTrie;
import com.firefly.utils.collection.Trie;

public class StringUtils {

	private final static Trie<String> CHARSETS = new ArrayTrie<>(256);
	public static final String EMPTY = "";
	public static final String[] EMPTY_STRING_ARRAY = new String[0];

	public static final char[] lowercases = { '\000', '\001', '\002', '\003', '\004', '\005', '\006', '\007', '\010',
			'\011', '\012', '\013', '\014', '\015', '\016', '\017', '\020', '\021', '\022', '\023', '\024', '\025',
			'\026', '\027', '\030', '\031', '\032', '\033', '\034', '\035', '\036', '\037', '\040', '\041', '\042',
			'\043', '\044', '\045', '\046', '\047', '\050', '\051', '\052', '\053', '\054', '\055', '\056', '\057',
			'\060', '\061', '\062', '\063', '\064', '\065', '\066', '\067', '\070', '\071', '\072', '\073', '\074',
			'\075', '\076', '\077', '\100', '\141', '\142', '\143', '\144', '\145', '\146', '\147', '\150', '\151',
			'\152', '\153', '\154', '\155', '\156', '\157', '\160', '\161', '\162', '\163', '\164', '\165', '\166',
			'\167', '\170', '\171', '\172', '\133', '\134', '\135', '\136', '\137', '\140', '\141', '\142', '\143',
			'\144', '\145', '\146', '\147', '\150', '\151', '\152', '\153', '\154', '\155', '\156', '\157', '\160',
			'\161', '\162', '\163', '\164', '\165', '\166', '\167', '\170', '\171', '\172', '\173', '\174', '\175',
			'\176', '\177' };

	public static final String __ISO_8859_1 = "iso-8859-1";
	public final static String __UTF8 = "utf-8";
	public final static String __UTF16 = "utf-16";

	static {
		CHARSETS.put("utf-8", __UTF8);
		CHARSETS.put("utf8", __UTF8);
		CHARSETS.put("utf-16", __UTF16);
		CHARSETS.put("utf16", __UTF16);
		CHARSETS.put("iso-8859-1", __ISO_8859_1);
		CHARSETS.put("iso_8859_1", __ISO_8859_1);
	}

	/**
	 * Convert alternate charset names (eg utf8) to normalized name (eg UTF-8).
	 * 
	 * @param s
	 *            the charset to normalize
	 * @return the normalized charset (or null if normalized version not found)
	 */
	public static String normalizeCharset(String s) {
		String n = CHARSETS.get(s);
		return (n == null) ? s : n;
	}

	/**
	 * Convert alternate charset names (eg utf8) to normalized name (eg UTF-8).
	 * 
	 * @param s
	 *            the charset to normalize
	 * @param offset
	 *            the offset in the charset
	 * @param length
	 *            the length of the charset in the input param
	 * @return the normalized charset (or null if not found)
	 */
	public static String normalizeCharset(String s, int offset, int length) {
		String n = CHARSETS.get(s, offset, length);
		return (n == null) ? s.substring(offset, offset + length) : n;
	}

	// Splitting
	// -----------------------------------------------------------------------
	/**
	 * <p>
	 * Splits the provided text into an array, using whitespace as the
	 * separator. Whitespace is defined by {@link Character#isWhitespace(char)}.
	 * </p>
	 *
	 * <p>
	 * The separator is not included in the returned String array. Adjacent
	 * separators are treated as one separator. For more control over the split
	 * use the StrTokenizer class.
	 * </p>
	 *
	 * <p>
	 * A <code>null</code> input String returns <code>null</code>.
	 * </p>
	 *
	 * <pre>
	 * StringUtils.split(null)       = null
	 * StringUtils.split("")         = []
	 * StringUtils.split("abc def")  = ["abc", "def"]
	 * StringUtils.split("abc  def") = ["abc", "def"]
	 * StringUtils.split(" abc ")    = ["abc"]
	 * </pre>
	 *
	 * @param str
	 *            the String to parse, may be null
	 * @return an array of parsed Strings, <code>null</code> if null String
	 *         input
	 */
	public static String[] split(String str) {
		return split(str, null, -1);
	}

	/**
	 * <p>
	 * Splits the provided text into an array, separators specified. This is an
	 * alternative to using StringTokenizer.
	 * </p>
	 *
	 * <p>
	 * The separator is not included in the returned String array. Adjacent
	 * separators are treated as one separator. For more control over the split
	 * use the StrTokenizer class.
	 * </p>
	 *
	 * <p>
	 * A <code>null</code> input String returns <code>null</code>. A
	 * <code>null</code> separatorChars splits on whitespace.
	 * </p>
	 *
	 * <pre>
	 * StringUtils.split(null, *)         = null
	 * StringUtils.split("", *)           = []
	 * StringUtils.split("abc def", null) = ["abc", "def"]
	 * StringUtils.split("abc def", " ")  = ["abc", "def"]
	 * StringUtils.split("abc  def", " ") = ["abc", "def"]
	 * StringUtils.split("ab:cd:ef", ":") = ["ab", "cd", "ef"]
	 * </pre>
	 *
	 * @param str
	 *            the String to parse, may be null
	 * @param separatorChars
	 *            the characters used as the delimiters, <code>null</code>
	 *            splits on whitespace
	 * @return an array of parsed Strings, <code>null</code> if null String
	 *         input
	 */
	public static String[] split(String str, String separatorChars) {
		return splitWorker(str, separatorChars, -1, false);
	}

	/**
	 * <p>
	 * Splits the provided text into an array, separator specified. This is an
	 * alternative to using StringTokenizer.
	 * </p>
	 *
	 * <p>
	 * The separator is not included in the returned String array. Adjacent
	 * separators are treated as one separator. For more control over the split
	 * use the StrTokenizer class.
	 * </p>
	 *
	 * <p>
	 * A <code>null</code> input String returns <code>null</code>.
	 * </p>
	 *
	 * <pre>
	 * StringUtils.split(null, *)         = null
	 * StringUtils.split("", *)           = []
	 * StringUtils.split("a.b.c", '.')    = ["a", "b", "c"]
	 * StringUtils.split("a..b.c", '.')   = ["a", "b", "c"]
	 * StringUtils.split("a:b:c", '.')    = ["a:b:c"]
	 * StringUtils.split("a b c", ' ')    = ["a", "b", "c"]
	 * </pre>
	 *
	 * @param str
	 *            the String to parse, may be null
	 * @param separatorChar
	 *            the character used as the delimiter
	 * @return an array of parsed Strings, <code>null</code> if null String
	 *         input
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
	 *
	 * <p>
	 * The separator is not included in the returned String array. Adjacent
	 * separators are treated as one separator.
	 * </p>
	 *
	 * <p>
	 * A <code>null</code> input String returns <code>null</code>. A
	 * <code>null</code> separatorChars splits on whitespace.
	 * </p>
	 *
	 * <p>
	 * If more than <code>max</code> delimited substrings are found, the last
	 * returned string includes all characters after the first
	 * <code>max - 1</code> returned strings (including separator characters).
	 * </p>
	 *
	 * <pre>
	 * StringUtils.split(null, *, *)            = null
	 * StringUtils.split("", *, *)              = []
	 * StringUtils.split("ab de fg", null, 0)   = ["ab", "cd", "ef"]
	 * StringUtils.split("ab   de fg", null, 0) = ["ab", "cd", "ef"]
	 * StringUtils.split("ab:cd:ef", ":", 0)    = ["ab", "cd", "ef"]
	 * StringUtils.split("ab:cd:ef", ":", 2)    = ["ab", "cd:ef"]
	 * </pre>
	 *
	 * @param str
	 *            the String to parse, may be null
	 * @param separatorChars
	 *            the characters used as the delimiters, <code>null</code>
	 *            splits on whitespace
	 * @param max
	 *            the maximum number of elements to include in the array. A zero
	 *            or negative value implies no limit
	 * @return an array of parsed Strings, <code>null</code> if null String
	 *         input
	 */
	public static String[] split(String str, String separatorChars, int max) {
		return splitWorker(str, separatorChars, max, false);
	}

	/**
	 * Performs the logic for the <code>split</code> and
	 * <code>splitPreserveAllTokens</code> methods that return a maximum array
	 * length.
	 *
	 * @param str
	 *            the String to parse, may be <code>null</code>
	 * @param separatorChars
	 *            the separate character
	 * @param max
	 *            the maximum number of elements to include in the array. A zero
	 *            or negative value implies no limit.
	 * @param preserveAllTokens
	 *            if <code>true</code>, adjacent separators are treated as empty
	 *            token separators; if <code>false</code>, adjacent separators
	 *            are treated as one separator.
	 * @return an array of parsed Strings, <code>null</code> if null String
	 *         input
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
		List<String> list = new ArrayList<String>();
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
	 * @param str
	 *            the String to parse, may be <code>null</code>
	 * @param separatorChar
	 *            the separate character
	 * @param preserveAllTokens
	 *            if <code>true</code>, adjacent separators are treated as empty
	 *            token separators; if <code>false</code>, adjacent separators
	 *            are treated as one separator.
	 * @return an array of parsed Strings, <code>null</code> if null String
	 *         input
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
		List<String> list = new ArrayList<String>();
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
	 *
	 * <p>
	 * The separator(s) will not be included in the returned String array.
	 * Adjacent separators are treated as one separator.
	 * </p>
	 *
	 * <p>
	 * A <code>null</code> input String returns <code>null</code>. A
	 * <code>null</code> separator splits on whitespace.
	 * </p>
	 *
	 * <pre>
	 * StringUtils.splitByWholeSeparator(null, *)               = null
	 * StringUtils.splitByWholeSeparator("", *)                 = []
	 * StringUtils.splitByWholeSeparator("ab de fg", null)      = ["ab", "de", "fg"]
	 * StringUtils.splitByWholeSeparator("ab   de fg", null)    = ["ab", "de", "fg"]
	 * StringUtils.splitByWholeSeparator("ab:cd:ef", ":")       = ["ab", "cd", "ef"]
	 * StringUtils.splitByWholeSeparator("ab-!-cd-!-ef", "-!-") = ["ab", "cd", "ef"]
	 * </pre>
	 *
	 * @param str
	 *            the String to parse, may be null
	 * @param separator
	 *            String containing the String to be used as a delimiter,
	 *            <code>null</code> splits on whitespace
	 * @return an array of parsed Strings, <code>null</code> if null String was
	 *         input
	 */
	public static String[] splitByWholeSeparator(String str, String separator) {
		return splitByWholeSeparatorWorker(str, separator, -1, false);
	}

	/**
	 * <p>
	 * Splits the provided text into an array, separator string specified.
	 * Returns a maximum of <code>max</code> substrings.
	 * </p>
	 *
	 * <p>
	 * The separator(s) will not be included in the returned String array.
	 * Adjacent separators are treated as one separator.
	 * </p>
	 *
	 * <p>
	 * A <code>null</code> input String returns <code>null</code>. A
	 * <code>null</code> separator splits on whitespace.
	 * </p>
	 *
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
	 * @param str
	 *            the String to parse, may be null
	 * @param separator
	 *            String containing the String to be used as a delimiter,
	 *            <code>null</code> splits on whitespace
	 * @param max
	 *            the maximum number of elements to include in the returned
	 *            array. A zero or negative value implies no limit.
	 * @return an array of parsed Strings, <code>null</code> if null String was
	 *         input
	 */
	public static String[] splitByWholeSeparator(String str, String separator, int max) {
		return splitByWholeSeparatorWorker(str, separator, max, false);
	}

	/**
	 * Performs the logic for the
	 * <code>splitByWholeSeparatorPreserveAllTokens</code> methods.
	 *
	 * @param str
	 *            the String to parse, may be <code>null</code>
	 * @param separator
	 *            String containing the String to be used as a delimiter,
	 *            <code>null</code> splits on whitespace
	 * @param max
	 *            the maximum number of elements to include in the returned
	 *            array. A zero or negative value implies no limit.
	 * @param preserveAllTokens
	 *            if <code>true</code>, adjacent separators are treated as empty
	 *            token separators; if <code>false</code>, adjacent separators
	 *            are treated as one separator.
	 * @return an array of parsed Strings, <code>null</code> if null String
	 *         input
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

		ArrayList<String> substrings = new ArrayList<String>();
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
	 * @param s
	 *            The pattern string.
	 * @param map
	 *            The key-value
	 * @return The string replaced.
	 */
	public static String replace(String s, Map<String, Object> map) {
		StringBuilder ret = new StringBuilder((int) (s.length() * 1.5));
		int cursor = 0;
		for (int start, end; (start = s.indexOf("${", cursor)) != -1 && (end = s.indexOf("}", start)) != -1;) {
			ret.append(s.substring(cursor, start)).append(map.get(s.substring(start + 2, end)));
			cursor = end + 1;
		}
		ret.append(s.substring(cursor, s.length()));
		return ret.toString();
	}

	public static String replace(String s, Object... objs) {
		if (objs == null || objs.length == 0)
			return s;
		if (s.indexOf("{}") == -1)
			return s;

		StringBuilder ret = new StringBuilder((int) (s.length() * 1.5));
		int cursor = 0;
		int index = 0;
		for (int start; (start = s.indexOf("{}", cursor)) != -1;) {
			ret.append(s.substring(cursor, start));
			if (index < objs.length) {
				try {
					ret.append(objs[index]);
				} catch (Throwable t) {
					t.printStackTrace();
					System.out.println(objs[index].getClass());
				}
			} else
				ret.append("{}");
			cursor = start + 2;
			index++;
		}
		ret.append(s.substring(cursor, s.length()));
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
	 * @param s
	 *            The unicode form of a string, e.g. "\\u8001\\u9A6C"
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
	 * fast lower case conversion. Only works on ascii (not unicode)
	 * 
	 * @param s
	 *            the string to convert
	 * @return a lower case version of s
	 */
	public static String asciiToLowerCase(String s) {
		char[] c = null;
		int i = s.length();

		// look for first conversion
		while (i-- > 0) {
			char c1 = s.charAt(i);
			if (c1 <= 127) {
				char c2 = lowercases[c1];
				if (c1 != c2) {
					c = s.toCharArray();
					c[i] = c2;
					break;
				}
			}
		}

		while (i-- > 0) {
			if (c[i] <= 127)
				c[i] = lowercases[c[i]];
		}
		return c == null ? s : new String(c);
	}

	/**
	 * Append 2 digits (zero padded) to the StringBuffer
	 * 
	 * @param buf
	 *            the buffer to append to
	 * @param i
	 *            the value to append
	 */
	public static void append2digits(StringBuffer buf, int i) {
		if (i < 100) {
			buf.append((char) (i / 10 + '0'));
			buf.append((char) (i % 10 + '0'));
		}
	}

	/**
	 * Append 2 digits (zero padded) to the StringBuilder
	 * 
	 * @param buf
	 *            the buffer to append to
	 * @param i
	 *            the value to append
	 */
	public static void append2digits(StringBuilder buf, int i) {
		if (i < 100) {
			buf.append((char) (i / 10 + '0'));
			buf.append((char) (i % 10 + '0'));
		}
	}

	/**
	 * Append substring to StringBuilder
	 * 
	 * @param buf
	 *            StringBuilder to append to
	 * @param s
	 *            String to append from
	 * @param offset
	 *            The offset of the substring
	 * @param length
	 *            The length of the substring
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
	 * @param buf
	 *            the buffer to append to
	 * @param b
	 *            the byte to append
	 * @param base
	 *            the base of the hex output (almost always 16).
	 * 
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

	/**
	 * Convert String to an integer. Parses up to the first non-numeric
	 * character. If no number is found an IllegalArgumentException is thrown
	 * 
	 * @param string
	 *            A String containing an integer.
	 * @param from
	 *            The index to start parsing from
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
	 * Convert String to an long. Parses up to the first non-numeric character.
	 * If no number is found an IllegalArgumentException is thrown
	 * 
	 * @param string
	 *            A String containing an integer.
	 * @return an int
	 */
	public static long toLong(String string) {
		long val = 0;
		boolean started = false;
		boolean minus = false;

		for (int i = 0; i < string.length(); i++) {
			char b = string.charAt(i);
			if (b <= ' ') {
				if (started)
					break;
			} else if (b >= '0' && b <= '9') {
				val = val * 10L + (b - '0');
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

	public static byte[] getBytes(String s) {
		return s.getBytes(StandardCharsets.ISO_8859_1);
	}

	public static byte[] getUtf8Bytes(String s) {
		return s.getBytes(StandardCharsets.UTF_8);
	}

	public static byte[] getBytes(String s, String charset) {
		try {
			return s.getBytes(charset);
		} catch (Exception e) {
			e.printStackTrace();
			return s.getBytes();
		}
	}

	/**
	 * Parse a CSV string using {@link #csvSplit(List,String, int, int)}
	 * 
	 * @param s
	 *            The string to parse
	 * @return An array of parsed values.
	 */
	public static String[] csvSplit(String s) {
		if (s == null)
			return null;
		return csvSplit(s, 0, s.length());
	}

	/**
	 * Parse a CSV string using {@link #csvSplit(List,String, int, int)}
	 * 
	 * @param s
	 *            The string to parse
	 * @param off
	 *            The offset into the string to start parsing
	 * @param len
	 *            The len in characters to parse
	 * @return An array of parsed values.
	 */
	public static String[] csvSplit(String s, int off, int len) {
		if (s == null)
			return null;
		if (off < 0 || len < 0 || off > s.length())
			throw new IllegalArgumentException();

		List<String> list = new ArrayList<>();
		csvSplit(list, s, off, len);
		return list.toArray(new String[list.size()]);
	}

	enum CsvSplitState {
		PRE_DATA, QUOTE, SLOSH, DATA, WHITE, POST_DATA
	};

	/**
	 * Split a quoted comma separated string to a list
	 * <p>
	 * Handle <a href="https://www.ietf.org/rfc/rfc4180.txt">rfc4180</a>-like
	 * CSV strings, with the exceptions:
	 * <ul>
	 * <li>quoted values may contain double quotes escaped with back-slash
	 * <li>Non-quoted values are trimmed of leading trailing white space
	 * <li>trailing commas are ignored
	 * <li>double commas result in a empty string value
	 * </ul>
	 * 
	 * @param list
	 *            The Collection to split to (or null to get a new list)
	 * @param s
	 *            The string to parse
	 * @param off
	 *            The offset into the string to start parsing
	 * @param len
	 *            The len in characters to parse
	 * @return list containing the parsed list values
	 */
	public static List<String> csvSplit(List<String> list, String s, int off, int len) {
		if (list == null)
			list = new ArrayList<>();
		CsvSplitState state = CsvSplitState.PRE_DATA;
		StringBuilder out = new StringBuilder();
		int last = -1;
		while (len > 0) {
			char ch = s.charAt(off++);
			len--;

			switch (state) {
			case PRE_DATA:
				if (Character.isWhitespace(ch))
					continue;

				if ('"' == ch) {
					state = CsvSplitState.QUOTE;
					continue;
				}

				if (',' == ch) {
					list.add("");
					continue;
				}

				state = CsvSplitState.DATA;
				out.append(ch);
				continue;

			case DATA:
				if (Character.isWhitespace(ch)) {
					last = out.length();
					out.append(ch);
					state = CsvSplitState.WHITE;
					continue;
				}

				if (',' == ch) {
					list.add(out.toString());
					out.setLength(0);
					state = CsvSplitState.PRE_DATA;
					continue;
				}

				out.append(ch);
				continue;

			case WHITE:
				if (Character.isWhitespace(ch)) {
					out.append(ch);
					continue;
				}

				if (',' == ch) {
					out.setLength(last);
					list.add(out.toString());
					out.setLength(0);
					state = CsvSplitState.PRE_DATA;
					continue;
				}

				state = CsvSplitState.DATA;
				out.append(ch);
				last = -1;
				continue;

			case QUOTE:
				if ('\\' == ch) {
					state = CsvSplitState.SLOSH;
					continue;
				}
				if ('"' == ch) {
					list.add(out.toString());
					out.setLength(0);
					state = CsvSplitState.POST_DATA;
					continue;
				}
				out.append(ch);
				continue;

			case SLOSH:
				out.append(ch);
				state = CsvSplitState.QUOTE;
				continue;

			case POST_DATA:
				if (',' == ch) {
					state = CsvSplitState.PRE_DATA;
					continue;
				}
				continue;
			}
		}

		switch (state) {
		case PRE_DATA:
		case POST_DATA:
			break;

		case DATA:
		case QUOTE:
		case SLOSH:
			list.add(out.toString());
			break;

		case WHITE:
			out.setLength(last);
			list.add(out.toString());
			break;
		}

		return list;
	}
}
