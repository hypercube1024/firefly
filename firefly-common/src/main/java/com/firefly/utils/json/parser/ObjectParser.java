package com.firefly.utils.json.parser;

import java.util.HashMap;
import java.util.Map;

import com.firefly.utils.json.Parser;
import com.firefly.utils.json.compiler.DecodeCompiler;
import com.firefly.utils.json.exception.JsonException;
import com.firefly.utils.json.support.JsonStringReader;
import com.firefly.utils.json.support.ParserMetaInfo;

public class ObjectParser implements Parser {
	
	private ParserMetaInfo[] parserMetaInfos;
	private int max;
	private Map<String, ParserMetaInfo> map;
	private boolean useMap;
	
	public void init(Class<?> clazz) {
		parserMetaInfos = DecodeCompiler.compile(clazz);
		max = parserMetaInfos.length - 1;
		if(max >= 8) {
			map = new HashMap<String, ParserMetaInfo>();
			for(ParserMetaInfo parserMetaInfo : parserMetaInfos) {
				map.put(parserMetaInfo.getPropertyNameString(), parserMetaInfo);
			}
			useMap = true;
		}
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
		
		// 判断空对象
		reader.mark();
		char c0 = reader.readAndSkipBlank();
		if(c0 == '}')
			return obj;
		else
			reader.reset();
		
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
				else
					reader.skipValue();
			}
			
			if(i == max)
				break;
			
			char ch = reader.readAndSkipBlank();
			if(ch == '}') // json string 的域数量比元信息少，提前结束
				return obj;

			if(ch != ',')
				throw new JsonException("missing ','");
		}
		
		char ch = reader.readAndSkipBlank();
		if(ch == '}')
			return obj;
		
		if(ch != ',')
			throw new JsonException("json string is not object format");
		
		for(;;) { // json string 的域数量比元信息多，继续读取
			char[] field = reader.readChars();
			if(!reader.isColon())
				throw new JsonException("missing ':'");
			
			ParserMetaInfo np = find(field);
			if(np != null)
				np.invoke(obj, reader);
			else
				reader.skipValue();
			
			char c = reader.readAndSkipBlank();
			if(c == '}') // 读到末尾
				return obj;

			
			if(c != ',')
				throw new JsonException("missing ','");
		}
	}
	
	private ParserMetaInfo find(char[] field) {
		if(useMap) {
			return map.get(new String(field));
		} else {
			for(ParserMetaInfo parserMetaInfo : parserMetaInfos) {
				if(parserMetaInfo.equals(field))
					return parserMetaInfo;
			}
		}
		return null;
	}

}
