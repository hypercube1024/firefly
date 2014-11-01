package com.firefly.utils.dom;

import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public interface Dom {

	/**
	 * Get the XML document
	 * 
	 * @param file 
	 * 			The file relative path
	 * 
	 * @return XML document
	 */
	public abstract Document getDocument(String file);
	
	/**
	 * Get the root node
	 * 
	 * @param doc 
	 * 		XML document object;
	 * @return The root element.
	 */
	public abstract Element getRoot(Document doc);
	
	/**
	 * Get the children elements
	 * 
	 * @param e 
	 * 			A current XML element
	 * 
	 * @return All children elements
	 */
	public abstract List<Element> elements(Element e);
	
	/**
	 * Get the children elements by element name
	 * 
	 * @param e 
	 * 		A current XML element
	 * @param name 
	 * 		Element name
	 * 
	 * @return Children elements
	 */
	public abstract List<Element> elements(Element e, String name);
	
	/**
	 * Get a element by name
	 * 
	 * @param e 
	 * 		A current XML element
	 * @param name 
	 * 		Element name
	 * 
	 * @return A XML element
	 */
	public abstract Element element(Element e, String name);
	
	/**
	 * Get the value of a XML element
	 * 
	 * @param valueElement 
	 * 		The value node 
	 * @return The text value
	 */
	public abstract String getTextValue(Element valueElement);
}
