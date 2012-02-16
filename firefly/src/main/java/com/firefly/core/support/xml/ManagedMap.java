package com.firefly.core.support.xml;

import java.util.HashMap;

@SuppressWarnings("serial")
public class ManagedMap<K, V> extends HashMap<K, V> {
	
	private String typeName;

	public String getTypeName() {
		return typeName;
	}

	public void setTypeName(String typeName) {
		this.typeName = typeName;
	}
}
