package com.firefly.core.support.xml.parse;

import java.util.List;

import org.w3c.dom.Element;

import com.firefly.core.support.xml.ManagedMap;
import com.firefly.core.support.xml.ManagedValue;
import com.firefly.core.support.xml.XmlManagedNode;
import com.firefly.utils.dom.Dom;

public class MapNodeParser extends AbstractXmlNodeParser implements
        XmlNodeParser {

    @Override
    public Object parse(Element ele, Dom dom) {
        String typeName = ele.getAttribute(XmlNodeConstants.TYPE_ATTRIBUTE);
        ManagedMap<XmlManagedNode, XmlManagedNode> target = new ManagedMap<XmlManagedNode, XmlManagedNode>();
        target.setTypeName(typeName);

        List<Element> elements = dom.elements(ele);
        for (Element entry : elements) {
            XmlManagedNode key = null;
            XmlManagedNode value = null;
            if (entry.hasAttribute(XmlNodeConstants.KEY_ATTRIBUTE)) {
                key = new ManagedValue(entry.getAttribute(XmlNodeConstants.KEY_ATTRIBUTE));
            }

            if (entry.hasAttribute(XmlNodeConstants.VALUE_ATTRIBUTE)) {
                value = new ManagedValue(entry.getAttribute(XmlNodeConstants.VALUE_ATTRIBUTE));
            }

            List<Element> keyEle = dom.elements(entry, XmlNodeConstants.MAP_KEY_ELEMENT);
            if (keyEle.size() > 1) {
                error("must not contain more than one key-element");
            } else if (keyEle.size() == 1) {
                if (key != null) {
                    error("only allowed to contain either 'key' attribute OR key-element");
                } else {
                    List<Element> subKey = dom.elements(keyEle.get(0));
                    if (subKey.size() != 1) {
                        String keyText = dom.getTextValue(keyEle.get(0));
                        if (keyText == null)
                            error("must contain one key-sub-element");
                        else
                            key = new ManagedValue(keyText);
                    } else {
                        key = (XmlManagedNode) XmlNodeStateMachine.stateProcessor(subKey.get(0), dom);
                    }
                }
            } else {
                if (key == null)
                    error("not contain 'key' attribute OR key-element");
            }

            List<Element> valueEle = dom.elements(entry, XmlNodeConstants.MAP_VALUE_ELEMENT);
            if (valueEle.size() > 1) {
                error("must not contain more than one value-element");
            } else if (valueEle.size() == 1) {
                if (value != null) {
                    error("only allowed to contain either 'value' attribute OR value-element");
                } else {
                    List<Element> subValue = dom.elements(valueEle.get(0));
                    if (subValue.size() != 1) {
                        String valueText = dom.getTextValue(valueEle.get(0));
                        if (valueText == null)
                            error("must contain one value-sub-element");
                        else
                            value = new ManagedValue(valueText);
                    } else {
                        value = (XmlManagedNode) XmlNodeStateMachine.stateProcessor(subValue.get(0), dom);
                    }
                }
            } else {
                if (value == null)
                    error("not contain 'value' attribute OR value-element");
            }

            target.put(key, value);
        }
        return target;
    }

}
