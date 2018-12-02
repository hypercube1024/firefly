package com.fireflysource.log.internal.utils.xml;

import com.fireflysource.log.internal.utils.StringUtils;
import org.w3c.dom.CharacterData;
import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class DefaultDom implements Dom {

    private DocumentBuilder db;

    public DefaultDom() {
        try {
            db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new XmlParsingException(e);
        }
    }

    @Override
    public Document getDocument(String file) {
        try (InputStream is = DefaultDom.class.getResourceAsStream("/" + file)) {
            if (is == null) {
                throw new XmlParsingException("the configuration file: " + file + " is not found");
            }
            Document doc = db.parse(is);
            return doc;
        } catch (Exception e) {
            throw new XmlParsingException(e);
        }
    }

    @Override
    public Element getRoot(Document doc) {
        return doc.getDocumentElement();
    }

    @Override
    public List<Element> elements(Element e) {
        return elements(e, null);
    }

    @Override
    public List<Element> elements(Element e, String name) {
        List<Element> eList = new ArrayList<Element>();

        NodeList nodeList = e.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); ++i) {
            Node node = nodeList.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                if (name != null) {
                    if (node.getNodeName().equals(name))
                        eList.add((Element) node);
                } else {
                    eList.add((Element) node);
                }
            }
        }
        return eList;
    }

    @Override
    public Element element(Element e, String name) {
        NodeList element = e.getElementsByTagName(name);
        if (element != null && e.getNodeType() == Node.ELEMENT_NODE) {
            return (Element) element.item(0);
        }
        return null;
    }

    @Override
    public String getTextValue(Element valueElement) {
        if (valueElement != null) {
            StringBuilder sb = new StringBuilder();
            NodeList nl = valueElement.getChildNodes();
            for (int i = 0; i < nl.getLength(); i++) {
                Node item = nl.item(i);
                if ((item instanceof CharacterData && !(item instanceof Comment)) || item instanceof EntityReference) {
                    sb.append(item.getNodeValue());
                }
            }
            return sb.toString().trim();
        }
        return null;
    }

    @Override
    public String getTextValueByTagName(Element e, String name) {
        Element valueElement = element(e, name);
        if (valueElement == null) {
            return null;
        } else {
            String value = getTextValue(valueElement);
            if (StringUtils.hasText(value)) {
                return value;
            } else {
                return null;
            }
        }
    }

    @Override
    public String getTextValueByTagName(Element e, String name, String defaultValue) {
        String value = getTextValueByTagName(e, name);
        return StringUtils.hasText(value) ? value : defaultValue;
    }
}
