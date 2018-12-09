package com.fireflysource.common.sys;

import java.io.InputStream;
import java.util.Properties;

/**
 * @author Pengtao Qiu
 */
public class ProjectVersion {

    private String value;

    private ProjectVersion() {
        try (InputStream is = ProjectVersion.class.getResourceAsStream("/firefly_version.properties")) {
            Properties properties = new Properties();
            properties.load(is);
            value = properties.getProperty("firefly.version");
        } catch (Exception ignored) {
        }
    }

    public static String getValue() {
        return Holder.instance.value;
    }

    public static String getAsciiArt() {
        return "\033[31;0m\n" +
                "______ _           __ _       \n" +
                "|  ___(_)         / _| |      \n" +
                "| |_   _ _ __ ___| |_| |_   _ \n" +
                "|  _| | | '__/ _ \\  _| | | | |\n" +
                "| |   | | | |  __/ | | | |_| |\n" +
                "\\_|   |_|_|  \\___|_| |_|\\__, |\n" +
                "                         __/ |\n" +
                "                        |___/ \n\033[0m";
    }

    private static class Holder {
        private static final ProjectVersion instance = new ProjectVersion();
    }
}
