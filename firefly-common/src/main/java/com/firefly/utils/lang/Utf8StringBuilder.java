package com.firefly.utils.lang;

/**
 * UTF-8 StringBuilder.
 *
 * This class wraps a standard {@link java.lang.StringBuilder} and provides
 * methods to append UTF-8 encoded bytes, that are converted into characters.
 *
 * This class is stateful and up to 4 calls to {@link #append(byte)} may be
 * needed before state a character is appended to the string buffer.
 *
 * The UTF-8 decoding is done by this class and no additional buffers or Readers
 * are used. The UTF-8 code was inspired by
 * http://bjoern.hoehrmann.de/utf-8/decoder/dfa/
 *
 */
public class Utf8StringBuilder extends Utf8Appendable {
	final StringBuilder buffer;

	public Utf8StringBuilder() {
		super(new StringBuilder());
		buffer = (StringBuilder) appendable;
	}

	public Utf8StringBuilder(int capacity) {
		super(new StringBuilder(capacity));
		buffer = (StringBuilder) appendable;
	}

	@Override
	public int length() {
		return buffer.length();
	}

	@Override
	public void reset() {
		super.reset();
		buffer.setLength(0);
	}

	public StringBuilder getStringBuilder() {
		checkState();
		return buffer;
	}

	@Override
	public String toString() {
		checkState();
		return buffer.toString();
	}

}
