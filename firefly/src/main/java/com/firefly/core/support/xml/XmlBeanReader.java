package com.firefly.core.support.xml;

import static com.firefly.core.support.xml.parse.XmlNodeConstants.*;
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
 * 读取Xml文件
 *
 * @author 须俊杰, alvinqiu
 */
public class XmlBeanReader extends AbstractBeanReader {

	public XmlBeanReader() {
		this(null);
	}

	public XmlBeanReader(String file) {
		beanDefinitions = new ArrayList<BeanDefinition>();
		Dom dom = new DefaultDom();
		Set<String> errorMemo = new HashSet<String>(); // 判断循环引用

		// 得到所有bean节点
		List<Element> beansList = new ArrayList<Element>();

		parseXml(dom, file, beansList, errorMemo);

		// 迭代beans列表
		if (beansList != null) {
			for (Element ele : beansList) {
				beanDefinitions.add((BeanDefinition) XmlNodeStateMachine
						.stateProcessor(ele, dom));
			}
		}
	}

	private void parseXml(Dom dom, String file, List<Element> beansList,
			Set<String> errorMemo) {
		// 获得Xml文档对象
		Document doc = dom.getDocument(file == null ? "firefly.xml" : file);
		// 得到根节点
		Element root = dom.getRoot(doc);
		// 得到所有bean节点
		List<Element> list = dom.elements(root, BEAN_ELEMENT);
		// 得到所有import节点
		List<Element> importList = dom.elements(root, IMPORT_ELEMENT);
		if (importList != null) {
			for (Element ele : importList) {
				if (ele.hasAttribute("resource")) {
					String resource = ele.getAttribute("resource");
					if (errorMemo.contains(resource)) {
						error("disallow cyclic references between xml-file");
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
