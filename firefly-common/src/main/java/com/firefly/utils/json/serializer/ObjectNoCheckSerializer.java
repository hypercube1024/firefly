package com.firefly.utils.json.serializer;

import static com.firefly.utils.json.JsonStringSymbol.OBJ_PRE;
import static com.firefly.utils.json.JsonStringSymbol.OBJ_SUF;

import java.io.IOException;

import com.firefly.utils.json.Serializer;
import com.firefly.utils.json.compiler.EncodeCompiler;
import com.firefly.utils.json.support.JsonObjMetaInfo;
import com.firefly.utils.json.support.JsonStringWriter;

public class ObjectNoCheckSerializer implements Serializer {
	
	private JsonObjMetaInfo[] jsonObjMetaInfos;
	
	public ObjectNoCheckSerializer(Class<?> clazz) {
		jsonObjMetaInfos = EncodeCompiler.compile(clazz);
	}

	@Override
	public void convertTo(JsonStringWriter writer, Object obj)
			throws IOException {
		writer.append(OBJ_PRE);
		for(JsonObjMetaInfo metaInfo : jsonObjMetaInfos){
			writer.write(metaInfo.getPropertyName());
			metaInfo.toJson(obj, writer);
		}
		writer.append(OBJ_SUF);
	}

}
