package com.fireflysource.common.bytecode;


import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.reflect.InvocationTargetException;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class TestClassProxy {

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

    public static class NonJavaBean {
        String hello;

        public NonJavaBean(String hello) {
            this.hello = hello;
        }

        public String getHello() {
            return hello;
        }

        public void setHello(String hello) {
            this.hello = hello;
        }
    }

    static Stream<Arguments> parametersProvider() {
        return Stream.of(arguments(JavassistClassProxyFactory.INSTANCE));
    }

    @ParameterizedTest
    @MethodSource("parametersProvider")
    void test(ClassProxyFactory classProxyFactory) throws Throwable {
        Fee origin = new Fee();

        Fee fee = classProxyFactory.createProxy(origin,
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
        assertEquals("hello fee intercept 1", fee.hello());
        assertEquals(1, fee.testInt(25));

        Fee fee2 = classProxyFactory.createProxy(fee,
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
        assertEquals("hello fee intercept 1 intercept 2", fee2.hello());
        assertEquals(1, fee.testInt(25));
    }

    @ParameterizedTest
    @MethodSource("parametersProvider")
    void testFilter(ClassProxyFactory classProxyFactory) throws Throwable {
        Fee origin = new Fee();

        Fee fee = classProxyFactory.createProxy(origin,
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
        assertEquals("hello fee filter 1", fee.hello());
        assertEquals(25, fee.testInt(25));
    }

    @ParameterizedTest
    @MethodSource("parametersProvider")
    void testNonJavaBean(ClassProxyFactory classProxyFactory) {
        assertThrows(InvocationTargetException.class, () -> {
            NonJavaBean x = new NonJavaBean("test");
            NonJavaBean y = classProxyFactory.createProxy(x,
                    (handler, originalInstance, args) -> "no java bean",
                    method -> method.getName().equals("getHello"));
            System.out.println(y.getHello());
        });
    }

}
