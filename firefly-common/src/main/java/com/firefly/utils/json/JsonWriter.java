package com.firefly.utils.json;

import java.io.Writer;

abstract public class JsonWriter extends Writer {
	abstract public void writeNull();
	
	abstract public void writeInt(int i);
	
	abstract public void writeShort(short i);
	
	abstract public void writeBoolean(boolean b);
	
	abstract public void writeByte(byte i);
	
	abstract public void writeLong(long i);

	abstract public void pushRef(Object obj);

	abstract public boolean existRef(Object obj);

	abstract public void popRef();

	abstract public void writeStringWithQuote(String value);

	abstract public void writeStringArray(String[] array);

	abstract public void writeIntArray(int[] array);

	abstract public void writeIntArray(Integer[] array);

	abstract public void writeShortArray(short[] array);

	abstract public void writeShortArray(Short[] array);

	abstract public void writeLongArray(long[] array);

	abstract public void writeLongArray(Long[] array);

	abstract public void writeBooleanArray(boolean[] array);

	abstract public void writeBooleanArray(Boolean[] array);

}