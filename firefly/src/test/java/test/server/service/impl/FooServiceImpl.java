package test.server.service.impl;

import com.firefly.annotation.Component;
import com.firefly.annotation.DestroyedMethod;
import com.firefly.annotation.InitialMethod;
import test.server.service.FooService;

/**
 * Created by Pengtao Qiu on 2016/11/6.
 */
@Component
public class FooServiceImpl implements FooService {
    @Override
    public String foo() {
        return "foo";
    }

    @InitialMethod
    public void init() {
        System.out.println("init foo service");
    }

    @DestroyedMethod
    public void destroy() {
        System.out.println("destroy foo service");
    }
}
