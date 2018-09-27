package test.component.impl;

import test.component.FieldInject;
import test.component.AddService;
import com.firefly.annotation.Component;
import com.firefly.annotation.Inject;

@Component("fieldInject")
public class FieldInjectImpl implements FieldInject {

    @Inject
    private AddService addService;
    @Inject("addService")
    private AddService addService2;

    @Override
    public int add(int x, int y) {
        return addService.add(x, y);
    }

    @Override
    public int add2(int x, int y) {
        return addService2.add(x, y);
    }

}
