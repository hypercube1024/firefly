package com.firefly.utils.dom;

import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public interface Dom {

    /**
     * Get the XML document
     *
     * @param file The file relative path
     * @return XML document
     */
    public Document getDocument(String file);

    /**
     * Get the root node
     *
     * @param doc XML document object;
     * @return The root element.
     */
    public Element getRoot(Document doc);

    /**
     * Get the children elements
     *
     * @param e A current XML element
     * @return All children elements
     */
    public List<Element> elements(Element e);

    /**
     * Get the children elements by element name
     *
     * @param e    A current XML element
     * @param name Element name
     * @return Children elements
     */
    public List<Element> elements(Element e, String name);

    /**
     * Get a element by name
     *
     * @param e    A current XML element
     * @param name The child node name
     * @return A XML element
     */
    public Element element(Element e, String name);

    /**
     * Get the value of a XML element
     *
     * @param valueElement The value node
     * @return The text value
     */
    public String getTextValue(Element valueElement);

    /**
     * Get the value of a XML node
     *
     * @param e    Current XML element
     * @param name The child node name
     * @return The text value
     */
    public String getTextValueByTagName(Element e, String name);

    /**
     * Get the value of a XML node
     *
     * @param e            Current XML element
     * @param name         The child node name
     * @param defaultValue Default text value, when the child node is not found or the
     *                     child node has not text value, the method return it
     * @return The text value
     */
    public String getTextValueByTagName(Element e, String name, String defaultValue);
}
