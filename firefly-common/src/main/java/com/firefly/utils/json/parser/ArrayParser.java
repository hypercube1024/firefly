package com.firefly.utils.json.parser;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

import com.firefly.utils.ReflectUtils;
import com.firefly.utils.json.JsonReader;
import com.firefly.utils.json.Parser;
import com.firefly.utils.json.exception.JsonException;
import com.firefly.utils.json.support.ParserMetaInfo;

public class ArrayParser implements Parser {
	
	private ParserMetaInfo elementMetaInfo;
	
	public ArrayParser(Class<?> clazz) {
		elementMetaInfo = new ParserMetaInfo();
		elementMetaInfo.setType(clazz);
		elementMetaInfo.setParser(ParserStateMachine.getParser(clazz));
	}

	@Override
	public Object convertTo(JsonReader reader, Class<?> clazz) throws IOException {
		if(reader.isNull())
			return null;
		
		if(!reader.isArray())
			throw new JsonException("json string is not array format");
		
		if(reader.isEmptyArray())
			return Array.newInstance(elementMetaInfo.getType(), 0);
		
		List<Object> obj = new ArrayList<Object>();
		
		for(;;) {
			obj.add(elementMetaInfo.getValue(reader));
			
			char ch = reader.readAndSkipBlank();
			if(ch == ']')
				return copyOf(obj);

			if(ch != ',')
				throw new JsonException("missing ','");
		}
	}
	
	public Object copyOf(List<Object> list) {
		Object ret = Array.newInstance(elementMetaInfo.getType(), list.size());
		for (int i = 0; i < list.size(); i++) {
			try {
				ReflectUtils.arraySet(ret, i, list.get(i));
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}
		return ret;
	}

}
