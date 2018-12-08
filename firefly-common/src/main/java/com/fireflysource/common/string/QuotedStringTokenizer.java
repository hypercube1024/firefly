package com.fireflysource.common.string;

import com.fireflysource.common.object.TypeUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

/**
 * StringTokenizer with Quoting support.
 * <p>
 * This class is a copy of the java.util.StringTokenizer API and the behaviour
 * is the same, except that single and double quoted string values are
 * recognised. Delimiters within quotes are not considered delimiters. Quotes
 * can be escaped with '\'.
 *
 * @see StringTokenizer
 */
public class QuotedStringTokenizer extends StringTokenizer {
    private final static String DEFAULT_DELIMITER = "\t\n\r";

    private String string;
    private String delimiter = DEFAULT_DELIMITER;
    private boolean returnQuotes;
    private boolean returnDelimiters;
    private StringBuilder token;
    private boolean hasToken = false;
    private int index = 0;
    private int lastStart = 0;
    private boolean isDouble = true;
    private boolean isSingle = true;

    public QuotedStringTokenizer(String str, String delimiter, boolean returnDelimiters, boolean returnQuotes) {
        super("");
        string = str;
        if (delimiter != null)
            this.delimiter = delimiter;
        this.returnDelimiters = returnDelimiters;
        this.returnQuotes = returnQuotes;

        if (this.delimiter.indexOf('\'') >= 0 || this.delimiter.indexOf('"') >= 0)
            throw new Error("Can't use quotes as delimiters: " + this.delimiter);

        token = new StringBuilder(string.length() > 1024 ? 512 : string.length() / 2);
    }

    public QuotedStringTokenizer(String str, String delimiter, boolean returnDelimiters) {
        this(str, delimiter, returnDelimiters, false);
    }

    public QuotedStringTokenizer(String str, String delimiter) {
        this(str, delimiter, false, false);
    }

    public QuotedStringTokenizer(String str) {
        this(str, null, false, false);
    }

    @Override
    public boolean hasMoreTokens() {
        // Already found a token
        if (hasToken)
            return true;

        lastStart = index;

        int state = 0;
        boolean escape = false;
        while (index < string.length()) {
            char c = string.charAt(index++);

            switch (state) {
                case 0: // Start
                    if (delimiter.indexOf(c) >= 0) {
                        if (returnDelimiters) {
                            token.append(c);
                            return hasToken = true;
                        }
                    } else if (c == '\'' && isSingle) {
                        if (returnQuotes)
                            token.append(c);
                        state = 2;
                    } else if (c == '\"' && isDouble) {
                        if (returnQuotes)
                            token.append(c);
                        state = 3;
                    } else {
                        token.append(c);
                        hasToken = true;
                        state = 1;
                    }
                    break;

                case 1: // Token
                    hasToken = true;
                    if (delimiter.indexOf(c) >= 0) {
                        if (returnDelimiters)
                            index--;
                        return hasToken;
                    } else if (c == '\'' && isSingle) {
                        if (returnQuotes)
                            token.append(c);
                        state = 2;
                    } else if (c == '\"' && isDouble) {
                        if (returnQuotes)
                            token.append(c);
                        state = 3;
                    } else {
                        token.append(c);
                    }
                    break;

                case 2: // Single Quote
                    hasToken = true;
                    if (escape) {
                        escape = false;
                        token.append(c);
                    } else if (c == '\'') {
                        if (returnQuotes)
                            token.append(c);
                        state = 1;
                    } else if (c == '\\') {
                        if (returnQuotes)
                            token.append(c);
                        escape = true;
                    } else {
                        token.append(c);
                    }
                    break;

                case 3: // Double Quote
                    hasToken = true;
                    if (escape) {
                        escape = false;
                        token.append(c);
                    } else if (c == '\"') {
                        if (returnQuotes)
                            token.append(c);
                        state = 1;
                    } else if (c == '\\') {
                        if (returnQuotes)
                            token.append(c);
                        escape = true;
                    } else {
                        token.append(c);
                    }
                    break;
            }
        }

        return hasToken;
    }

    @Override
    public String nextToken() throws NoSuchElementException {
        if (!hasMoreTokens() || token == null)
            throw new NoSuchElementException();
        String t = token.toString();
        token.setLength(0);
        hasToken = false;
        return t;
    }

    @Override
    public String nextToken(String delim) throws NoSuchElementException {
        delimiter = delim;
        index = lastStart;
        token.setLength(0);
        hasToken = false;
        return nextToken();
    }

    @Override
    public boolean hasMoreElements() {
        return hasMoreTokens();
    }

    @Override
    public Object nextElement() throws NoSuchElementException {
        return nextToken();
    }

    /**
     * Not implemented.
     */
    @Override
    public int countTokens() {
        return -1;
    }

    /**
     * Quote a string. The string is quoted only if quoting is required due to
     * embedded delimiters, quote characters or the empty string.
     *
     * @param s     The string to quote.
     * @param delim the delimiter to use to quote the string
     * @return quoted string
     */
    public static String quoteIfNeeded(String s, String delim) {
        if (s == null)
            return null;
        if (s.length() == 0)
            return "\"\"";

        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' || c == '"' || c == '\'' || Character.isWhitespace(c) || delim.indexOf(c) >= 0) {
                StringBuffer b = new StringBuffer(s.length() + 8);
                quote(b, s);
                return b.toString();
            }
        }

        return s;
    }

    /**
     * Quote a string. The string is quoted only if quoting is required due to
     * embeded delimiters, quote characters or the empty string.
     *
     * @param s The string to quote.
     * @return quoted string
     */
    public static String quote(String s) {
        if (s == null)
            return null;
        if (s.length() == 0)
            return "\"\"";

        StringBuffer b = new StringBuffer(s.length() + 8);
        quote(b, s);
        return b.toString();

    }

    private static final char[] escapes = new char[32];

    static {
        Arrays.fill(escapes, (char) 0xFFFF);
        escapes['\b'] = 'b';
        escapes['\t'] = 't';
        escapes['\n'] = 'n';
        escapes['\f'] = 'f';
        escapes['\r'] = 'r';
    }

    /**
     * Quote a string into an Appendable. Only quotes and backslash are escaped.
     *
     * @param buffer The Appendable
     * @param input  The String to quote.
     */
    public static void quoteOnly(Appendable buffer, String input) {
        if (input == null)
            return;

        try {
            buffer.append('"');
            for (int i = 0; i < input.length(); ++i) {
                char c = input.charAt(i);
                if (c == '"' || c == '\\')
                    buffer.append('\\');
                buffer.append(c);
            }
            buffer.append('"');
        } catch (IOException x) {
            throw new RuntimeException(x);
        }
    }

    /**
     * Quote a string into an Appendable. The characters ", \, \n, \r, \t, \f
     * and \b are escaped
     *
     * @param buffer The Appendable
     * @param input  The String to quote.
     */
    public static void quote(Appendable buffer, String input) {
        if (input == null)
            return;

        try {
            buffer.append('"');
            for (int i = 0; i < input.length(); ++i) {
                char c = input.charAt(i);
                if (c >= 32) {
                    if (c == '"' || c == '\\')
                        buffer.append('\\');
                    buffer.append(c);
                } else {
                    char escape = escapes[c];
                    if (escape == 0xFFFF) {
                        // Unicode escape
                        buffer.append('\\').append('u').append('0').append('0');
                        if (c < 0x10)
                            buffer.append('0');
                        buffer.append(Integer.toString(c, 16));
                    } else {
                        buffer.append('\\').append(escape);
                    }
                }
            }
            buffer.append('"');
        } catch (IOException x) {
            throw new RuntimeException(x);
        }
    }

    public static String unquoteOnly(String s) {
        return unquoteOnly(s, false);
    }

    /**
     * Unquote a string, NOT converting unicode sequences
     *
     * @param s       The string to unquote.
     * @param lenient if true, will leave in backslashes that aren't valid escapes
     * @return quoted string
     */
    public static String unquoteOnly(String s, boolean lenient) {
        if (s == null)
            return null;
        if (s.length() < 2)
            return s;

        char first = s.charAt(0);
        char last = s.charAt(s.length() - 1);
        if (first != last || (first != '"' && first != '\''))
            return s;

        StringBuilder b = new StringBuilder(s.length() - 2);
        boolean escape = false;
        for (int i = 1; i < s.length() - 1; i++) {
            char c = s.charAt(i);

            if (escape) {
                escape = false;
                if (lenient && !isValidEscaping(c)) {
                    b.append('\\');
                }
                b.append(c);
            } else if (c == '\\') {
                escape = true;
            } else {
                b.append(c);
            }
        }

        return b.toString();
    }

    public static String unquote(String s) {
        return unquote(s, false);
    }

    /**
     * Unquote a string.
     *
     * @param s       The string to unquote.
     * @param lenient true if unquoting should be lenient to escaped content,
     *                leaving some alone, false if string unescaping
     * @return quoted string
     */
    public static String unquote(String s, boolean lenient) {
        if (s == null)
            return null;
        if (s.length() < 2)
            return s;

        char first = s.charAt(0);
        char last = s.charAt(s.length() - 1);
        if (first != last || (first != '"' && first != '\''))
            return s;

        StringBuilder b = new StringBuilder(s.length() - 2);
        boolean escape = false;
        for (int i = 1; i < s.length() - 1; i++) {
            char c = s.charAt(i);

            if (escape) {
                escape = false;
                switch (c) {
                    case 'n':
                        b.append('\n');
                        break;
                    case 'r':
                        b.append('\r');
                        break;
                    case 't':
                        b.append('\t');
                        break;
                    case 'f':
                        b.append('\f');
                        break;
                    case 'b':
                        b.append('\b');
                        break;
                    case '\\':
                        b.append('\\');
                        break;
                    case '/':
                        b.append('/');
                        break;
                    case '"':
                        b.append('"');
                        break;
                    case 'u':
                        b.append((char) ((TypeUtils.convertHexDigit((byte) s.charAt(i++)) << 24)
                                + (TypeUtils.convertHexDigit((byte) s.charAt(i++)) << 16)
                                + (TypeUtils.convertHexDigit((byte) s.charAt(i++)) << 8)
                                + (TypeUtils.convertHexDigit((byte) s.charAt(i++)))));
                        break;
                    default:
                        if (lenient && !isValidEscaping(c)) {
                            b.append('\\');
                        }
                        b.append(c);
                }
            } else if (c == '\\') {
                escape = true;
            } else {
                b.append(c);
            }
        }

        return b.toString();
    }

    /**
     * Check that char c (which is preceded by a backslash) is a valid escape
     * sequence.
     *
     * @param c
     * @return
     */
    private static boolean isValidEscaping(char c) {
        return ((c == 'n') || (c == 'r') || (c == 't') || (c == 'f') || (c == 'b') || (c == '\\') || (c == '/')
                || (c == '"') || (c == 'u'));
    }

    public static boolean isQuoted(String s) {
        return s != null && s.length() > 0 && s.charAt(0) == '"' && s.charAt(s.length() - 1) == '"';
    }

    /**
     * @return handle double quotes if true
     */
    public boolean getDouble() {
        return isDouble;
    }

    /**
     * @param d handle double quotes if true
     */
    public void setDouble(boolean d) {
        isDouble = d;
    }

    /**
     * @return handle single quotes if true
     */
    public boolean getSingle() {
        return isSingle;
    }

    /**
     * @param single handle single quotes if true
     */
    public void setSingle(boolean single) {
        isSingle = single;
    }
}
