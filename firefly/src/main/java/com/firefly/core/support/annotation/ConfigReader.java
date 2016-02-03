package com.firefly.core.support.annotation;

import java.util.LinkedList;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.firefly.utils.VerifyUtils;
import com.firefly.utils.dom.DefaultDom;
import com.firefly.utils.dom.Dom;
import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;

public class ConfigReader {
	private static Log log = LogFactory.getInstance().getLog("firefly-system");

	public static final String DEFAULT_CONFIG_FILE = "firefly.xml";
	public static final String SCAN_ELEMENT = "component-scan";
	public static final String MVC_ELEMENT = "mvc";
	public static final String PACKAGE_ATTRIBUTE = "base-package";
	public static final String VIEW_PATH_ATTRIBUTE = "view-path";
	public static final String VIEW_ENCODING_ATTRIBUTE = "view-encoding";
	public static final String VIEW_TYPE_ATTRIBUTE = "view-type";

	private Config config;

	private ConfigReader() {
		config = new Config();
	}

	private static class Holder {
		private static ConfigReader instance = new ConfigReader();
	}

	public static ConfigReader getInstance() {
		return Holder.instance;
	}

	public Config load(String file) {
		Dom dom = new DefaultDom();
		// 获得Xml文档对象
		Document doc = dom.getDocument(file == null ? DEFAULT_CONFIG_FILE : file);
		// 得到根节点
		Element root = dom.getRoot(doc);
		load(root, dom);
		return config;
	}

	public Config load(Element root, Dom dom) {
		// 得到所有scan节点
		List<Element> scanList = dom.elements(root, SCAN_ELEMENT);

		if (scanList != null) {
			List<String> paths = new LinkedList<String>();
			for (int i = 0; i < scanList.size(); i++) {
				Element ele = scanList.get(i);
				String path = ele.getAttribute(PACKAGE_ATTRIBUTE);
				if (!VerifyUtils.isEmpty(path))
					paths.add(path);
			}
			config.setPaths(paths.toArray(new String[0]));
		} else {
			config.setPaths(new String[0]);
		}

		Element mvc = dom.element(root, MVC_ELEMENT);
		if (mvc != null) {
			String viewPath = mvc.getAttribute(VIEW_PATH_ATTRIBUTE);
			String encoding = mvc.getAttribute(VIEW_ENCODING_ATTRIBUTE);
			log.info("the MVC view path is {}, the encoding is {}", viewPath, encoding);

			if (VerifyUtils.isNotEmpty(viewPath))
				config.setViewPath(viewPath);
			if (VerifyUtils.isNotEmpty(encoding))
				config.setEncoding(encoding);
		}
		return config;
	}

	public Config getConfig() {
		return config;
	}
}
