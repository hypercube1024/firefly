package com.firefly.utils.json;

import java.io.Reader;
import java.math.BigDecimal;
import java.math.BigInteger;

abstract public class JsonReader extends Reader{
	
	@Override
	public boolean markSupported() {
        return true;
    }
	
	abstract public void increasePosition();
	
	abstract public void decreasePosition();
	
	abstract public int position();
	
	abstract public boolean isEnd();

	abstract public boolean isEndFlag(char ch);

	abstract public boolean isString();

	abstract public boolean isArray();
	
	/**
	 * Invokes it after isArray() and resets index
	 * @return If it returns true, the size of array is 0.
	 */
	abstract public boolean isEmptyArray();

	abstract public boolean isObject();
	
	/**
	 * Invokes it after isObject() and resets index
	 * @return If it returns true, the object is not any properties
	 */
	abstract public boolean isEmptyObject();
	
	/**
	 * Judges null and resets index
	 * @return If it returns true, it's null.
	 */
	abstract public boolean isNull();

	abstract public boolean isColon();

	abstract public boolean isComma();

	

	abstract public char readAndSkipBlank();

	abstract public boolean readBoolean();

	abstract public int readInt();

	abstract public long readLong();
	
	abstract public String readValueAsString();
	
	abstract public BigInteger readBigInteger();
	
	abstract public BigDecimal readBigDecimal();

	abstract public double readDouble();

	abstract public float readFloat();

	abstract public char[] readField(char[] chs);

	abstract public char[] readChars();

	abstract public void skipValue();

	abstract public String readString();

}