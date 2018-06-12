package com.firefly.utils.json.io;

import com.firefly.utils.VerifyUtils;
import com.firefly.utils.json.JsonReader;
import com.firefly.utils.json.exception.JsonException;

import java.math.BigDecimal;
import java.math.BigInteger;

public class JsonStringReader extends JsonReader {
    private char[] chars;
    private int pos = 0;
    private final int limit;
    private int mark = 0;

    public JsonStringReader(String str) {
        chars = str.toCharArray();
        limit = chars.length;
    }

    @Override
    public void increasePosition() {
        pos++;
    }

    @Override
    public void decreasePosition() {
        pos--;
    }

    @Override
    public int position() {
        return pos;
    }

    @Override
    public boolean isEnd() {
//		System.out.println("end pos and limit --> " + pos + "|" + limit);
        return pos >= limit;
    }

    @Override
    public void mark(int readAheadLimit) {
        mark = pos;
    }

    @Override
    public void reset() {
        pos = mark;
    }

    @Override
    public boolean isEndFlag(char ch) {
        switch (ch) {
            case ',':
            case '}':
            case ']':
            case ' ':
            case ':':
                return true;
        }
        return false;
    }

    @Override
    public boolean isString() {
        char c = readAndSkipBlank();
        return c == '"';
    }

    @Override
    public boolean isArray() {
        char c = readAndSkipBlank();
        return c == '[';
    }

    @Override
    public boolean isEmptyArray() {
        mark(1024);
        char c = readAndSkipBlank();

        if (c == ']')
            return true;

        reset();
        return false;
    }

    @Override
    public boolean isObject() {
        char c = readAndSkipBlank();
        return c == '{';
    }

    @Override
    public boolean isEmptyObject() {
        mark(1024);
        char c = readAndSkipBlank();

        if (c == '}')
            return true;

        reset();
        return false;
    }

    @Override
    public boolean isColon() {
        char c = readAndSkipBlank();
        return c == ':';
    }

    @Override
    public boolean isComma() {
        char c = readAndSkipBlank();
        return c == ',';
    }

    @Override
    public boolean isNull() {
        mark(1024);
        char ch = readAndSkipBlank();
        if (pos + 3 > limit) {
            reset();
            return false;
        }

        if (ch == 'n' && 'u' == read() && 'l' == read() && 'l' == read()) {
            if (pos >= limit)
                return true;

            ch = readAndSkipBlank();
            if (isEndFlag(ch)) {
                pos--;
                return true;
            } else {
                reset();
                return false;
            }
        } else {
            reset();
            return false;
        }
    }

    @Override
    public int read() {
        return chars[pos++];
    }

    @Override
    public char readAndSkipBlank() {
        char c = (char) read();
        if (c > ' ')
            return c;
        for (; ; ) {
            c = (char) read();
            if (c > ' ')
                return c;
        }
    }

    @Override
    public boolean readBoolean() {
        boolean ret = false;

        if (isNull()) {
            return false;
        }

        char ch = readAndSkipBlank();
        boolean isString = (ch == '"');
        if (isString) {
            ch = readAndSkipBlank();
        }
        if (ch == 't' && 'r' == read() && 'u' == read() && 'e' == read()) {
            ret = true;
        } else if (ch == 'f' && 'a' == read() && 'l' == read() && 's' == read() && 'e' == read()) {
            ret = false;
        }
        if (isString) {
            ch = readAndSkipBlank();
            if (ch != '"')
                throw new JsonException("read boolean error, the position is " + pos);
        }

        return ret;
    }

    @Override
    public int readInt() {
        return (int) readLong();
    }

    @Override
    public long readLong() {
        long value = 0;
        if (isNull()) {
            return value;
        }
        char ch = readAndSkipBlank();
        boolean isString = (ch == '"');
        if (isString) {
            ch = readAndSkipBlank();
        }
        boolean negative = (ch == '-');

        if (!negative) {
            if (VerifyUtils.isDigit(ch))
                value = (value << 3) + (value << 1) + (ch - '0');
            else
                throw new JsonException("read int error, character \"" + ch + "\" is not integer, the position is " + pos);
        }

        for (; ; ) {
            ch = (char) read();
            if (VerifyUtils.isDigit(ch))
                value = (value << 3) + (value << 1) + (ch - '0');
            else {
                if (isString) {
                    if (ch == '"')
                        break;
                } else {
                    if (isEndFlag(ch)) {
                        pos--;
                        break;
                    } else
                        throw new JsonException("read int error, character \"" + ch + "\" is not integer, the position is " + pos);
                }
            }

            if (pos >= limit) {
                break;
            }
        }
        return negative ? -value : value;
    }

    @Override
    public String readValueAsString() {
        int start = pos;
        int startBlankLength = 0;
        int endBlankLength = 0;
        boolean hasChar = false;
        for (; ; ) {
            char ch = (char) read();
            if (ch <= ' ') {
                if (!hasChar) {
                    startBlankLength++;
                } else {
                    endBlankLength++;
                }
                continue;
            }

            if (!hasChar) {
                hasChar = true;
            }

            if (isEndFlag(ch)) {
                pos--;
                break;
            }
        }
        start = start + startBlankLength;
        int end = pos - endBlankLength;
        int len = end - start;
        return new String(chars, start, len);
    }

    @Override
    public BigInteger readBigInteger() {
        String value = "0";
        if (isNull()) {
            return new BigInteger(value);
        }
        char ch = readAndSkipBlank();
        boolean isString = (ch == '"');
        if (isString) {
            ch = readAndSkipBlank();
        }
        pos--;

        int start = pos;
        for (; ; ) {
            ch = (char) read();
            if (isString) {
                if (ch == '"')
                    break;
            } else {
                if (isEndFlag(ch)) {
                    pos--;
                    break;
                }
            }
        }

        int len = isString ? pos - start - 1 : pos - start;
        String temp = new String(chars, start, len);
        return new BigInteger(temp);
    }

    @Override
    public BigDecimal readBigDecimal() {
        String value = "0.0";
        if (isNull()) {
            return new BigDecimal(value);
        }
        char ch = readAndSkipBlank();
        boolean isString = (ch == '"');
        if (isString) {
            ch = readAndSkipBlank();
        }
        pos--;
        int start = pos;
        for (; ; ) {
            ch = (char) read();
            if (isString) {
                if (ch == '"')
                    break;
            } else {
                if (isEndFlag(ch)) {
                    pos--;
                    break;
                }
            }
        }

        int len = isString ? pos - start - 1 : pos - start;
        String temp = new String(chars, start, len);
        return new BigDecimal(temp);
    }

    @Override
    public double readDouble() {
        double value = 0.0;
        if (isNull()) {
            return value;
        }
        char ch = readAndSkipBlank();
        boolean isString = (ch == '"');
        if (isString) {
            ch = readAndSkipBlank();
        }
        pos--;
        int start = pos;
        for (; ; ) {
            ch = (char) read();
            if (isString) {
                if (ch == '"')
                    break;
            } else {
                if (isEndFlag(ch)) {
                    pos--;
                    break;
                }
            }
        }

        int len = isString ? pos - start - 1 : pos - start;
        String temp = new String(chars, start, len);
        return Double.parseDouble(temp);
    }

    @Override
    public float readFloat() {
        float value = 0.0F;
        if (isNull()) {
            return value;
        }
        char ch = readAndSkipBlank();
        boolean isString = (ch == '"');
        if (isString) {
            ch = readAndSkipBlank();
        }
        pos--;
        int start = pos;
        for (; ; ) {
            ch = (char) read();
            if (isString) {
                if (ch == '"')
                    break;
            } else {
                if (isEndFlag(ch)) {
                    pos--;
                    break;
                }
            }
        }

        int len = isString ? pos - start - 1 : pos - start;
        String temp = new String(chars, start, len);
        return Float.parseFloat(temp);
    }

    @Override
    public char[] readField(char[] chs) {
        if (!isString()) {
            throw new JsonException("read field error, the position is " + pos);
        }
        int cur = pos;
        int len = chs.length;
        boolean skip = true;

        int next = pos + len;
        if (next < limit && chars[next] == '"') {
            for (int i = 0; i < len; i++) {
                if (chs[i] != chars[cur++]) {
                    skip = false;
                    break;
                }
            }
        } else {
            skip = false;
        }

        if (skip) {
            pos = cur + 1;
            return null;
        } else {
            char[] field;
            int start = pos;
            for (; ; ) {
                char c = (char) read();
                if (c == '"')
                    break;
            }
            int fieldLen = pos - 1 - start;
            field = new char[fieldLen];
            System.arraycopy(chars, start, field, 0, fieldLen);
            return field;
        }
    }

    @Override
    public char[] readChars() {
        if (!isString()) {
            throw new JsonException("read field error, the position is " + pos);
        }
        int start = pos;
        for (; ; ) {
            char c = (char) read();
            if (c == '"')
                break;
        }
        int fieldLen = pos - 1 - start;
        char[] c = new char[fieldLen];
        System.arraycopy(chars, start, c, 0, fieldLen);
        return c;
    }

    @Override
    public void skipValue() {
        char ch = readAndSkipBlank();
        switch (ch) {
            case '"': // skip string
                for (; ; ) {
                    ch = (char) read();
                    if (ch == '"')
                        break;
                    else if (ch == '\\')
                        pos++;
                }
                break;
            case '[': // skip array
                for (; ; ) {
                    if (isEmptyArray())
                        break;

                    skipValue();
                    ch = readAndSkipBlank();
                    if (ch == ']')
                        break;

                    if (ch != ',')
                        throw new JsonException("json string array format error, the position is " + pos);
                }
                break;
            case '{': // skip object
                for (; ; ) {
                    if (isEmptyObject())
                        break;

                    readChars();
                    if (!isColon())
                        throw new JsonException("json string object format error, the position is " + pos);

                    skipValue();
                    ch = readAndSkipBlank();
                    if (ch == '}')
                        break;

                    if (ch != ',')
                        throw new JsonException("json string object format error, the position is " + pos);
                }
                break;

            default: // skip number or null
                for (; ; ) {
                    ch = (char) read();
                    if (isEndFlag(ch)) {
                        pos--;
                        break;
                    }
                }
                break;
        }
    }

    @Override
    public String readString() {
        if (isNull()) {
            return null;
        }
        if (!isString()) {
            throw new JsonException("read string error, the position is " + pos);
        }

        try (JsonStringWriter writer = new JsonStringWriter()) {
            int cur = pos;
            int len;
            for (; ; ) {
                char ch = chars[cur++];
                if (ch == '"') {
                    len = cur - pos - 1;
                    writer.write(chars, pos, len);
                    pos = cur;
                    break;
                } else if (ch == '\\') {
                    char c0 = chars[cur++];
                    len = cur - 2 - pos;
                    writer.write(chars, pos, len);
                    switch (c0) {
                        case 'b':
                            writer.write('\b');
                            break;
                        case 'n':
                            writer.write('\n');
                            break;
                        case 'r':
                            writer.write('\r');
                            break;
                        case 'f':
                            writer.write('\f');
                            break;
                        case '\\':
                            writer.write('\\');
                            break;
                        case '/':
                            writer.write('/');
                            break;
                        case '"':
                            writer.write('"');
                            break;
                        case 't':
                            writer.write('\t');
                            break;
                        case 'u': // unicode char parse
                            char[] controlChars = new char[4];
                            for (int i = 0; i < controlChars.length; i++) {
                                controlChars[i] = chars[cur++];
                            }
                            char tmp = (char) Integer.parseInt(String.valueOf(controlChars), 16);
                            writer.write(tmp);
                            break;
                    }
                    pos = cur;
                }

            }
            return writer.toString();
        }
    }

    @Override
    public int read(char[] cbuf, int off, int len) {
        throw new JsonException("method not implements!");
    }

    @Override
    public void close() {

    }

}
