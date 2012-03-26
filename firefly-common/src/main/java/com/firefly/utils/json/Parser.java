package com.firefly.utils.json;

import com.firefly.utils.json.support.JsonStringReader;

public interface Parser {
	void convertTo(JsonStringReader reader);
}
