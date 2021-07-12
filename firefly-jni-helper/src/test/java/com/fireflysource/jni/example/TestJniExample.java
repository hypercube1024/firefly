package com.fireflysource.jni.example;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestJniExample {

    @Test
    @EnabledOnOs({OS.MAC})
    void testSayHello() {
        String result = JniExample.sayHello("欢迎！");
        System.out.println(result);
        assertEquals("Bonjour, 欢迎！", result);
    }
}
