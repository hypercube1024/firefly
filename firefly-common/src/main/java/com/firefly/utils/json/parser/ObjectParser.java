package com.firefly.utils.json.parser;

import com.firefly.utils.json.Parser;
import com.firefly.utils.json.compiler.DecodeCompiler;
import com.firefly.utils.json.exception.JsonException;
import com.firefly.utils.json.support.JsonStringReader;
import com.firefly.utils.json.support.ParserMetaInfo;

public class ObjectParser implements Parser {
	
	private ParserMetaInfo[] parserMetaInfos;
	private int max;
	
	public ObjectParser(Class<?> clazz) {
		parserMetaInfos = DecodeCompiler.compile(clazz);
		max = parserMetaInfos.length - 1;
	}

	@Override
	public Object convertTo(JsonStringReader reader, Class<?> clazz) {
		if(!reader.isObject())
			throw new JsonException("json string is not object format");
		
		Object obj = null;
		try {
			obj = clazz.newInstance();
		} catch (Throwable e) {
			e.printStackTrace();
		}
		
		for (int i = 0;; i++) {
			ParserMetaInfo parser = parserMetaInfos[i];
			char[] field = reader.readField(parser.getPropertyName());
			if(field == null) {
				if(!reader.isColon())
					throw new JsonException("json string field format error");
				
				parser.invoke(obj, reader);
				
				if(i == max)
					break;

				if(!reader.isComma())
					throw new JsonException("json string value format error");
			} else {
				// TODO 无法跳过的情况下
			}
			
			
		}
		
		if(!reader.isObjectEnd())
			throw new JsonException("json string is not object format");
		
		return obj;
	}

}
