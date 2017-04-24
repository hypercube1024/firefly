package test.proxy;

import com.firefly.annotation.Inject;
import com.firefly.utils.ReflectUtils;
import com.firefly.utils.classproxy.ClassProxy;

/**
 * @author Pengtao Qiu
 */
public class NameServiceProxy4 implements ClassProxy {

    @Inject
    private FuckService fuckService;

    @Override
    public Object intercept(ReflectUtils.MethodProxy handler, Object originalInstance, Object[] args) {
        String id = (String) args[0];
        System.out.println("enter proxy4, id: " + id);
        return handler.invoke(originalInstance, "(p4->" + fuckService.fuck() + id + ",p4->" + fuckService.fuck() + ")");
    }
}
