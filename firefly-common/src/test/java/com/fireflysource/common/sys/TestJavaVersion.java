package com.fireflysource.common.sys;

import org.junit.jupiter.api.Test;

public class TestJavaVersion {

    @Test
    void test() {
        System.out.println(JavaVersion.VERSION.getVersion());
        System.out.println(JavaVersion.VERSION.getPlatform());
        System.out.println(System.getProperty("java.version"));
    }
}
