package com.firefly.utils.json.serializer;

import static com.firefly.utils.json.JsonStringSymbol.QUOTE;

import com.firefly.utils.json.Serializer;
import com.firefly.utils.json.support.JsonStringWriter;

public class CharacterSerializer implements Serializer {

	@Override
	public void convertTo(JsonStringWriter writer, Object obj) {
		writer.append(QUOTE).append((Character)obj).append(QUOTE);
	}

}
