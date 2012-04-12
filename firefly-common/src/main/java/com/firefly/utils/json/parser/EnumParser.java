package com.firefly.utils.json.parser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.firefly.utils.json.Parser;
import com.firefly.utils.json.support.JsonStringReader;

public class EnumParser implements Parser {
	
	private EnumObj[] enumObjs;
	
	public EnumParser(Class<?> clazz) {
		List<EnumObj> list = new ArrayList<EnumObj>();
		Object[] o = clazz.getEnumConstants();
		enumObjs = new EnumObj[o.length];
		for (Object o1 : o) {
			EnumObj enumObj = new EnumObj();
			enumObj.e = o1;
			enumObj.key = ((Enum<?>)o1).name().toCharArray();
			list.add(enumObj);
		}
		list.toArray(enumObjs);
	}

	@Override
	public Object convertTo(JsonStringReader reader, Class<?> clazz) {
		return find(reader.readEnum());
	}
	
	private Object find(char[] key) {
		for(EnumObj eo : enumObjs) {
			if(Arrays.equals(eo.key, key))
				return eo.e;
		}
		return null;
	}
	
	private class EnumObj {
		Object e;
		char[] key;
	}

}
