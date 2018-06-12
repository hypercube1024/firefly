package com.firefly.utils.json.io;

import com.firefly.utils.json.JsonWriter;

import java.lang.ref.SoftReference;

abstract public class AbstractJsonStringWriter extends JsonWriter {

    protected char[] buf;
    protected int count;
    protected final static ThreadLocal<SoftReference<char[]>> bufLocal = new ThreadLocal<>();
    public static final char[] NULL = "null".toCharArray();
    public static final char[] MIN_SHORT_VALUE = "-32768".toCharArray();
    public static final char[] MIN_INT_VALUE = "-2147483648".toCharArray();
    public static final char[] MIN_LONG_VALUE = "-9223372036854775808".toCharArray();
    public static final char[] TRUE_VALUE = "true".toCharArray();
    public static final char[] FALSE_VALUE = "false".toCharArray();

    public AbstractJsonStringWriter() {
        SoftReference<char[]> ref = bufLocal.get();

        if (ref != null) {
            buf = ref.get();
            bufLocal.set(null);
        }

        if (buf == null)
            buf = new char[1024 * 4];
    }

    public AbstractJsonStringWriter(int initialSize) {
        if (initialSize < 0) {
            throw new IllegalArgumentException("Negative initial size: " + initialSize);
        }
        buf = new char[initialSize];
    }

    @Override
    public void write(int c) {
        int newCount = count + 1;
        if (newCount > buf.length) {
            expandCapacity(newCount);
        }
        buf[count] = (char) c;
        count = newCount;
    }

    @Override
    public void write(char[] c) {
        write(c, 0, c.length);
    }

    @Override
    public void write(char[] c, int off, int len) {
        if (off < 0 || off > c.length || len < 0 || off + len > c.length
                || off + len < 0) {
            throw new IndexOutOfBoundsException();
        } else if (len == 0) {
            return;
        }

        int newCount = count + len;
        if (newCount > buf.length) {
            expandCapacity(newCount);
        }
        System.arraycopy(c, off, buf, count, len);
        count = newCount;
    }

    @Override
    public void write(String str) {
        write(str, 0, str.length());
    }

    @Override
    public void write(String str, int off, int len) {
        int newCount = count + len;
        if (newCount > buf.length) {
            expandCapacity(newCount);
        }
        str.getChars(off, off + len, buf, count);
        count = newCount;
    }

    @Override
    public AbstractJsonStringWriter append(CharSequence csq) {
        String str = csq.toString();
        write(str, 0, str.length());
        return this;
    }

    @Override
    public AbstractJsonStringWriter append(CharSequence csq, int start, int end) {
        String str = csq.subSequence(start, end).toString();
        write(str, 0, str.length());
        return this;
    }

    @Override
    public AbstractJsonStringWriter append(char c) {
        write(c);
        return this;
    }

    @Override
    public String toString() {
        return new String(buf, 0, count);
    }

    @Override
    public void flush() {
    }

    @Override
    public void close() {
        reset();
        bufLocal.set(new SoftReference<>(buf));
    }

    @Override
    public void writeNull() {
        write(NULL);
    }

    @Override
    public void writeBoolean(boolean b) {
        if (b)
            write(TRUE_VALUE);
        else
            write(FALSE_VALUE);
    }

    @Override
    public void writeInt(int i) {
        if (i == Integer.MIN_VALUE) {
            write(MIN_INT_VALUE);
            return;
        }
        int size = (i < 0) ? IOUtils.stringSize(-i) + 1 : IOUtils.stringSize(i);
        int newCount = count + size;

        if (newCount > buf.length) {
            expandCapacity(newCount);
        }

        IOUtils.getChars(i, newCount, buf);
        count = newCount;
    }

    @Override
    public void writeShort(short i) {
        writeInt((int) i);
    }

    @Override
    public void writeByte(byte i) {
        writeInt((int) i);
    }

    @Override
    public void writeLong(long i) {
        if (i == Long.MIN_VALUE) {
            write(MIN_LONG_VALUE);
            return;
        }

        int size = (i < 0) ? IOUtils.stringSize(-i) + 1 : IOUtils.stringSize(i);

        int newCount = count + size;
        if (newCount > buf.length) {
            expandCapacity(newCount);
        }

        IOUtils.getChars(i, newCount, buf);
        count = newCount;
    }

    public void reset() {
        count = 0;
    }

    public int size() {
        return count;
    }

    protected void expandCapacity(int minimumCapacity) {
        int newCapacity = (buf.length * 3) / 2 + 1;

        if (newCapacity < minimumCapacity) {
            newCapacity = minimumCapacity;
        }
        char newValue[] = new char[newCapacity];
        System.arraycopy(buf, 0, newValue, 0, count);
        buf = newValue;
    }

}
