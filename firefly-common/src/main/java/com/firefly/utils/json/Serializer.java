package com.firefly.utils.json;

import java.io.IOException;

public interface Serializer {
	void convertTo(JsonWriter writer, Object obj) throws IOException;
}
