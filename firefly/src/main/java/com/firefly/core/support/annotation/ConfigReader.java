package com.firefly.core.support.annotation;

import com.firefly.utils.VerifyUtils;
import com.firefly.utils.dom.DefaultDom;
import com.firefly.utils.dom.Dom;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.List;

public class ConfigReader {

    public static final String DEFAULT_CONFIG_FILE = "firefly.xml";
    public static final String SCAN_ELEMENT = "component-scan";
    public static final String PACKAGE_ATTRIBUTE = "base-package";

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
        Document doc = dom.getDocument(file == null ? DEFAULT_CONFIG_FILE : file);
        Element root = dom.getRoot(doc);
        load(root, dom);
        return config;
    }

    public Config load(Element root, Dom dom) {
        List<Element> scanList = dom.elements(root, SCAN_ELEMENT);

        if (scanList != null) {
            List<String> paths = new ArrayList<>();
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
        return config;
    }

    public Config getConfig() {
        return config;
    }
}
