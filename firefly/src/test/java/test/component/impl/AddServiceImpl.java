package test.component.impl;

import test.component.AddService;
import com.firefly.annotation.Component;

@Component("addService")
public class AddServiceImpl implements AddService {
    private int i = 0;

    @Override
    public int add(int x, int y) {
        return x + y;
    }

    @Override
    public int getI() {
        return i++;
    }

}
