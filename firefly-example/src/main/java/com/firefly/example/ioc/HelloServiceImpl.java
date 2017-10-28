package com.firefly.example.ioc;

import com.firefly.annotation.DestroyedMethod;
import com.firefly.annotation.InitialMethod;

/**
 * @author Pengtao Qiu
 */
@ProxyChain
public class HelloServiceImpl implements HelloService {

    private String message;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public void print() {
        System.out.println(message);
    }

    @InitialMethod
    public void init() {
        System.out.println("init HelloService");
    }

    @DestroyedMethod
    public void destroy() {
        System.out.println("destroy HelloService");
    }
}
