package com.firefly.utils.json.support;

import com.firefly.utils.VerifyUtils;
import com.firefly.utils.json.exception.JsonException;

public class JsonStringReader {
	private char[] chars;
	private int pos = 0;
	private int limit;
	private int mark = 0;

	public JsonStringReader(String str) {
		chars = str.toCharArray();
		limit = chars.length;
	}
	
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
	
	public int getMark() {
		return mark;
	}
	
	public void mark() {
		mark = pos;
	}
	
	public void reset() {
		pos = mark;
	}
	
	public char get(int index) {
		return chars[index];
	}
	
	public boolean isString() {
		char c = readAndSkipBlank();
		return c == '"';
	}

	public boolean isArray() {
		char c = readAndSkipBlank();
		return c == '[';
	}

	public boolean isObject() {
		char c = readAndSkipBlank();
		return c == '{';
	}
	
	public boolean isColon() {
		char c = readAndSkipBlank();
		return c == ':';
	}
	
	public boolean isComma() {
		char c = readAndSkipBlank();
		return c == ',';
	}

	public int position() {
		return pos;
	}

	public int limit() {
		return limit;
	}

	public char read() {
		return chars[pos++];
	}

	public char readAndSkipBlank() {
		char c = read();
		if (c > ' ')
			return c;
		for (;;) {
			c = read();
			if (c > ' ')
				return c;
		}
	}
	
	public int readInt() {
		int value = 0;
		char ch = readAndSkipBlank();
		boolean isString = (ch == '"');
		if(isString)
			ch = readAndSkipBlank();
		boolean negative = (ch == '-');
		
		if(!negative) {
			if(VerifyUtils.isDigit(ch))
				value = (value << 3) + (value << 1) + (ch - '0');
			else
				throw new JsonException("read int error, charactor \"" + ch + "\" is not integer");
		}
		
		for(;;) {
			ch = read();
			if(VerifyUtils.isDigit(ch))
				value = (value << 3) + (value << 1) + (ch - '0');
			else {
				if(isString) {
					if(ch == '"')
						break;
				} else {
					if (isEndFlag(ch)) {
						pos--;
						break;
					} else
						throw new JsonException("read int error, charactor \"" + ch + "\" is not integer");
				}
			}
			
			if(pos >= limit)
				break;
		}
		return negative ? -value : value;
	}
	
	public long readLong() {
		long value = 0;
		char ch = readAndSkipBlank();
		boolean isString = (ch == '"');
		if(isString)
			ch = readAndSkipBlank();
		boolean negative = (ch == '-');
		
		if(!negative) {
			if(VerifyUtils.isDigit(ch))
				value = (value << 3) + (value << 1) + (ch - '0');
			else
				throw new JsonException("read int error, charactor \"" + ch + "\" is not integer");
		}
		
		for(;;) {
			ch = read();
			if(VerifyUtils.isDigit(ch))
				value = (value << 3) + (value << 1) + (ch - '0');
			else {
				if(isString) {
					if(ch == '"')
						break;
				} else {
					if (isEndFlag(ch)) {
						pos--;
						break;
					} else
						throw new JsonException("read int error, charactor \"" + ch + "\" is not integer");
				}
			}
			
			if(pos >= limit)
				break;
		}
		return negative ? -value : value;
	}

	public char[] readField(char[] chs) {
		if(!isString())
			throw new JsonException("read field error");
			
		int cur = pos;
		int len = chs.length;
		boolean skip = true;
		
		if(chars[pos + len] == '"') {
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
			char[] field = null;
			mark();
			for(;;) {
				char c = read();
				if(c == '"') {
					break;
				}
			}
			int fieldLen = pos - 1 - mark;
			field = new char[fieldLen];
			System.arraycopy(chars, mark, field, 0, fieldLen);
			return field;
		}
	}

}
