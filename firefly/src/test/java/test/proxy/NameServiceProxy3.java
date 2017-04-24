package test.proxy;

import com.firefly.annotation.Component;
import com.firefly.annotation.Inject;
import com.firefly.utils.ReflectUtils;
import com.firefly.utils.classproxy.ClassProxy;

/**
 * @author Pengtao Qiu
 */
@Component("nameServiceProxy3")
public class NameServiceProxy3 implements ClassProxy {

    @Inject
    private SexService sexService;

    @Override
    public Object intercept(ReflectUtils.MethodProxy handler, Object originalInstance, Object[] args) {
        String id = (String) args[0];
        System.out.println("enter proxy3, id: " + id);
        return handler.invoke(originalInstance, "(" + sexService.getSex() + "->p3," + id + ",p3)");
    }
}
