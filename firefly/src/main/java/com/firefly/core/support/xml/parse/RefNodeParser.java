package com.firefly.core.support.xml.parse;

import org.w3c.dom.Element;

import static com.firefly.core.support.xml.parse.XmlNodeConstants.*;

import com.firefly.core.support.xml.ManagedRef;
import com.firefly.utils.StringUtils;
import com.firefly.utils.dom.Dom;

public class RefNodeParser extends AbstractXmlNodeParser implements XmlNodeParser {

    @Override
    public Object parse(Element ele, Dom dom) {
        if (ele.hasAttribute(BEAN_REF_ATTRIBUTE)) {
            String refText = ele.getAttribute(BEAN_REF_ATTRIBUTE);
            if (StringUtils.hasText(refText)) {
                ManagedRef ref = new ManagedRef();
                ref.setBeanName(refText);
                return ref;
            } else {
                error("<ref> element contains empty target attribute");
                return null;
            }
        } else {
            error("'bean' is required for <ref> element");
            return null;
        }
    }

}
