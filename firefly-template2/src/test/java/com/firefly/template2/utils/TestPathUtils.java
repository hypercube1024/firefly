package com.firefly.template2.utils;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;

import static org.hamcrest.Matchers.is;

/**
 * @author Pengtao Qiu
 */
public class TestPathUtils {

    @Test
    public void test() {
        String path = "/hello/world/";
        System.out.println(File.separatorChar);
        Assert.assertThat(PathUtils.removeTheLastPathSeparator(path), is("/hello/world"));

        path = "/hello";
        Assert.assertThat(PathUtils.removeTheLastPathSeparator(path), is("/hello"));

        path = "/";
        Assert.assertThat(PathUtils.removeTheLastPathSeparator(path), is("/"));

        path = "_";
        Assert.assertThat(PathUtils.removeTheLastPathSeparator(path), is("_"));
    }
}
