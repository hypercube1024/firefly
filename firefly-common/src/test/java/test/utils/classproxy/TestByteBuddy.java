package test.utils.classproxy;

import com.firefly.utils.ReflectUtils;
import com.firefly.utils.classproxy.AbstractProxyFactory;
import com.firefly.utils.exception.CommonRuntimeException;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.ClassFileVersion;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.implementation.FixedValue;
import net.bytebuddy.implementation.InvocationHandlerAdapter;

import java.lang.reflect.Method;
import java.util.UUID;

/**
 * @author Pengtao Qiu
 */
public class TestByteBuddy {

    public static class Bar {
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    public static Object invoke(Object name, Object... arg) {
        return "test: " + arg[0];
    }

    public static ReflectUtils.MethodProxy _getMethodProxy(Method method) {
        String packageName = "com.firefly.utils";
        String className = "ByteBuddyMethodReflectionProxy" + UUID.randomUUID().toString().replace("-", "");
        String completeClassName = packageName + "." + className;

        try {
            ByteBuddy b = new ByteBuddy(ClassFileVersion.JAVA_V8);
            return b.subclass(ReflectUtils.MethodProxy.class)
                    .name(completeClassName)
                    .defineMethod("method", Method.class, Visibility.PUBLIC)
                    .intercept(FixedValue.value(method))
                    .defineMethod("invoke", Object.class, Visibility.PUBLIC)
                    .withParameters(Object.class, Object[].class)
//                    .intercept(MethodDelegation.to(TestByteBuddy.class))
                    .intercept(InvocationHandlerAdapter.of((proxy, m, args) -> {
                        Object instance = args[0];
                        Object[] p = (Object[]) args[1];
                        if (p.length > 0) {
                            return method.invoke(instance, p[0]);
                        } else {
                            return method.invoke(instance);
                        }
                    }))
                    .make()
                    .load(AbstractProxyFactory.classLoader)
                    .getLoaded().newInstance();
        } catch (Exception e) {
            throw new CommonRuntimeException(e);
        }
    }

    public static void main(String[] args) {
        Bar bar = new Bar();

        ReflectUtils.MethodProxy setter = _getMethodProxy(ReflectUtils.getSetterMethod(Bar.class, "name"));
        ReflectUtils.MethodProxy getter = _getMethodProxy(ReflectUtils.getGetterMethod(Bar.class, "name"));
        System.out.println(setter.method());
        System.out.println(getter.method());
        System.out.println(setter.invoke(bar, "hello world"));
        System.out.println(getter.invoke(bar));
    }
}
