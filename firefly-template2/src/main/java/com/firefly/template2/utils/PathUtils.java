package com.firefly.template2.utils;

import java.io.File;

/**
 * @author Pengtao Qiu
 */
abstract public class PathUtils {

    public static String removeTheLastPathSeparator(String path) {
        if (path.length() > 1) {
            if (File.separatorChar == path.charAt(path.length() - 1)) {
                return path.substring(0, path.length() - 1);
            } else {
                return path;
            }
        } else {
            return path;
        }
    }

}
