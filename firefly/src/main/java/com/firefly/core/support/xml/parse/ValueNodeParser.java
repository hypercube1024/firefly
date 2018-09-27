package com.firefly.core.support.xml.parse;

import org.w3c.dom.Element;

import static com.firefly.core.support.xml.parse.XmlNodeConstants.*;

import com.firefly.core.support.xml.ManagedValue;
import com.firefly.utils.dom.Dom;

public class ValueNodeParser extends AbstractXmlNodeParser implements XmlNodeParser {

    @Override
    public Object parse(Element ele, Dom dom) {
        ManagedValue typedValue = new ManagedValue();
        String value = dom.getTextValue(ele);
        String typeName = null;
        if (ele.hasAttribute(TYPE_ATTRIBUTE)) {
            typeName = ele.getAttribute(TYPE_ATTRIBUTE);
            if (typeName == null) {
                error("<value> element contains empty target attribute");
                return null;
            }
        }

        typedValue.setValue(value);
        typedValue.setTypeName(typeName);
        return typedValue;
    }
}
