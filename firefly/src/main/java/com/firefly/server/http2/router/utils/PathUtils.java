package com.firefly.server.http2.router.utils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Pengtao Qiu
 */
abstract public class PathUtils {

    public static List<String> split(String path) {
        List<String> ret = new ArrayList<>();
        int start = 1;
        int max = path.length() - 1;

        for (int i = 1; i <= max; i++) {
            if (path.charAt(i) == '/') {
                ret.add(path.substring(start, i).trim());
                start = i + 1;
            }
        }

        if (path.charAt(max) != '/') {
            ret.add(path.substring(start).trim());
        }
        return ret;
    }

}
