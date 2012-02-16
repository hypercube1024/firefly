package com.firefly.utils.json;

import java.io.IOException;

import com.firefly.utils.json.support.JsonStringWriter;

public interface Serializer {
	void convertTo(JsonStringWriter writer, Object obj) throws IOException;
}
