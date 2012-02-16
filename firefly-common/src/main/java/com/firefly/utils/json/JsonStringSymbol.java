package com.firefly.utils.json;

public interface JsonStringSymbol {
	char QUOTE = '"';
	char ARRAY_PRE = '[';
	char ARRAY_SUF = ']';
	char OBJ_PRE = '{';
	char OBJ_SUF = '}';
	char SEPARATOR = ',';
	char OBJ_SEPARATOR = ':';
	char[] EMPTY_ARRAY = "[]".toCharArray();
}
