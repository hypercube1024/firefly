package com.firefly.core.support.xml.parse;

import java.util.List;

import org.w3c.dom.Element;

import com.firefly.core.support.xml.ManagedArray;
import com.firefly.core.support.xml.XmlManagedNode;
import com.firefly.utils.dom.Dom;

public class ArrayNodeParser extends AbstractXmlNodeParser implements XmlNodeParser {

    @Override
    public Object parse(Element ele, Dom dom) {
        ManagedArray<XmlManagedNode> target = new ManagedArray<XmlManagedNode>();
        List<Element> elements = dom.elements(ele);
        for (Element e : elements) {
            target.add((XmlManagedNode) XmlNodeStateMachine.stateProcessor(e, dom));
        }
        return target;
    }
}
