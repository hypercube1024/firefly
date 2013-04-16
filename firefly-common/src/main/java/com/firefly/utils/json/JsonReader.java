package com.firefly.utils.json;

import java.io.Reader;
import java.math.BigDecimal;
import java.math.BigInteger;

abstract public class JsonReader extends Reader{
	
	@Override
	public boolean markSupported() {
        return true;
    }

	abstract public boolean isEndFlag(char ch);

	abstract public boolean isString();

	abstract public boolean isArray();
	
	/**
	 * 判断空数组，必须先调用isArray()，并且会重置游标
	 * @return 是否size为0的数组
	 */
	abstract public boolean isEmptyArray();

	abstract public boolean isObject();
	
	/**
	 * 判断空对象，必须先调用isObject()，并且会重置游标
	 * @return 是否空({})json对象
	 */
	abstract public boolean isEmptyObject();
	
	/**
	 * 判断是否为null值，并且会重置游标
	 * @return 是否null
	 */
	abstract public boolean isNull();

	abstract public boolean isColon();

	abstract public boolean isComma();

	

	abstract public char readAndSkipBlank();

	abstract public boolean readBoolean();

	abstract public int readInt();

	abstract public long readLong();
	
	abstract public BigInteger readBigInteger();
	
	abstract public BigDecimal readBigDecimal();

	abstract public double readDouble();

	abstract public float readFloat();

	abstract public char[] readField(char[] chs);

	abstract public char[] readChars();

	abstract public void skipValue();

	abstract public String readString();

}