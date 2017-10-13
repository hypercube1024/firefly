package com.firefly.example.ioc;

import com.firefly.annotation.Component;
import com.firefly.annotation.Inject;

/**
 * @author Pengtao Qiu
 */
@Component
public class FooServiceImpl implements FooService {

    @Inject
    private HelloService helloService;

    @Override
    public void say(String message) {
        System.out.println(message);
        helloService.print();
    }
}
