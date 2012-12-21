package com.firefly.utils.json.serializer;

import static com.firefly.utils.json.JsonStringSymbol.ARRAY_PRE;
import static com.firefly.utils.json.JsonStringSymbol.ARRAY_SUF;
import static com.firefly.utils.json.JsonStringSymbol.SEPARATOR;
import static com.firefly.utils.json.JsonStringSymbol.EMPTY_ARRAY;

import java.io.IOException;

import com.firefly.utils.json.JsonWriter;
import com.firefly.utils.json.Serializer;

public class ArraySerializer implements Serializer {

	@Override
	public void convertTo(JsonWriter writer, Object obj)
			throws IOException {
		Object[] objArray = (Object[]) obj;
		int iMax = objArray.length - 1;
		if (iMax == -1) {
			writer.write(EMPTY_ARRAY);
			return;
		}

		writer.append(ARRAY_PRE);
		for (int i = 0;; i++) {
			SerialStateMachine.toJson(objArray[i], writer);
			if (i == iMax) {
				writer.append(ARRAY_SUF);
				return;
			}
			writer.append(SEPARATOR);
		}
	}

}
