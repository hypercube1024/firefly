package com.firefly.core.support.xml.parse;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.firefly.core.support.xml.ManagedRef;
import com.firefly.core.support.xml.ManagedValue;
import com.firefly.core.support.xml.XmlBeanDefinition;
import com.firefly.core.support.xml.XmlBeanReader;
import com.firefly.core.support.xml.XmlGenericBeanDefinition;
import com.firefly.core.support.xml.XmlManagedNode;
import com.firefly.utils.ReflectUtils;
import com.firefly.utils.StringUtils;
import com.firefly.utils.VerifyUtils;
import com.firefly.utils.dom.Dom;

import static com.firefly.core.support.xml.parse.XmlNodeConstants.*;

public class BeanNodeParser extends AbstractXmlNodeParser implements XmlNodeParser {

	@Override
	public Object parse(Element ele, Dom dom) {
		// gets basic attribute
		String id = ele.getAttribute(ID_ATTRIBUTE);
		String className = ele.getAttribute(CLASS_ATTRIBUTE);

		XmlBeanDefinition xmlBeanDefinition = new XmlGenericBeanDefinition();
		xmlBeanDefinition.setId(id);
		xmlBeanDefinition.setClassName(className);
		
		Class<?> clazz = null;
		log.info("classes [{}]", className);
		try {
			clazz = XmlBeanReader.class.getClassLoader().loadClass(className);
		} catch (Throwable e) {
			error("loads class \"" + className + "\" error");
		}

		String initMethod = ele.getAttribute(INIT_METHOD_ATTRIBUTE);
		if(StringUtils.hasText(initMethod)) {
			try {
				Method method = clazz.getMethod(initMethod);
				xmlBeanDefinition.setInitMethod(method);
			} catch (NoSuchMethodException | SecurityException e) {
				error("the initial method " + initMethod + " not found");
			}
		}

		String destroyedMethod = ele.getAttribute(DESTORY_METHOD_ATTRIBUTE);
		if(StringUtils.hasText(destroyedMethod)) {
			try {
				Method method = clazz.getMethod(destroyedMethod);
				xmlBeanDefinition.setDestroyedMethod(method);
			} catch (NoSuchMethodException | SecurityException e) {
				error("the initial method " + initMethod + " not found");
			}
		}
		
		// gets bean's constructor
		List<Element> constructors = dom.elements(ele, CONTRUCTOR_ELEMENT);
		if(constructors != null && constructors.size() > 0) {
			Element constructorElement = constructors.get(0);
			List<Element> arguments = dom.elements(constructorElement, ARGUMENT_ELEMENT);
			if(arguments != null && arguments.size() >= 1) {
				List<Class<?>> argsClass = new ArrayList<Class<?>>();
				for(Element argument : arguments) {
					XmlManagedNode xmlManagedNode = parseXmlManagedNode(argument, dom);
					if(xmlManagedNode != null) {
						xmlBeanDefinition.getContructorParameters().add(xmlManagedNode);
					}
					String initArgsType = argument.getAttribute(TYPE_ATTRIBUTE);
					if(VerifyUtils.isNotEmpty(initArgsType)) {
						try {
							argsClass.add(XmlBeanReader.class.getClassLoader().loadClass(initArgsType));
						} catch (ClassNotFoundException e) {
							error("The '" + initArgsType + "' not found");
						}
					} else {
						error("The '" + className + "' constructor argument node MUST has type attribute");
					}
				}
				try {
					xmlBeanDefinition.setConstructor(clazz.getConstructor(argsClass.toArray(new Class<?>[0])));
				} catch (Throwable e) {
					error("The '" + className + "' gets constructor error");
				}
			} else {
				error("The '" + className + "' constructor node MUST be more than one argument node!");
			}
		} else {
			try {
				xmlBeanDefinition.setConstructor(clazz.getConstructor(new Class<?>[0]));
			} catch (Throwable e) {
				error("The '" + className + "' gets constructor error");
			}
		}

		// gets all interface name
		String[] names = ReflectUtils.getInterfaceNames(clazz);
		xmlBeanDefinition.setInterfaceNames(names);
		log.debug("class [{}] names size [{}]", className, names.length);

		// gets all properties
		List<Element> properties = dom.elements(ele, PROPERTY_ELEMENT);
		if (properties != null) {
			for (Element property : properties) {
				String name = property.getAttribute(NAME_ATTRIBUTE);
				XmlManagedNode xmlManagedNode = parseXmlManagedNode(property, dom);
				if(xmlManagedNode != null && VerifyUtils.isNotEmpty(name)) {
					xmlBeanDefinition.getProperties().put(name, xmlManagedNode);
				}
			}
		}
		return xmlBeanDefinition;
	}
	
	private XmlManagedNode parseXmlManagedNode(Element property, Dom dom) {
		boolean hasValueAttribute = property.hasAttribute(VALUE_ATTRIBUTE);
		boolean hasRefAttribute = property.hasAttribute(REF_ATTRIBUTE);

		// element types choose one of ref, value, list, etc.
		NodeList nl = property.getChildNodes();
		Element subElement = null;
		for (int i = 0; i < nl.getLength(); ++i) {
			Node node = nl.item(i);
			if (node instanceof Element) {
				if (subElement != null) {
					error("This element must not contain more than one sub-element");
				} else {
					subElement = (Element) node;
				}
			}
		}

		if (hasValueAttribute && hasRefAttribute || 
				((hasValueAttribute || hasRefAttribute) && subElement != null)) {
			error("This element is only allowed to contain either 'ref' attribute OR 'value' attribute OR sub-element");
		}

		if (hasValueAttribute) {
			// literal value
			String value = property.getAttribute(VALUE_ATTRIBUTE);
			if (!StringUtils.hasText(value)) {
				error("This element contains empty 'value' attribute");
			}
			return new ManagedValue(value);
		} else if (hasRefAttribute) {
			// bean reference
			String ref = property.getAttribute(REF_ATTRIBUTE);
			if (!StringUtils.hasText(ref)) {
				error("This element contains empty 'ref' attribute");
			}
			return new ManagedRef(ref);
		} else if (subElement != null) {
			// sub-elements
			return (XmlManagedNode)XmlNodeStateMachine.stateProcessor(subElement, dom);
		} else {
			error("This element must specify a ref or value");
			return null;
		}
	}
}
