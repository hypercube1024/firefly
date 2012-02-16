package com.firefly.utils.json.serializer;

import static com.firefly.utils.json.JsonStringSymbol.ARRAY_PRE;
import static com.firefly.utils.json.JsonStringSymbol.ARRAY_SUF;
import static com.firefly.utils.json.JsonStringSymbol.SEPARATOR;
import static com.firefly.utils.json.JsonStringSymbol.EMPTY_ARRAY;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;

import com.firefly.utils.json.Serializer;
import com.firefly.utils.json.support.JsonStringWriter;

public class CollectionSerializer implements Serializer {

	@Override
	public void convertTo(JsonStringWriter writer, Object obj)
			throws IOException {
		Collection<?> collection = (Collection<?>) obj;
		if (collection.size() == 0) {
			writer.write(EMPTY_ARRAY);
			return;
		}

		writer.append(ARRAY_PRE);
		for (Iterator<?> it = collection.iterator();;) {
			StateMachine.toJson(it.next(), writer);
			if (!it.hasNext()) {
				writer.append(ARRAY_SUF);
				return;
			}
			writer.append(SEPARATOR);
		}

	}

}
