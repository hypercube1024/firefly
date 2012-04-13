package com.firefly.utils.json.serializer;

import com.firefly.utils.json.Serializer;

public abstract class SimpleArraySerializer implements Serializer {

	protected boolean primitive;

	public SimpleArraySerializer(boolean primitive) {
		this.primitive = primitive;
	}

}
