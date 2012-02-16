package com.firefly.template;

import java.util.HashMap;
import java.util.Map;

public class FunctionRegistry {
	private static final Map<String, Function> MAP = new HashMap<String, Function>();
	
	public static void add(String name, Function function) {
		MAP.put(name, function);
	}
	
	public static Function get(String name) {
		return MAP.get(name);
	}
}
