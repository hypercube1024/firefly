package com.firefly.example.ioc;

import com.firefly.annotation.Component;
import com.firefly.utils.ReflectUtils.MethodProxy;
import com.firefly.utils.classproxy.ClassProxy;

/**
 * @author Pengtao Qiu
 */
@Component("authenticationProxy")
public class AuthenticationProxy implements ClassProxy {

    @Override
    public Object intercept(MethodProxy handler, Object originalInstance, Object[] args) {
        System.out.println("authentication start " + handler.method().getName());
        Object ret = handler.invoke(originalInstance, args);
        System.out.println("authentication exit " + handler.method().getName());
        return ret;
    }
}
