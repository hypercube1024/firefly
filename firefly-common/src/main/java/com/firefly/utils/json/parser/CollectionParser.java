package com.firefly.utils.json.parser;

import com.firefly.utils.json.JsonReader;
import com.firefly.utils.json.exception.JsonException;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Collection;

public class CollectionParser extends ComplexTypeParser {
	
	public CollectionParser(Type elementType) {
		super(elementType);
	}

	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Object convertTo(JsonReader reader, Class<?> clazz) throws IOException {
		if(reader.isNull())
			return null;
		
		if(!reader.isArray())
			throw new JsonException("json string is not array format");
		
		Collection obj = null;
		try {
			obj = (Collection)clazz.newInstance();
		} catch (Throwable e) {
		}
		
		if(reader.isEmptyArray())
			return obj;
		
		for(;;) {
			obj.add(elementMetaInfo.getValue(reader));
			
			char ch = reader.readAndSkipBlank();
			if(ch == ']')
				return obj;

			if(ch != ',')
				throw new JsonException("missing ','");
		}
	}

}
