package com.fireflysource.common.io;

import com.fireflysource.common.object.TypeUtils;

import java.io.*;
import java.nio.Buffer;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Buffer utility methods.
 * <p>The standard JVM {@link ByteBuffer} can exist in two modes: In fill mode the valid
 * data is between 0 and pos; In flush mode the valid data is between the pos and the limit.
 * The various ByteBuffer methods assume a mode and some of them will switch or enforce a mode:
 * Allocate and clear set fill mode; flip and compact switch modes; read and write assume fill
 * and flush modes.    This duality can result in confusing code such as:
 * </p>
 * <pre>
 *     buffer.clear();
 *     channel.write(buffer);
 * </pre>
 * <p>
 * Which looks as if it should write no data, but in fact writes the buffer worth of garbage.
 * </p>
 * <p>
 * The BufferUtils class provides a set of utilities that operate on the convention that ByteBuffers
 * will always be left, passed in an API or returned from a method in the flush mode - ie with
 * valid data between the pos and limit.    This convention is adopted so as to avoid confusion as to
 * what state a buffer is in and to avoid excessive copying of data that can result with the usage
 * of compress.</p>
 * <p>
 * Thus this class provides alternate implementations of {@link #allocate(int)},
 * {@link #allocateDirect(int)} and {@link #clear(ByteBuffer)} that leave the buffer
 * in flush mode.   Thus the following tests will pass:
 * </p>
 * <pre>
 *     ByteBuffer buffer = BufferUtils.allocate(1024);
 *     assert(buffer.remaining()==0);
 *     BufferUtils.clear(buffer);
 *     assert(buffer.remaining()==0);
 * </pre>
 * <p>If the BufferUtils methods {@link #fill(ByteBuffer, byte[], int, int)},
 * {@link #append(ByteBuffer, byte[], int, int)} or {@link #put(ByteBuffer, ByteBuffer)} are used,
 * then the caller does not need to explicitly switch the buffer to fill mode.
 * If the caller wishes to use other ByteBuffer bases libraries to fill a buffer,
 * then they can use explicit calls of #flipToFill(ByteBuffer) and #flipToFlush(ByteBuffer, int)
 * to change modes.  Note because this convention attempts to avoid the copies of compact, the position
 * is not set to zero on each fill cycle and so its value must be remembered:
 * </p>
 * <pre>
 *      int pos = BufferUtils.flipToFill(buffer);
 *      try
 *      {
 *          buffer.put(data);
 *      }
 *      finally
 *      {
 *          flipToFlush(buffer, pos);
 *      }
 * </pre>
 * <p>
 * The flipToFill method will effectively clear the buffer if it is empty and will compact the buffer if there is no space.
 * </p>
 */
public class BufferUtils {
    public static final ByteBuffer EMPTY_BUFFER = ByteBuffer.allocate(0);
    static final int TEMP_BUFFER_SIZE = 4096;
    static final byte SPACE = 0x20;
    static final byte MINUS = '-';
    static final byte[] DIGIT = {(byte) '0', (byte) '1', (byte) '2', (byte) '3', (byte) '4', (byte) '5', (byte) '6', (byte) '7', (byte) '8', (byte) '9', (byte) 'A', (byte) 'B', (byte) 'C', (byte) 'D', (byte) 'E', (byte) 'F'};
    private final static int[] decDivisors = {1000000000, 100000000, 10000000, 1000000, 100000, 10000, 1000, 100, 10, 1};
    private final static int[] hexDivisors = {0x10000000, 0x1000000, 0x100000, 0x10000, 0x1000, 0x100, 0x10, 0x1};
    private final static long[] decDivisorsL = {1000000000000000000L, 100000000000000000L, 10000000000000000L, 1000000000000000L, 100000000000000L, 10000000000000L, 1000000000000L, 100000000000L, 10000000000L, 1000000000L, 100000000L, 10000000L, 1000000L, 100000L, 10000L, 1000L, 100L, 10L, 1L};

    /**
     * Allocate ByteBuffer in flush mode.
     * The position and limit will both be zero, indicating that the buffer is
     * empty and must be flipped before any data is put to it.
     *
     * @param capacity capacity of the allocated ByteBuffer
     * @return Buffer
     */
    public static ByteBuffer allocate(int capacity) {
        ByteBuffer buf = ByteBuffer.allocate(capacity);
        buf.limit(0);
        return buf;
    }

    /**
     * Allocate ByteBuffer in flush mode.
     * The position and limit will both be zero, indicating that the buffer is
     * empty and in flush mode.
     *
     * @param capacity capacity of the allocated ByteBuffer
     * @return Buffer
     */
    public static ByteBuffer allocateDirect(int capacity) {
        ByteBuffer buf = ByteBuffer.allocateDirect(capacity);
        buf.limit(0);
        return buf;
    }

    /**
     * Clear the buffer to be empty in flush mode.
     * The position and limit are set to 0;
     *
     * @param buffer The buffer to clear.
     */
    public static void clear(ByteBuffer buffer) {
        if (buffer != null) {
            buffer.position(0);
            buffer.limit(0);
        }
    }

    /**
     * Clear the buffer to be empty in fill mode.
     * The position is set to 0 and the limit is set to the capacity.
     *
     * @param buffer The buffer to clear.
     */
    public static void clearToFill(ByteBuffer buffer) {
        if (buffer != null) {
            buffer.position(0);
            buffer.limit(buffer.capacity());
        }
    }

    /**
     * Flip the buffer to fill mode.
     * The position is set to the first unused position in the buffer
     * (the old limit) and the limit is set to the capacity.
     * If the buffer is empty, then this call is effectively {@link #clearToFill(ByteBuffer)}.
     * If there is no unused space to fill, a {@link ByteBuffer#compact()} is done to attempt
     * to create space.
     * <p>
     * This method is used as a replacement to {@link ByteBuffer#compact()}.
     *
     * @param buffer The buffer to flip
     * @return The position of the valid data before the flipped position. This value should be
     * passed to a subsequent call to {@link #flipToFlush(ByteBuffer, int)}
     */
    public static int flipToFill(ByteBuffer buffer) {
        int position = buffer.position();
        int limit = buffer.limit();
        if (position == limit) {
            buffer.position(0);
            buffer.limit(buffer.capacity());
            return 0;
        }

        int capacity = buffer.capacity();
        if (limit == capacity) {
            buffer.compact();
            return 0;
        }

        buffer.position(limit);
        buffer.limit(capacity);
        return position;
    }

    /**
     * Flip the buffer to Flush mode.
     * The limit is set to the first unused byte(the old position) and
     * the position is set to the passed position.
     * <p>
     * This method is used as a replacement of {@link Buffer#flip()}.
     *
     * @param buffer   the buffer to be flipped
     * @param position The position of valid data to flip to. This should
     *                 be the return value of the previous call to {@link #flipToFill(ByteBuffer)}
     */
    public static void flipToFlush(ByteBuffer buffer, int position) {
        buffer.limit(buffer.position());
        buffer.position(position);
    }

    /**
     * Convert a ByteBuffer to a byte array.
     *
     * @param buffer The buffer to convert in flush mode. The buffer is not altered.
     * @return An array of bytes duplicated from the buffer.
     */
    public static byte[] toArray(ByteBuffer buffer) {
        if (buffer.hasArray()) {
            byte[] array = buffer.array();
            int from = buffer.arrayOffset() + buffer.position();
            return Arrays.copyOfRange(array, from, from + buffer.remaining());
        } else {
            byte[] to = new byte[buffer.remaining()];
            buffer.slice().get(to);
            return to;
        }
    }

    /**
     * Convert a buffer collection to a byte array.
     *
     * @param buffers The buffer to convert in flush mode. The buffer is not altered.
     * @return An array of bytes duplicated from the buffer.
     */
    public static byte[] toArray(Collection<ByteBuffer> buffers) {
        List<byte[]> list = buffers.stream().map(BufferUtils::toArray).collect(Collectors.toList());
        int count = list.stream().mapToInt(arr -> arr.length).sum();
        if (count < 0) {
            throw new IllegalArgumentException("The buffers are too big");
        }
        byte[] result = new byte[count];
        int index = 0;
        for (byte[] bytes : list) {
            System.arraycopy(bytes, 0, result, index, bytes.length);
            index += bytes.length;
        }
        return result;
    }

    /**
     * @param buf the buffer to check
     * @return true if buf is equal to EMPTY_BUFFER
     */
    public static boolean isTheEmptyBuffer(ByteBuffer buf) {
        @SuppressWarnings("ReferenceEquality")
        boolean isTheEmptyBuffer_ = (buf == EMPTY_BUFFER);
        return isTheEmptyBuffer_;
    }

    /**
     * Check for an empty or null buffer.
     *
     * @param buf the buffer to check
     * @return true if the buffer is null or empty.
     */
    public static boolean isEmpty(ByteBuffer buf) {
        return buf == null || buf.remaining() == 0;
    }

    /**
     * Check for a non null and non empty buffer.
     *
     * @param buf the buffer to check
     * @return true if the buffer is not null and not empty.
     */
    public static boolean hasContent(ByteBuffer buf) {
        return buf != null && buf.remaining() > 0;
    }

    /**
     * Check for a non null and full buffer.
     *
     * @param buf the buffer to check
     * @return true if the buffer is not null and the limit equals the capacity.
     */
    public static boolean isFull(ByteBuffer buf) {
        return buf != null && buf.limit() == buf.capacity();
    }

    /**
     * Get remaining from null checked buffer
     *
     * @param buffer The buffer to get the remaining from, in flush mode.
     * @return 0 if the buffer is null, else the bytes remaining in the buffer.
     */
    public static int length(ByteBuffer buffer) {
        return buffer == null ? 0 : buffer.remaining();
    }

    /**
     * Get the space from the limit to the capacity
     *
     * @param buffer the buffer to get the space from
     * @return space
     */
    public static int space(ByteBuffer buffer) {
        if (buffer == null)
            return 0;
        return buffer.capacity() - buffer.limit();
    }

    /**
     * Compact the buffer
     *
     * @param buffer the buffer to compact
     * @return true if the compact made a full buffer have space
     */
    public static boolean compact(ByteBuffer buffer) {
        if (buffer.position() == 0)
            return false;
        boolean full = buffer.limit() == buffer.capacity();
        buffer.compact().flip();
        return full && buffer.limit() < buffer.capacity();
    }

    /**
     * Put data from one buffer into another, avoiding over/under flows
     *
     * @param from Buffer to take bytes from in flush mode
     * @param to   Buffer to put bytes to in fill mode.
     * @return number of bytes moved
     */
    public static int put(ByteBuffer from, ByteBuffer to) {
        int put;
        int remaining = from.remaining();
        if (remaining > 0) {
            if (remaining <= to.remaining()) {
                to.put(from);
                put = remaining;
                from.position(from.limit());
            } else if (from.hasArray()) {
                put = to.remaining();
                to.put(from.array(), from.arrayOffset() + from.position(), put);
                from.position(from.position() + put);
            } else {
                put = to.remaining();
                ByteBuffer slice = from.slice();
                slice.limit(put);
                to.put(slice);
                from.position(from.position() + put);
            }
        } else {
            put = 0;
        }
        return put;
    }

    /**
     * Put data from one buffer into another, avoiding over/under flows
     *
     * @param from Buffer to take bytes from in flush mode
     * @param to   Buffer to put bytes to in flush mode. The buffer is flipToFill before the put and flipToFlush after.
     * @return number of bytes moved
     * @deprecated use {@link #append(ByteBuffer, ByteBuffer)}
     */
    public static int flipPutFlip(ByteBuffer from, ByteBuffer to) {
        return append(to, from);
    }

    /**
     * Append bytes to a buffer.
     *
     * @param to  Buffer is flush mode
     * @param b   bytes to append
     * @param off offset into byte
     * @param len length to append
     * @throws BufferOverflowException if unable to append buffer due to space limits
     */
    public static void append(ByteBuffer to, byte[] b, int off, int len) throws BufferOverflowException {
        int pos = flipToFill(to);
        try {
            to.put(b, off, len);
        } finally {
            flipToFlush(to, pos);
        }
    }

    /**
     * Appends a byte to a buffer
     *
     * @param to Buffer is flush mode
     * @param b  byte to append
     */
    public static void append(ByteBuffer to, byte b) {
        int pos = flipToFill(to);
        try {
            to.put(b);
        } finally {
            flipToFlush(to, pos);
        }
    }

    /**
     * Appends a buffer to a buffer
     *
     * @param to Buffer is flush mode
     * @param b  buffer to append
     * @return The position of the valid data before the flipped position.
     */
    public static int append(ByteBuffer to, ByteBuffer b) {
        int pos = flipToFill(to);
        try {
            return put(b, to);
        } finally {
            flipToFlush(to, pos);
        }
    }

    /**
     * Like append, but does not throw {@link BufferOverflowException}
     *
     * @param to  Buffer The buffer to fill to. The buffer will be flipped to fill mode and then flipped back to flush mode.
     * @param b   bytes The bytes to fill
     * @param off offset into bytes
     * @param len length to fill
     * @return the number of bytes taken from the buffer.
     */
    public static int fill(ByteBuffer to, byte[] b, int off, int len) {
        int pos = flipToFill(to);
        try {
            int remaining = to.remaining();
            int take = remaining < len ? remaining : len;
            to.put(b, off, take);
            return take;
        } finally {
            flipToFlush(to, pos);
        }
    }

    public static void readFrom(File file, ByteBuffer buffer) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            FileChannel channel = raf.getChannel();
            long needed = raf.length();

            while (needed > 0 && buffer.hasRemaining())
                needed = needed - channel.read(buffer);
        }
    }

    public static void readFrom(InputStream is, int needed, ByteBuffer buffer) throws IOException {
        ByteBuffer tmp = allocate(8192);

        while (needed > 0 && buffer.hasRemaining()) {
            int l = is.read(tmp.array(), 0, 8192);
            if (l < 0)
                break;
            tmp.position(0);
            tmp.limit(l);
            buffer.put(tmp);
        }
    }

    public static void writeTo(ByteBuffer buffer, OutputStream out) throws IOException {
        if (buffer.hasArray()) {
            out.write(buffer.array(), buffer.arrayOffset() + buffer.position(), buffer.remaining());
            // update buffer position, in way similar to non-array version of writeTo
            buffer.position(buffer.position() + buffer.remaining());
        } else {
            byte[] bytes = new byte[TEMP_BUFFER_SIZE];
            while (buffer.hasRemaining()) {
                int byteCountToWrite = Math.min(buffer.remaining(), TEMP_BUFFER_SIZE);
                buffer.get(bytes, 0, byteCountToWrite);
                out.write(bytes, 0, byteCountToWrite);
            }
        }
    }

    /**
     * Convert the buffer to an ISO-8859-1 String
     *
     * @param buffer The buffer to convert in flush mode. The buffer is unchanged
     * @return The buffer as a string.
     */
    public static String toString(ByteBuffer buffer) {
        return toString(buffer, StandardCharsets.ISO_8859_1);
    }

    /**
     * Convert the buffer to an UTF-8 String
     *
     * @param buffer The buffer to convert in flush mode. The buffer is unchanged
     * @return The buffer as a string.
     */
    public static String toUTF8String(ByteBuffer buffer) {
        return toString(buffer, StandardCharsets.UTF_8);
    }

    /**
     * Convert the buffer to an ISO-8859-1 String
     *
     * @param buffer  The buffer to convert in flush mode. The buffer is unchanged
     * @param charset The {@link Charset} to use to convert the bytes
     * @return The buffer as a string.
     */
    public static String toString(ByteBuffer buffer, Charset charset) {
        if (buffer == null)
            return null;
        byte[] array = buffer.hasArray() ? buffer.array() : null;
        if (array == null) {
            byte[] to = new byte[buffer.remaining()];
            buffer.slice().get(to);
            return new String(to, 0, to.length, charset);
        }
        return new String(array, buffer.arrayOffset() + buffer.position(), buffer.remaining(), charset);
    }

    /**
     * Convert a partial buffer to a String.
     *
     * @param buffer   the buffer to convert
     * @param position The position in the buffer to start the string from
     * @param length   The length of the buffer
     * @param charset  The {@link Charset} to use to convert the bytes
     * @return The buffer as a string.
     */
    public static String toString(ByteBuffer buffer, int position, int length, Charset charset) {
        if (buffer == null)
            return null;
        byte[] array = buffer.hasArray() ? buffer.array() : null;
        if (array == null) {
            ByteBuffer ro = buffer.asReadOnlyBuffer();
            ro.position(position);
            ro.limit(position + length);
            byte[] to = new byte[length];
            ro.get(to);
            return new String(to, 0, to.length, charset);
        }
        return new String(array, buffer.arrayOffset() + position, length, charset);
    }

    /**
     * Convert buffer to an integer. Parses up to the first non-numeric character. If no number is found an IllegalArgumentException is thrown
     *
     * @param buffer A buffer containing an integer in flush mode. The position is not changed.
     * @return an int
     */
    public static int toInt(ByteBuffer buffer) {
        return toInt(buffer, buffer.position(), buffer.remaining());
    }

    /**
     * Convert buffer to an integer. Parses up to the first non-numeric character. If no number is found an
     * IllegalArgumentException is thrown
     *
     * @param buffer   A buffer containing an integer in flush mode. The position is not changed.
     * @param position the position in the buffer to start reading from
     * @param length   the length of the buffer to use for conversion
     * @return an int of the buffer bytes
     */
    public static int toInt(ByteBuffer buffer, int position, int length) {
        int val = 0;
        boolean started = false;
        boolean minus = false;

        int limit = position + length;

        if (length <= 0)
            throw new NumberFormatException(toString(buffer, position, length, StandardCharsets.UTF_8));

        for (int i = position; i < limit; i++) {
            byte b = buffer.get(i);
            if (b <= SPACE) {
                if (started)
                    break;
            } else if (b >= '0' && b <= '9') {
                val = val * 10 + (b - '0');
                started = true;
            } else if (b == MINUS && !started) {
                minus = true;
            } else
                break;
        }

        if (started)
            return minus ? (-val) : val;
        throw new NumberFormatException(toString(buffer));
    }

    /**
     * Convert buffer to an integer. Parses up to the first non-numeric character. If no number is found an IllegalArgumentException is thrown
     *
     * @param buffer A buffer containing an integer in flush mode. The position is updated.
     * @return an int
     */
    public static int takeInt(ByteBuffer buffer) {
        int val = 0;
        boolean started = false;
        boolean minus = false;
        int i;
        for (i = buffer.position(); i < buffer.limit(); i++) {
            byte b = buffer.get(i);
            if (b <= SPACE) {
                if (started)
                    break;
            } else if (b >= '0' && b <= '9') {
                val = val * 10 + (b - '0');
                started = true;
            } else if (b == MINUS && !started) {
                minus = true;
            } else
                break;
        }

        if (started) {
            buffer.position(i);
            return minus ? (-val) : val;
        }
        throw new NumberFormatException(toString(buffer));
    }

    /**
     * Convert buffer to an long. Parses up to the first non-numeric character. If no number is found an IllegalArgumentException is thrown
     *
     * @param buffer A buffer containing an integer in flush mode. The position is not changed.
     * @return an int
     */
    public static long toLong(ByteBuffer buffer) {
        long val = 0;
        boolean started = false;
        boolean minus = false;

        for (int i = buffer.position(); i < buffer.limit(); i++) {
            byte b = buffer.get(i);
            if (b <= SPACE) {
                if (started)
                    break;
            } else if (b >= '0' && b <= '9') {
                val = val * 10L + (b - '0');
                started = true;
            } else if (b == MINUS && !started) {
                minus = true;
            } else
                break;
        }

        if (started)
            return minus ? (-val) : val;
        throw new NumberFormatException(toString(buffer));
    }

    public static void putHexInt(ByteBuffer buffer, int n) {
        if (n < 0) {
            buffer.put((byte) '-');

            if (n == Integer.MIN_VALUE) {
                buffer.put((byte) (0x7f & '8'));
                buffer.put((byte) (0x7f & '0'));
                buffer.put((byte) (0x7f & '0'));
                buffer.put((byte) (0x7f & '0'));
                buffer.put((byte) (0x7f & '0'));
                buffer.put((byte) (0x7f & '0'));
                buffer.put((byte) (0x7f & '0'));
                buffer.put((byte) (0x7f & '0'));

                return;
            }
            n = -n;
        }

        if (n < 0x10) {
            buffer.put(DIGIT[n]);
        } else {
            boolean started = false;
            // This assumes constant time int arithmatic
            for (int hexDivisor : hexDivisors) {
                if (n < hexDivisor) {
                    if (started)
                        buffer.put((byte) '0');
                    continue;
                }

                started = true;
                int d = n / hexDivisor;
                buffer.put(DIGIT[d]);
                n = n - d * hexDivisor;
            }
        }
    }

    public static void putDecInt(ByteBuffer buffer, int n) {
        if (n < 0) {
            buffer.put((byte) '-');

            if (n == Integer.MIN_VALUE) {
                buffer.put((byte) '2');
                n = 147483648;
            } else
                n = -n;
        }

        if (n < 10) {
            buffer.put(DIGIT[n]);
        } else {
            boolean started = false;
            // This assumes constant time int arithmatic
            for (int decDivisor : decDivisors) {
                if (n < decDivisor) {
                    if (started)
                        buffer.put((byte) '0');
                    continue;
                }

                started = true;
                int d = n / decDivisor;
                buffer.put(DIGIT[d]);
                n = n - d * decDivisor;
            }
        }
    }

    public static void putDecLong(ByteBuffer buffer, long n) {
        if (n < 0) {
            buffer.put((byte) '-');

            if (n == Long.MIN_VALUE) {
                buffer.put((byte) '9');
                n = 223372036854775808L;
            } else
                n = -n;
        }

        if (n < 10) {
            buffer.put(DIGIT[(int) n]);
        } else {
            boolean started = false;
            // This assumes constant time int arithmatic
            for (long aDecDivisorsL : decDivisorsL) {
                if (n < aDecDivisorsL) {
                    if (started)
                        buffer.put((byte) '0');
                    continue;
                }

                started = true;
                long d = n / aDecDivisorsL;
                buffer.put(DIGIT[(int) d]);
                n = n - d * aDecDivisorsL;
            }
        }
    }

    public static ByteBuffer toBuffer(int value) {
        ByteBuffer buf = ByteBuffer.allocate(32);
        putDecInt(buf, value);
        return buf;
    }

    public static ByteBuffer toBuffer(long value) {
        ByteBuffer buf = ByteBuffer.allocate(32);
        putDecLong(buf, value);
        return buf;
    }

    public static ByteBuffer toBuffer(String s) {
        return toBuffer(s, StandardCharsets.ISO_8859_1);
    }

    public static ByteBuffer toBuffer(String s, Charset charset) {
        if (s == null)
            return EMPTY_BUFFER;
        return toBuffer(s.getBytes(charset));
    }

    /**
     * Create a new ByteBuffer using provided byte array.
     *
     * @param array the byte array to back buffer with.
     * @return ByteBuffer with provided byte array, in flush mode
     */
    public static ByteBuffer toBuffer(byte[] array) {
        if (array == null)
            return EMPTY_BUFFER;
        return toBuffer(array, 0, array.length);
    }

    /**
     * Create a new ByteBuffer using the provided byte array.
     *
     * @param array  the byte array to use.
     * @param offset the offset within the byte array to use from
     * @param length the length in bytes of the array to use
     * @return ByteBuffer with provided byte array, in flush mode
     */
    public static ByteBuffer toBuffer(byte[] array, int offset, int length) {
        if (array == null)
            return EMPTY_BUFFER;
        return ByteBuffer.wrap(array, offset, length);
    }

    public static ByteBuffer toDirectBuffer(String s) {
        return toDirectBuffer(s, StandardCharsets.ISO_8859_1);
    }

    public static ByteBuffer toDirectBuffer(String s, Charset charset) {
        if (s == null)
            return EMPTY_BUFFER;
        byte[] bytes = s.getBytes(charset);
        ByteBuffer buf = ByteBuffer.allocateDirect(bytes.length);
        buf.put(bytes);
        buf.flip();
        return buf;
    }

    public static ByteBuffer toMappedBuffer(File file) throws IOException {
        try (FileChannel channel = FileChannel.open(file.toPath(), StandardOpenOption.READ)) {
            return channel.map(FileChannel.MapMode.READ_ONLY, 0, file.length());
        }
    }

    /**
     * @param buffer the buffer to test
     * @return {@code false}
     * @deprecated don't use - there is no way to reliably tell if a ByteBuffer is mapped.
     */
    @Deprecated
    public static boolean isMappedBuffer(ByteBuffer buffer) {
        return false;
    }

    public static String toSummaryString(ByteBuffer buffer) {
        if (buffer == null)
            return "null";
        StringBuilder buf = new StringBuilder();
        buf.append("[p=");
        buf.append(buffer.position());
        buf.append(",l=");
        buf.append(buffer.limit());
        buf.append(",c=");
        buf.append(buffer.capacity());
        buf.append(",r=");
        buf.append(buffer.remaining());
        buf.append("]");
        return buf.toString();
    }

    public static String toDetailString(ByteBuffer[] buffer) {
        StringBuilder builder = new StringBuilder();
        builder.append('[');
        for (int i = 0; i < buffer.length; i++) {
            if (i > 0) builder.append(',');
            builder.append(toDetailString(buffer[i]));
        }
        builder.append(']');
        return builder.toString();
    }

    /**
     * Convert Buffer to string ID independent of content
     */
    private static void idString(ByteBuffer buffer, StringBuilder out) {
        out.append(buffer.getClass().getSimpleName());
        out.append("@");
        if (buffer.hasArray() && buffer.arrayOffset() == 4) {
            out.append('T');
            byte[] array = buffer.array();
            TypeUtils.toHex(array[0], out);
            TypeUtils.toHex(array[1], out);
            TypeUtils.toHex(array[2], out);
            TypeUtils.toHex(array[3], out);
        } else
            out.append(Integer.toHexString(System.identityHashCode(buffer)));
    }

    /**
     * Convert Buffer to string ID independent of content
     *
     * @param buffer the buffet to generate a string ID from
     * @return A string showing the buffer ID
     */
    public static String toIDString(ByteBuffer buffer) {
        StringBuilder buf = new StringBuilder();
        idString(buffer, buf);
        return buf.toString();
    }

    /**
     * Convert Buffer to a detail debug string of pointers and content
     *
     * @param buffer the buffer to generate a detail string from
     * @return A string showing the pointers and content of the buffer
     */
    public static String toDetailString(ByteBuffer buffer) {
        if (buffer == null)
            return "null";

        StringBuilder buf = new StringBuilder();
        idString(buffer, buf);
        buf.append("[p=");
        buf.append(buffer.position());
        buf.append(",l=");
        buf.append(buffer.limit());
        buf.append(",c=");
        buf.append(buffer.capacity());
        buf.append(",r=");
        buf.append(buffer.remaining());
        buf.append("]={");

        appendDebugString(buf, buffer);

        buf.append("}");

        return buf.toString();
    }

    private static void appendDebugString(StringBuilder buf, ByteBuffer buffer) {
        try {
            for (int i = 0; i < buffer.position(); i++) {
                appendContentChar(buf, buffer.get(i));
                if (i == 16 && buffer.position() > 32) {
                    buf.append("...");
                    i = buffer.position() - 16;
                }
            }
            buf.append("<<<");
            for (int i = buffer.position(); i < buffer.limit(); i++) {
                appendContentChar(buf, buffer.get(i));
                if (i == buffer.position() + 16 && buffer.limit() > buffer.position() + 32) {
                    buf.append("...");
                    i = buffer.limit() - 16;
                }
            }
            buf.append(">>>");
            int limit = buffer.limit();
            buffer.limit(buffer.capacity());
            for (int i = limit; i < buffer.capacity(); i++) {
                appendContentChar(buf, buffer.get(i));
                if (i == limit + 16 && buffer.capacity() > limit + 32) {
                    buf.append("...");
                    i = buffer.capacity() - 16;
                }
            }
            buffer.limit(limit);
        } catch (Throwable x) {
            buf.append("!!concurrent mod!!");
        }
    }

    private static void appendContentChar(StringBuilder buf, byte b) {
        if (b == '\\')
            buf.append("\\\\");
        else if ((b >= 0x20) && (b <= 0x7E)) // limit to 7-bit printable US-ASCII character space
            buf.append((char) b);
        else if (b == '\r')
            buf.append("\\r");
        else if (b == '\n')
            buf.append("\\n");
        else if (b == '\t')
            buf.append("\\t");
        else
            buf.append("\\x").append(TypeUtils.toHexString(b));
    }

    /**
     * Convert buffer to a Hex Summary String.
     *
     * @param buffer the buffer to generate a hex byte summary from
     * @return A string showing a summary of the content in hex
     */
    public static String toHexSummary(ByteBuffer buffer) {
        if (buffer == null)
            return "null";
        StringBuilder buf = new StringBuilder();

        buf.append("b[").append(buffer.remaining()).append("]=");
        for (int i = buffer.position(); i < buffer.limit(); i++) {
            TypeUtils.toHex(buffer.get(i), buf);
            if (i == buffer.position() + 24 && buffer.limit() > buffer.position() + 32) {
                buf.append("...");
                i = buffer.limit() - 8;
            }
        }
        return buf.toString();
    }

    /**
     * Convert buffer to a Hex String.
     *
     * @param buffer the buffer to generate a hex byte summary from
     * @return A hex string
     */
    public static String toHexString(ByteBuffer buffer) {
        if (buffer == null)
            return "null";
        return TypeUtils.toHexString(toArray(buffer));
    }

    public static void putCRLF(ByteBuffer buffer) {
        buffer.put((byte) 13);
        buffer.put((byte) 10);
    }

    public static boolean isPrefix(ByteBuffer prefix, ByteBuffer buffer) {
        if (prefix.remaining() > buffer.remaining())
            return false;
        int bi = buffer.position();
        for (int i = prefix.position(); i < prefix.limit(); i++) {
            if (prefix.get(i) != buffer.get(bi++)) {
                return false;
            }
        }
        return true;
    }

    public static ByteBuffer ensureCapacity(ByteBuffer buffer, int capacity) {
        if (buffer == null) {
            return allocate(capacity);
        }
        if (buffer.capacity() >= capacity) {
            return buffer;
        }
        if (buffer.hasArray()) {
            return ByteBuffer.wrap(
                    Arrays.copyOfRange(buffer.array(), buffer.arrayOffset(), buffer.arrayOffset() + capacity),
                    buffer.position(), buffer.remaining());
        } else {
            ByteBuffer newBuffer = allocateDirect(capacity);
            append(newBuffer, buffer);
            flipToFill(buffer);
            return newBuffer;
        }
    }

    public static ByteBuffer addCapacity(ByteBuffer buffer, int capacity) {
        ByteBuffer newBuffer = BufferUtils.ensureCapacity(buffer, buffer.position() + capacity);
        BufferUtils.flipToFill(newBuffer);
        newBuffer.position(buffer.position());
        return newBuffer;
    }
}
