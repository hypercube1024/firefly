package com.firefly.utils.json;

import com.firefly.utils.StringUtils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;

public class JsonObject extends HashMap<String, Object> {

	private static final long serialVersionUID = 1016483993009899270L;
	
	public JsonObject getJsonObject(String key) {
		Object ret = get(key);
		if(ret instanceof String && "null".equals(ret))
			return null;
		return (JsonObject)ret;
	}
	
	public JsonArray getJsonArray(String key) {
		Object ret = get(key);
		if(ret instanceof String && "null".equals(ret))
			return null;
		return (JsonArray)get(key);
	}
	
	public String getString(String key) {
		return (String)get(key);
	}
	
	public byte getByte(String key) {
		String str = getString(key);
		if (StringUtils.hasText(str)) {
			return Byte.parseByte(str);
		} else {
			return 0;
		}
	}
	
	public short getShort(String key) {
		String str = getString(key);
		if (StringUtils.hasText(str)) {
			return Short.parseShort(str);
		} else {
			return 0;
		}
	}
	
	public int getInteger(String key) {
		String str = getString(key);
		if (StringUtils.hasText(str)) {
			return Integer.parseInt(str);
		} else {
			return 0;
		}
	}
	
	public long getLong(String key) {
		String str = getString(key);
		if (StringUtils.hasText(str)) {
			return Long.parseLong(str);
		} else {
			return 0;
		}
	}
	
	public float getFloat(String key) {
		String str = getString(key);
		if (StringUtils.hasText(str)) {
			return Float.parseFloat(str);
		} else {
			return 0;
		}
	}
	
	public double getDouble(String key) {
		String str = getString(key);
		if (StringUtils.hasText(str)) {
			return Double.parseDouble(str);
		} else {
			return 0;
		}
	}
	
	public BigInteger getBigInteger(String key) {
		String str = getString(key);
		if (StringUtils.hasText(str)) {
			return new BigInteger(str);
		} else {
			return null;
		}
	}
	
	public BigDecimal getBigDecimal(String key) {
		String str = getString(key);
		if (StringUtils.hasText(str)) {
			return new BigDecimal(str);
		} else {
			return null;
		}
	}
	
	public boolean getBoolean(String key) {
		return Boolean.parseBoolean(getString(key));
	}

}
