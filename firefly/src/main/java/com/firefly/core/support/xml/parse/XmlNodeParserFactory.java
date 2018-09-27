package com.firefly.core.support.xml.parse;

import java.util.HashMap;
import java.util.Map;

import static com.firefly.core.support.xml.parse.XmlNodeConstants.*;

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
