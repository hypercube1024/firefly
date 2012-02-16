package com.firefly.utils.json.serializer;

import static com.firefly.utils.json.JsonStringSymbol.QUOTE;

import java.io.IOException;

import com.firefly.utils.json.Serializer;
import com.firefly.utils.json.support.JsonStringWriter;

public class EnumSerializer implements Serializer {

	@Override
	public void convertTo(JsonStringWriter writer, Object obj)
			throws IOException {
		Enum<?> e = (Enum<?>)obj;
		writer.append(QUOTE);
		writer.write(e.name());
		writer.append(QUOTE);
	}

}
