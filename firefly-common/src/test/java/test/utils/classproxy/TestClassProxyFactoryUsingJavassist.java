package test.utils.classproxy;

import com.firefly.utils.classproxy.ClassProxyFactory;
import com.firefly.utils.classproxy.ClassProxyFactoryUsingJavassist;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.hamcrest.Matchers.is;

@RunWith(Parameterized.class)
public class TestClassProxyFactoryUsingJavassist {

    @Parameterized.Parameter
    public Run r;

    public static class Fee {
        protected void testProtected() {

        }

        public void testVoid(String str, Long l) {

        }

        public int testInt(int i) {
            return i;
        }

        public Void testParameters(String str, int i, Long l) {
            return null;
        }

        public String hello() {
            return "hello fee";
        }
    }

    static class Run {
        ClassProxyFactory classProxyFactory;
        String name;

        @Override
        public String toString() {
            return name;
        }
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Run> data() {
        List<Run> list = new ArrayList<>();
        Run run = new Run();
        run.classProxyFactory = ClassProxyFactoryUsingJavassist.INSTANCE;
        run.name = "javassist proxy factory";
        list.add(run);
        return list;
    }

    @Test
    public void test() throws Throwable {
        Fee origin = new Fee();

        Fee fee = (Fee) r.classProxyFactory.createProxy(origin,
                (handler, originalInstance, args) -> {
                    System.out.println("intercept method 1: " + handler.method().getName() + "|" + originalInstance.getClass().getCanonicalName());
                    if (handler.method().getName().equals("testInt")) {
                        args[0] = 1;
                    }
                    Object ret = handler.invoke(originalInstance, args);
                    System.out.println("intercept method 1 end...");
                    if (handler.method().getName().equals("hello")) {
                        ret = ret + " intercept 1";
                    }
                    return ret;
                }, null);
        System.out.println(fee.getClass().getCanonicalName());
        Assert.assertThat(fee.hello(), is("hello fee intercept 1"));
        Assert.assertThat(fee.testInt(25), is(1));

        Fee fee2 = (Fee) r.classProxyFactory.createProxy(fee,
                (handler, originalInstance, args) -> {
                    System.out.println("intercept method 2: " + handler.method().getName() + "|" + originalInstance.getClass().getCanonicalName());
                    if (handler.method().getName().equals("testInt")) {
                        args[0] = 2;
                    }
                    Object ret = handler.invoke(originalInstance, args);
                    System.out.println("intercept method 2 end...");

                    if (handler.method().getName().equals("hello")) {
                        ret = ret + " intercept 2";
                    }
                    return ret;
                }, null);
        System.out.println(fee2.getClass().getCanonicalName());
        Assert.assertThat(fee2.hello(), is("hello fee intercept 1 intercept 2"));
        Assert.assertThat(fee.testInt(25), is(1));
    }

    @Test
    public void testFilter() throws Throwable {
        Fee origin = new Fee();

        Fee fee = (Fee) r.classProxyFactory.createProxy(origin,
                (handler, originalInstance, args) -> {
                    System.out.println("filter method 1: " + handler.method().getName() + "|" + originalInstance.getClass().getCanonicalName());
                    if (handler.method().getName().equals("testInt")) {
                        args[0] = 1;
                    }
                    Object ret = handler.invoke(originalInstance, args);
                    System.out.println("filter method 1 end...");
                    if (handler.method().getName().equals("hello")) {
                        ret = ret + " filter 1";
                    }
                    return ret;
                }, method -> !method.getName().equals("testInt"));
        System.out.println(fee.getClass().getCanonicalName());
        Assert.assertThat(fee.hello(), is("hello fee filter 1"));
        Assert.assertThat(fee.testInt(25), is(25));
    }

//    public static void main(String[] args) throws Throwable {
//        new TestClassProxyFactoryUsingJavassist().test();
//    }
}
