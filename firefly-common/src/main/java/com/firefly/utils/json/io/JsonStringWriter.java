package com.firefly.utils.json.io;

import com.firefly.utils.function.Action1;
import com.firefly.utils.lang.ArrayUtils;

import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import static com.firefly.utils.json.JsonStringSymbol.*;

public class JsonStringWriter extends AbstractJsonStringWriter {

    public static Set<Character> SPECIAL_CHARACTER = new HashSet<Character>() {{
        for (int i = 0; i <= 0x1f; i++) {
            add((char) i);
        }
        for (int i = 0x7f; i <= 0x9f; i++) {
            add((char) i);
        }
        add((char) 0x00ad);
        for (int i = 0x0600; i <= 0x0604; i++) {
            add((char) i);
        }
        add((char) 0x070f);
        add((char) 0x17b4);
        add((char) 0x17b5);
        for (int i = 0x200c; i <= 0x200f; i++) {
            add((char) i);
        }
        for (int i = 0x2028; i <= 0x202f; i++) {
            add((char) i);
        }
        for (int i = 0x2060; i <= 0x206f; i++) {
            add((char) i);
        }
        add((char) 0xfeff);
        for (int i = 0xff01; i <= 0xff0f; i++) {
            add((char) i);
        }
        for (int i = 0xfff0; i <= 0xffff; i++) {
            add((char) i);
        }
    }};

    /**
     * This method is used to filter some special characters.
     *
     * @param ch be tested character
     * @return A escaped string, if the value equals null, it represents the character dosen't need escape.
     */
    public static String escapeSpecialCharacter(char ch) {
        if (SPECIAL_CHARACTER.contains(ch)) {
            String hexStr = Integer.toHexString(ch);
            StringBuilder padding = new StringBuilder();
            for (int j = hexStr.length(); j < 4; j++) {
                padding.append("0");
            }
            return "\\u" + padding + hexStr;
        }
        return null;
    }

    private Deque<Object> deque = new LinkedList<>();

    @Override
    public void pushRef(Object obj) {
        deque.addFirst(obj);
    }

    @Override
    public boolean existRef(Object obj) {
        return deque.contains(obj);
    }

    @Override
    public void popRef() {
        deque.removeFirst();
    }

    private void write(char ch, boolean needExpand) {
        if (needExpand)
            write(ch);
        else
            buf[count++] = ch;
    }

    private void writeJsonStringValue(String value) {
        boolean needExpand = false;
        buf[count++] = QUOTE;
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            switch (ch) {
                case '\b':
                    write('\\', needExpand);
                    write('b', needExpand);
                    break;
                case '\n':
                    write('\\', needExpand);
                    write('n', needExpand);
                    break;
                case '\r':
                    write('\\', needExpand);
                    write('r', needExpand);
                    break;
                case '\f':
                    write('\\', needExpand);
                    write('f', needExpand);
                    break;
                case '\\':
                    write('\\', needExpand);
                    write('\\', needExpand);
                    break;
                case '/':
                    write('\\', needExpand);
                    write('/', needExpand);
                    break;
                case '"':
                    write('\\', needExpand);
                    write('"', needExpand);
                    break;
                case '\t':
                    write('\\', needExpand);
                    write('t', needExpand);
                    break;

                default:
                    String hexStr = escapeSpecialCharacter(ch);
                    if (hexStr == null) {
                        write(ch, needExpand);
                    } else {
                        needExpand = true;
                        write(hexStr);
                    }
                    break;
            }
        }
        write(QUOTE, needExpand);
    }

    @Override
    public void writeStringWithQuote(String value) {
        int newCount = count + value.length() * 2 + 2;
        if (newCount > buf.length) {
            expandCapacity(newCount);
        }
        writeJsonStringValue(value);
    }

    @Override
    public void writeStringArray(String[] array) {
        int arrayLen = array.length;
        if (arrayLen == 0) {
            buf[count++] = ARRAY_PRE;
            buf[count++] = ARRAY_SUF;
            return;
        }

        int iMax = arrayLen - 1;
        int totalSize = 2;
        for (String str : array) {
            totalSize += str.length() * 2 + 2 + 1;
        }

        int newCount = count + totalSize;
        if (newCount > buf.length) {
            expandCapacity(newCount);
        }

        buf[count++] = ARRAY_PRE;
        for (int i = 0; ; ++i) {
            writeJsonStringValue(array[i]);
            if (i == iMax) {
                buf[count++] = ARRAY_SUF;
                return;
            }
            buf[count++] = SEPARATOR;
        }
    }

    private <T extends Number> void writeNumberArray(T[] array, int elementMaxLen, Action1<T> copyToChars) {
        int arrayLen = array.length;
        if (arrayLen == 0) {
            buf[count++] = ARRAY_PRE;
            buf[count++] = ARRAY_SUF;
            return;
        }

        int iMax = arrayLen - 1;
        int newCount = count + (elementMaxLen + 1) * arrayLen + 2 - 1;
        if (newCount > buf.length) {
            expandCapacity(newCount);
        }

        buf[count++] = ARRAY_PRE;
        for (int i = 0; ; i++) {
            copyToChars.call(array[i]);
            if (i == iMax) {
                buf[count++] = ARRAY_SUF;
                return;
            }
            buf[count++] = SEPARATOR;
        }
    }

    @Override
    public void writeIntArray(int[] array) {
        writeIntArray(ArrayUtils.toObject(array));
    }

    @Override
    public void writeIntArray(Integer[] array) {
        int elementMaxLen = MIN_INT_VALUE.length;
        writeNumberArray(array, elementMaxLen, val -> {
            if (val == Integer.MIN_VALUE) {
                System.arraycopy(MIN_INT_VALUE, 0, buf, count, elementMaxLen);
                count += elementMaxLen;
            } else {
                count += (val < 0) ? IOUtils.stringSize(-val) + 1 : IOUtils.stringSize(val);
                IOUtils.getChars(val, count, buf);
            }
        });
    }

    @Override
    public void writeShortArray(short[] array) {
        writeShortArray(ArrayUtils.toObject(array));
    }

    @Override
    public void writeShortArray(Short[] array) {
        int elementMaxLen = MIN_SHORT_VALUE.length;
        writeNumberArray(array, elementMaxLen, val -> {
            if (val == Short.MIN_VALUE) {
                System.arraycopy(MIN_SHORT_VALUE, 0, buf, count, elementMaxLen);
                count += elementMaxLen;
            } else {
                count += (val < 0) ? IOUtils.stringSize(-val) + 1 : IOUtils.stringSize(val);
                IOUtils.getChars(val, count, buf);
            }
        });
    }

    @Override
    public void writeLongArray(long[] array) {
        writeLongArray(ArrayUtils.toObject(array));
    }

    @Override
    public void writeLongArray(Long[] array) {
        int elementMaxLen = MIN_LONG_VALUE.length;
        writeNumberArray(array, elementMaxLen, val -> {
            if (val == Long.MIN_VALUE) {
                System.arraycopy(MIN_LONG_VALUE, 0, buf, count, elementMaxLen);
                count += elementMaxLen;
            } else {
                count += (val < 0) ? IOUtils.stringSize(-val) + 1 : IOUtils.stringSize(val);
                IOUtils.getChars(val, count, buf);
            }
        });
    }

    @Override
    public void writeBooleanArray(boolean[] array) {
        writeBooleanArray(ArrayUtils.toObject(array));
    }

    @Override
    public void writeBooleanArray(Boolean[] array) {
        int arrayLen = array.length;
        if (arrayLen == 0) {
            buf[count++] = ARRAY_PRE;
            buf[count++] = ARRAY_SUF;
            return;
        }
        int iMax = arrayLen - 1;
        int newCount = count + (5 + 1) * arrayLen + 2 - 1;
        if (newCount > buf.length) {
            expandCapacity(newCount);
        }

        buf[count++] = ARRAY_PRE;
        for (int i = 0; ; i++) {
            if (array[i]) {
                buf[count++] = 't';
                buf[count++] = 'r';
                buf[count++] = 'u';
                buf[count++] = 'e';
            } else {
                buf[count++] = 'f';
                buf[count++] = 'a';
                buf[count++] = 'l';
                buf[count++] = 's';
                buf[count++] = 'e';
            }
            if (i == iMax) {
                buf[count++] = ARRAY_SUF;
                return;
            }
            buf[count++] = SEPARATOR;
        }
    }
}
