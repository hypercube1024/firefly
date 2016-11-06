package test.server.service.impl;

import test.server.service.HelloWorldService;

/**
 * Created by Pengtao Qiu on 2016/11/5.
 */
public class HelloWorldServiceImpl implements HelloWorldService {
    @Override
    public String sayHello() {
        return "say hello world";
    }

    public void init() {
        System.out.println("initialize hello service " + this.toString());
    }

    public void destroy() {
        System.out.println("destroy hello services " + this.toString());
    }
}
