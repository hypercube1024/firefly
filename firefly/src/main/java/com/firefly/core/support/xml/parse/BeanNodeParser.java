package com.firefly.core.support.xml.parse;

import static com.firefly.core.support.xml.parse.XmlNodeConstants.*;
import java.util.List;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import com.firefly.core.support.xml.ManagedRef;
import com.firefly.core.support.xml.ManagedValue;
import com.firefly.core.support.xml.XmlBeanDefinition;
import com.firefly.core.support.xml.XmlBeanReader;
import com.firefly.core.support.xml.XmlGenericBeanDefinition;
import com.firefly.utils.ReflectUtils;
import com.firefly.utils.StringUtils;
import com.firefly.utils.dom.Dom;

public class BeanNodeParser extends AbstractXmlNodeParser implements XmlNodeParser {

	@Override
	public Object parse(Element ele, Dom dom) {
		// 获取基本属性
		String id = ele.getAttribute(ID_ATTRIBUTE);
		String className = ele.getAttribute(CLASS_ATTRIBUTE);
		XmlBeanDefinition xmlBeanDefinition = new XmlGenericBeanDefinition();
		xmlBeanDefinition.setId(id);
		xmlBeanDefinition.setClassName(className);

		// 实例化对象
		Class<?> clazz = null;
		Object obj = null;
		log.info("classes [{}]", className);
		try {
			clazz = XmlBeanReader.class.getClassLoader().loadClass(className);
			obj = clazz.newInstance();
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		} catch (InstantiationException e) {
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
		xmlBeanDefinition.setObject(obj);

		// 取得接口名称
		String[] names = ReflectUtils.getInterfaceNames(clazz);
		xmlBeanDefinition.setInterfaceNames(names);
		log.debug("class [{}] names size [{}]", className, names.length);

		// 获取所有property
		List<Element> properties = dom.elements(ele, PROPERTY_ELEMENT);

		// 迭代property列表
		if (properties != null) {
			for (Element property : properties) {
				String name = property.getAttribute(NAME_ATTRIBUTE);

				boolean hasValueAttribute = property
						.hasAttribute(VALUE_ATTRIBUTE);
				boolean hasRefAttribute = property.hasAttribute(REF_ATTRIBUTE);

				// 只能有一个子元素: ref, value, list, etc.
				NodeList nl = property.getChildNodes();
				Element subElement = null;
				for (int i = 0; i < nl.getLength(); ++i) {
					Node node = nl.item(i);
					if (node instanceof Element) {
						if (subElement != null) {
							error(name
									+ " must not contain more than one sub-element");
						} else {
							subElement = (Element) node;
						}
					}
				}

				if (hasValueAttribute
						&& hasRefAttribute
						|| ((hasValueAttribute || hasRefAttribute) && subElement != null)) {
					error(name
							+ " is only allowed to contain either 'ref' attribute OR 'value' attribute OR sub-element");
				}

				if (hasValueAttribute) {
					// 普通赋值
					String value = property.getAttribute(VALUE_ATTRIBUTE);
					if (!StringUtils.hasText(value)) {
						error(name + " contains empty 'value' attribute");
					}
					xmlBeanDefinition.getProperties().put(name,
							new ManagedValue(value));
				} else if (hasRefAttribute) {
					// 依赖其他bean
					String ref = property.getAttribute(REF_ATTRIBUTE);
					if (!StringUtils.hasText(ref)) {
						error(name + " contains empty 'ref' attribute");
					}
					xmlBeanDefinition.getProperties().put(name,
							new ManagedRef(ref));
				} else if (subElement != null) {
					// 处理子元素
					Object subEle = XmlNodeStateMachine.stateProcessor(subElement, dom);
					xmlBeanDefinition.getProperties().put(name, subEle);
				} else {
					error(name + " must specify a ref or value");
					return null;
				}
			}
		}
		return xmlBeanDefinition;
	}
}
