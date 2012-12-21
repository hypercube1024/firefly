package com.firefly.utils.json;

import java.io.Reader;

abstract public class JsonReader extends Reader{

	abstract public int getMarkPos();

	abstract public void markPos();

	abstract public void resetPos();

	abstract public char get(int index);

	abstract public int position();

	abstract public int limit();

	abstract public boolean isEndFlag(char ch);

	abstract public boolean isString();

	abstract public boolean isArray();

	abstract public boolean isObject();

	abstract public boolean isObjectEnd();

	abstract public boolean isColon();

	abstract public boolean isComma();

	abstract public boolean isNull();

	abstract public char readAndSkipBlank();

	abstract public boolean readBoolean();

	abstract public int readInt();

	abstract public long readLong();

	abstract public double readDouble();

	abstract public float readFloat();

	abstract public char[] readField(char[] chs);

	abstract public char[] readChars();

	abstract public void skipValue();

	abstract public String readString();

}