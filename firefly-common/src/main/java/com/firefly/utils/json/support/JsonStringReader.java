package com.firefly.utils.json.support;

import com.firefly.utils.VerifyUtils;
import com.firefly.utils.io.StringWriter;
import com.firefly.utils.json.exception.JsonException;

public class JsonStringReader {
	private char[] chars;
	private int pos = 0;
	private final int limit;
	private int mark = 0;

	public JsonStringReader(String str) {
		chars = str.toCharArray();
		limit = chars.length;
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
	
	public int position() {
		return pos;
	}

	public int limit() {
		return limit;
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
	
	public boolean isObjectEnd() {
		char c = readAndSkipBlank();
		return c == '}';
	}
	
	public boolean isColon() {
		char c = readAndSkipBlank();
		return c == ':';
	}
	
	public boolean isComma() {
		char c = readAndSkipBlank();
		return c == ',';
	}
	
	public boolean isNull() {
		char ch = readAndSkipBlank();
		if(pos + 3 > limit)
			return false;
		
		if(ch == 'n' && 'u' == read() && 'l' == read() && 'l' == read()) {
			if(pos >= limit)
				return true;
			
			ch = readAndSkipBlank();
			if(isEndFlag(ch)) {
				pos--;
				return true;
			} else
				return false;
		} else
			return false;
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
	
	public boolean readBoolean() {
		boolean ret = false;
		
		mark();
		if(isNull())
			return ret;
		else
			reset();
		
		char ch = readAndSkipBlank();
		boolean isString = (ch == '"');
		if(isString)
			ch = readAndSkipBlank();
		
		if(ch == 't' && 'r' == read() && 'u' == read() && 'e' == read())
			ret = true;
		else if (ch == 'f' && 'a' == read() && 'l' == read() && 's' == read() && 'e' == read())
			ret = false;
		
		if(isString) {
			ch = readAndSkipBlank();
			if(ch != '"')
				throw new JsonException("read boolean error");
		}
		
		return ret;
	}
	
	public int readInt() {
		int value = 0;
		mark();
		if(isNull())
			return value;
		else
			reset();
		
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
		mark();
		if(isNull())
			return value;
		else
			reset();
		
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
	
	public double readDouble() {
		double value = 0.0;
		mark();
		if(isNull())
			return value;
		else
			reset();
		
		char ch = readAndSkipBlank();
		boolean isString = (ch == '"');
		if(isString)
			ch = readAndSkipBlank();
		pos--;
		
		int start = pos;
		for(;;) {
			ch = read();
			if(isString) {
				if(ch == '"')
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
	
	public float readFloat() {
		float value = 0.0F;
		mark();
		if(isNull())
			return value;
		else
			reset();
		
		char ch = readAndSkipBlank();
		boolean isString = (ch == '"');
		if(isString)
			ch = readAndSkipBlank();
		pos--;
		
		int start = pos;
		for(;;) {
			ch = read();
			if(isString) {
				if(ch == '"')
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

	public char[] readField(char[] chs) {
		if(!isString())
			throw new JsonException("read field error");
			
		int cur = pos;
		int len = chs.length;
		boolean skip = true;
		
		int next = pos + len;
		if(next < limit && chars[next] == '"') {
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
			int start = pos;
			for(;;) {
				char c = read();
				if(c == '"')
					break;
			}
			int fieldLen = pos - 1 - start;
			field = new char[fieldLen];
			System.arraycopy(chars, start, field, 0, fieldLen);
			return field;
		}
	}
	
	public char[] readChars() {
		if(!isString())
			throw new JsonException("read field error");
		
		int start = pos;
		for(;;) {
			char c = read();
			if(c == '"')
				break;
		}
		int fieldLen = pos - 1 - start;
		char[] c = new char[fieldLen];
		System.arraycopy(chars, start, c, 0, fieldLen);
		return c;
	}
	
	public void skipValue() {
		char ch = readAndSkipBlank();
		switch (ch) {
		case '"': // 跳过字符串
			for(;;) {
				ch = read();
				if(ch == '"')
					break;
				else if(ch == '\\')
					pos++;
			}
			break;
		case '[': // 跳过数组
			for(;;) {
				// 判断空数组
				mark();
				ch = readAndSkipBlank();
				if(ch == ']')
					break;
				else
					reset();
				
				skipValue();
				ch = readAndSkipBlank();
				if(ch == ']')
					break;
				
				if(ch != ',')
					throw new JsonException("json string array format error");
			}
			break;
		case '{': // 跳过对象
			for(;;) {
				// 判断空对象
				mark();
				ch = readAndSkipBlank();
				if(ch == '}')
					break;
				else
					reset();
				
				readChars();
				if(!isColon())
					throw new JsonException("json string object format error");
				
				skipValue();
				ch = readAndSkipBlank();
				if(ch == '}')
					break;
				
				if(ch != ',')
					throw new JsonException("json string object format error");
			}
			break;

		default: // 跳过数字或者null
			for(;;) {
				ch = read();
				if(isEndFlag(ch)) {
					pos--;
					break;
				}
			}
			break;
		}
	}
	
	public String readString() {
		mark();
		if(isNull())
			return null;
		else
			reset();
		
		if(!isString())
			throw new JsonException("read string error");
		
		StringWriter writer = new StringWriter();
		String ret = null;
		
		int cur = pos;
		int len = 0;
		for(;;) {
			char ch = chars[cur++];
			if(ch == '"') {
				len = cur - pos - 1;
				writer.write(chars, pos, len);
				pos = cur;
				break;
			} else if(ch == '\\') {
				switch (chars[cur++]) {
				case 'b':
					len = cur - 2 - pos;
					writer.write(chars, pos, len);
					writer.write('\b');
					pos = cur;
					break;
				case 'n':
					len = cur - 2 - pos;
					writer.write(chars, pos, len);
					writer.write('\n');
					pos = cur;
					break;
				case 'r':
					len = cur - 2 - pos;
					writer.write(chars, pos, len);
					writer.write('\r');
					pos = cur;
					break;
				case 'f':
					len = cur - 2 - pos;
					writer.write(chars, pos, len);
					writer.write('\f');
					pos = cur;
					break;
				case '\\':
					len = cur - 2 - pos;
					writer.write(chars, pos, len);
					writer.write('\\');
					pos = cur;
					break;
				case '"':
					len = cur - 2 - pos;
					writer.write(chars, pos, len);
					writer.write('"');
					pos = cur;
					break;
				case 't':
					len = cur - 2 - pos;
					writer.write(chars, pos, len);
					writer.write('\t');
					pos = cur;
					break;
				}
			}
			
		}
		try {
			ret = writer.toString();
		} finally {
			writer.close();
		}
		return ret;
	}

}
