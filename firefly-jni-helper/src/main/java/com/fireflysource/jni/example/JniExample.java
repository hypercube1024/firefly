package com.fireflysource.jni.example;

import com.fireflysource.common.jni.JniLibLoader;

public class JniExample {

    static {
        JniLibLoader.load("jni_helper_example");
    }

    public static native String sayHello(String s);
}
