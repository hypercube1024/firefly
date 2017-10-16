package com.firefly.example.ioc;

import com.firefly.annotation.Component;
import com.firefly.utils.ReflectUtils;
import com.firefly.utils.classproxy.ClassProxy;

/**
 * @author Pengtao Qiu
 */
@Component("logProxy")
public class LogProxy implements ClassProxy {

    @Override
    public Object intercept(ReflectUtils.MethodProxy handler, Object originalInstance, Object[] args) {
        System.out.println("entry " + handler.method().getName());
        Object ret = handler.invoke(originalInstance, args);
        System.out.println("exit " + handler.method().getName());
        return ret;
    }
}
