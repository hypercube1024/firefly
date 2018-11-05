package com.firefly.utils;

import com.firefly.utils.dom.DefaultDom;

import java.io.InputStream;
import java.util.Properties;

/**
 * @author Pengtao Qiu
 */
public class ProjectVersion {

    private String value;

    private static class Holder {
        private static final ProjectVersion instance = new ProjectVersion();
    }

    private ProjectVersion() {
        try (InputStream is = DefaultDom.class.getResourceAsStream("/firefly_version.properties")) {
            Properties properties = new Properties();
            properties.load(is);
            value = properties.getProperty("firefly.version");
        } catch (Exception ignored) {
        }
    }

    public static String getValue() {
        return Holder.instance.value;
    }
}
