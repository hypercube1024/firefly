package com.firefly.core.support.xml.parse;

import com.firefly.utils.dom.Dom;
import org.w3c.dom.Element;

public interface XmlNodeParser {
    Object parse(Element ele, Dom dom);
}
