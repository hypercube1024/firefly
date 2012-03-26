package com.firefly.utils.json.serializer;

import static com.firefly.utils.json.JsonStringSymbol.OBJ_PRE;
import static com.firefly.utils.json.JsonStringSymbol.OBJ_SUF;

import java.io.IOException;

import com.firefly.utils.json.Serializer;
import com.firefly.utils.json.compiler.EncodeCompiler;
import com.firefly.utils.json.support.SerializerMetaInfo;
import com.firefly.utils.json.support.JsonStringWriter;

public class ObjectNoCheckSerializer implements Serializer {
	
	private SerializerMetaInfo[] serializerMetaInfos;
	
	public ObjectNoCheckSerializer(Class<?> clazz) {
		serializerMetaInfos = EncodeCompiler.compile(clazz);
	}

	@Override
	public void convertTo(JsonStringWriter writer, Object obj)
			throws IOException {
		writer.append(OBJ_PRE);
		for(SerializerMetaInfo metaInfo : serializerMetaInfos){
			writer.write(metaInfo.getPropertyName());
			metaInfo.toJson(obj, writer);
		}
		writer.append(OBJ_SUF);
	}

}
