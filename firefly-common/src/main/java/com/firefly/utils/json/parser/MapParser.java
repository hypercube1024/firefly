package com.firefly.utils.json.parser;

import com.firefly.utils.json.JsonReader;
import com.firefly.utils.json.exception.JsonException;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Map;

public class MapParser extends ComplexTypeParser {
	
	public MapParser(Type elementType) {
		super(elementType);
	}

	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Object convertTo(JsonReader reader, Class<?> clazz) throws IOException {
		if(reader.isNull())
			return null;
		
		if(!reader.isObject())
			throw new JsonException("json string is not object format");
		
		Map obj = null;
		try {
			obj = (Map)clazz.newInstance();
		} catch (Throwable e) {
		}
		
		if(reader.isEmptyObject())
			return obj;
		
		for(;;) {
			String key = reader.readString();
			if(!reader.isColon())
				throw new JsonException("missing ':'");
			
			obj.put(key, elementMetaInfo.getValue(reader));
			
			char ch = reader.readAndSkipBlank();
			if(ch == '}')
				return obj;

			if(ch != ',')
				throw new JsonException("missing ','");
		}
	}

}
