package com.firefly.utils.json;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;

public class JsonObject extends HashMap<String, Object> {

	private static final long serialVersionUID = 1016483993009899270L;
	
	public JsonObject getJsonObject(String key) {
		return (JsonObject)get(key);
	}
	
	public JsonArray getJsonArray(String key) {
		return (JsonArray)get(key);
	}
	
	public String getString(String key) {
		return (String)get(key);
	}
	
	public byte getByte(String key) {
		return Byte.parseByte(getString(key));
	}
	
	public short getShort(String key) {
		return Short.parseShort(getString(key));
	}
	
	public int getInteger(String key) {
		return Integer.parseInt(getString(key));
	}
	
	public long getLong(String key) {
		return Long.parseLong(getString(key));
	}
	
	public float getFloat(String key) {
		return Float.parseFloat(getString(key));
	}
	
	public double getDouble(String key) {
		return Double.parseDouble(getString(key));
	}
	
	public BigInteger getBigInteger(String key) {
		return new BigInteger(getString(key));
	}
	
	public BigDecimal getBigDecimal(String key) {
		return new BigDecimal(getString(key));
	}
	
	public boolean getBoolean(String key) {
		return Boolean.parseBoolean(getString(key));
	}

}
