package com.firefly.core.support.xml.parse;

import com.firefly.core.support.xml.ManagedList;
import com.firefly.core.support.xml.XmlManagedNode;
import com.firefly.utils.dom.Dom;
import org.w3c.dom.Element;

import java.util.List;

import static com.firefly.core.support.xml.parse.XmlNodeConstants.TYPE_ATTRIBUTE;

public class ListNodeParser extends AbstractXmlNodeParser implements XmlNodeParser {

    @Override
    public Object parse(Element ele, Dom dom) {
        String typeName = ele.getAttribute(TYPE_ATTRIBUTE);
        ManagedList<XmlManagedNode> target = new ManagedList<XmlManagedNode>();
        target.setTypeName(typeName);
        List<Element> elements = dom.elements(ele);
        for (Element e : elements) {
            target.add((XmlManagedNode) XmlNodeStateMachine.stateProcessor(e, dom));
        }
        return target;
    }
}
