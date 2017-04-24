package test.proxy;

import com.firefly.annotation.Component;
import com.firefly.utils.ReflectUtils;
import com.firefly.utils.classproxy.ClassProxy;

/**
 * @author Pengtao Qiu
 */
@Component("nameServiceProxy1")
public class NameServiceProxy1 implements ClassProxy {
    @Override
    public Object intercept(ReflectUtils.MethodProxy handler, Object originalInstance, Object[] args) {
        String id = (String) args[0];
        System.out.println("enter proxy1, id: " + id);
        return handler.invoke(originalInstance, "(p1," + id + ",p1)");
    }
}
