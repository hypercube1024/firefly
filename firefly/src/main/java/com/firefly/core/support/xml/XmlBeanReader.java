package com.firefly.core.support.xml;

import static com.firefly.core.support.xml.parse.XmlNodeConstants.BEAN_ELEMENT;
import static com.firefly.core.support.xml.parse.XmlNodeConstants.IMPORT_ELEMENT;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.firefly.core.support.AbstractBeanReader;
import com.firefly.core.support.BeanDefinition;
import com.firefly.core.support.xml.parse.XmlNodeStateMachine;
import com.firefly.utils.VerifyUtils;
import com.firefly.utils.dom.DefaultDom;
import com.firefly.utils.dom.Dom;

/**
 * Reading XML configuration file 
 *
 * @author JJ Xu, Alvin Qiu
 */
public class XmlBeanReader extends AbstractBeanReader {

	public XmlBeanReader() {
		this(null);
	}

	public XmlBeanReader(String file) {
		beanDefinitions = new ArrayList<BeanDefinition>();
		Dom dom = new DefaultDom();
		Set<String> errorMemo = new HashSet<>(); // It's used for eliminating circular references

		// all bean elements
		List<Element> beansList = new ArrayList<>();

		parseXml(dom, file, beansList, errorMemo);

		if (beansList != null) {
			for (Element ele : beansList) {
				beanDefinitions.add((BeanDefinition) XmlNodeStateMachine.stateProcessor(ele, dom));
			}
		}
	}

	private void parseXml(Dom dom, String file, List<Element> beansList, Set<String> errorMemo) {
		Document doc = dom.getDocument(file == null ? "firefly.xml" : file);
		Element root = dom.getRoot(doc);
		// all bean nodes
		List<Element> list = dom.elements(root, BEAN_ELEMENT);
		// all import nodes
		List<Element> importList = dom.elements(root, IMPORT_ELEMENT);
		if (importList != null) {
			for (Element ele : importList) {
				if (ele.hasAttribute("resource")) {
					String resource = ele.getAttribute("resource");
					if (errorMemo.contains(resource)) {
						error("disallow cyclic references between two XML files");
						return;
					} else {
						if (VerifyUtils.isEmpty(resource)) {
							error("resource cannot be null");
							return;
						} else {
							errorMemo.add(resource);
							parseXml(dom, resource, beansList, errorMemo);
						}
					}
				} else {
					error("has no resource attribute");
					return;
				}
			}
		}
		beansList.addAll(list);
	}
}
