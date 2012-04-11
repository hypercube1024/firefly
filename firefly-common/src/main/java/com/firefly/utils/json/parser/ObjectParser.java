package com.firefly.utils.json.parser;

import com.firefly.utils.json.Parser;
import com.firefly.utils.json.compiler.DecodeCompiler;
import com.firefly.utils.json.exception.JsonException;
import com.firefly.utils.json.support.JsonStringReader;
import com.firefly.utils.json.support.ParserMetaInfo;

public class ObjectParser implements Parser {
	
	private ParserMetaInfo[] parserMetaInfos;
	private int max;
	
	public void init(Class<?> clazz) {
		parserMetaInfos = DecodeCompiler.compile(clazz);
		max = parserMetaInfos.length - 1;
	}

	@Override
	public Object convertTo(JsonStringReader reader, Class<?> clazz) {
		reader.mark();
		if(reader.isNull())
			return null;
		else
			reader.reset();
		
		if(!reader.isObject())
			throw new JsonException("json string is not object format");
		
		Object obj = null;
		try {
			obj = clazz.newInstance();
		} catch (Throwable e) {
			e.printStackTrace();
		}
		
		for (int i = 0;;i++) {
			ParserMetaInfo parser = parserMetaInfos[i];
			char[] field = reader.readField(parser.getPropertyName());
			if(!reader.isColon())
				throw new JsonException("missing ':'");
			
			if(field == null) { // 顺序相同，快速跳过
				parser.invoke(obj, reader);
			} else {
				ParserMetaInfo np = find(field);
				if(np != null)
					np.invoke(obj, reader);
			}
			
			if(i == max)
				break;
			
			char ch = reader.readAndSkipBlank();
			if(ch == '}') // json string的域数量比元信息少，提前结束
				return obj;

			if(ch != ',')
				throw new JsonException("missing ','");
		}
		
		if(!reader.isObjectEnd())
			throw new JsonException("json string is not object format");
		
		return obj;
	}
	
	private ParserMetaInfo find(char[] field) {
		for(ParserMetaInfo parserMetaInfo : parserMetaInfos) {
			if(parserMetaInfo.equals(field))
				return parserMetaInfo;
		}
		return null;
	}

}
