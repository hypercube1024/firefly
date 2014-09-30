package com.firefly.core.support.xml.parse;

import static com.firefly.core.support.xml.parse.XmlNodeConstants.ARRAY_ELEMENT;
import static com.firefly.core.support.xml.parse.XmlNodeConstants.BEAN_ELEMENT;
import static com.firefly.core.support.xml.parse.XmlNodeConstants.CONTRUCTOR_ELEMENT;
import static com.firefly.core.support.xml.parse.XmlNodeConstants.LIST_ELEMENT;
import static com.firefly.core.support.xml.parse.XmlNodeConstants.MAP_ELEMENT;
import static com.firefly.core.support.xml.parse.XmlNodeConstants.REF_ELEMENT;
import static com.firefly.core.support.xml.parse.XmlNodeConstants.VALUE_ELEMENT;

import java.util.HashMap;
import java.util.Map;

public class XmlNodeParserFactory {
	private static final Map<String, XmlNodeParser> map = new HashMap<String, XmlNodeParser>();

	static {
		map.put(BEAN_ELEMENT, new BeanNodeParser());
		map.put(REF_ELEMENT, new RefNodeParser());
		map.put(VALUE_ELEMENT, new ValueNodeParser());
		map.put(LIST_ELEMENT, new ListNodeParser());
		map.put(ARRAY_ELEMENT, new ArrayNodeParser());
		map.put(MAP_ELEMENT, new MapNodeParser());
		map.put(CONTRUCTOR_ELEMENT, new ContructorParser());
	}

	public static XmlNodeParser getParser(String elementName) {
		return map.get(elementName);
	}
}
