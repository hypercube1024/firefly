package com.firefly.core.support.xml.parse;

import org.w3c.dom.Element;

import com.firefly.utils.dom.Dom;

public interface XmlNodeParser {
	Object parse(Element ele, Dom dom);
}
