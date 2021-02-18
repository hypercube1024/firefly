package com.fireflysource.common.bytecode;

import com.fireflysource.common.sys.JavaVersion;
import javassist.CtClass;

public class JavassistUtils {

    public static Class<?> getClass(CtClass cc) throws Exception {
        Class<?> clazz;
        if (JavaVersion.VERSION.getPlatform() < 9) {
            clazz = cc.toClass(Thread.currentThread().getContextClassLoader(), null);
        } else {
            clazz = cc.toClass(JavassistUtils.class);
        }
        return clazz;
    }
}
