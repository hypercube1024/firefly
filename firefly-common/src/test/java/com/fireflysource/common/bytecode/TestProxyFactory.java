package com.fireflysource.common.bytecode;


import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.reflect.Field;
import java.util.stream.Stream;

import static com.fireflysource.common.reflection.ReflectionUtils.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;


/**
 * @author Pengtao Qiu
 */
public class TestProxyFactory {

    static Stream<Arguments> parametersProvider() {
        return Stream.of(
                arguments(JavaReflectionProxyFactory.INSTANCE),
                arguments(JavassistReflectionProxyFactory.INSTANCE)
        );
    }

    @ParameterizedTest
    @MethodSource("parametersProvider")
    void testProxyMethod(ProxyFactory proxyFactory) throws Throwable {
        Foo foo = new Foo();
        MethodProxy proxy = proxyFactory.getMethodProxy(Foo.class.getMethod("setProperty", String.class, boolean.class));
        assertNull(proxy.invoke(foo, "proxy foo", true));
        assertEquals("proxy foo", foo.getName());
        assertTrue(foo.isFailure());

        proxy = proxyFactory.getMethodProxy(getGetterMethod(Foo.class, "name"));
        assertEquals("proxy foo", proxy.invoke(foo));

        proxy = proxyFactory.getMethodProxy(getGetterMethod(Foo.class, "failure"));
        assertTrue((Boolean) proxy.invoke(foo));

        proxy = proxyFactory.getMethodProxy(getSetterMethod(Foo.class, "price"));
        assertNull(proxy.invoke(foo, 35.5));
        assertEquals(35.5, foo.getPrice());
    }

    @ParameterizedTest
    @MethodSource("parametersProvider")
    void testArray(ProxyFactory proxyFactory) {
        int[] intArr = new int[5];
        Integer[] intArr2 = new Integer[10];

        ArrayProxy intArrProxy = proxyFactory.getArrayProxy(intArr.getClass());
        ArrayProxy intArr2Proxy = proxyFactory.getArrayProxy(intArr2.getClass());

        assertEquals(5, intArrProxy.size(intArr));
        assertEquals(10, intArr2Proxy.size(intArr2));

        intArrProxy.set(intArr, 0, 33);
        assertEquals(33, intArrProxy.get(intArr, 0));

        intArr2Proxy.set(intArr2, intArr2.length - 1, 55);
        assertEquals(55, intArr2Proxy.get(intArr2, 9));

        intArrProxy.set(intArr, 1, 23);
        assertEquals(23, intArrProxy.get(intArr, 1));

        intArr2Proxy.set(intArr2, intArr2.length - 1, 65);
        assertEquals(65, intArr2Proxy.get(intArr2, 9));
    }

    @ParameterizedTest
    @MethodSource("parametersProvider")
    void testProxyField(ProxyFactory proxyFactory) throws Throwable {
        Foo foo = new Foo();
        Field num2 = Foo.class.getField("num2");
        Field info = Foo.class.getField("info");

        FieldProxy proxyNum2 = proxyFactory.getFieldProxy(num2);
        proxyNum2.set(foo, 30);
        assertEquals(30, proxyNum2.get(foo));

        FieldProxy proxyInfo = proxyFactory.getFieldProxy(info);
        proxyInfo.set(foo, "test info 0");
        assertEquals("test info 0", proxyInfo.get(foo));

        setProperty(foo, "name", "hello");
        assertEquals("hello", getProperty(foo, "name"));


        Foo foo2 = new Foo();

        proxyNum2 = proxyFactory.getFieldProxy(num2);
        proxyNum2.set(foo2, 303);
        assertEquals(303, proxyNum2.get(foo2));

        proxyInfo = proxyFactory.getFieldProxy(info);
        proxyInfo.set(foo2, "test info 03");
        assertEquals("test info 03", proxyInfo.get(foo2));
    }

    public static class Foo {
        public String name;
        public int num2;
        public String info;
        private boolean failure;
        private int number;
        private double price;
        private String iPhone;
        private boolean iPad;

        public String getiPhone() {
            return iPhone;
        }

        public void setiPhone(String iPhone) {
            this.iPhone = iPhone;
        }

        public boolean isiPad() {
            return iPad;
        }

        public void setiPad(boolean iPad) {
            this.iPad = iPad;
        }

        public int getNumber() {
            return number;
        }

        public void setNumber(int number) {
            this.number = number;
        }

        public double getPrice() {
            return price;
        }

        public void setPrice(double price) {
            this.price = price;
        }

        public boolean isFailure() {
            return failure;
        }

        public void setFailure(boolean failure) {
            this.failure = failure;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public void setProperty(String name, boolean failure) {
            this.name = name;
            this.failure = failure;
        }

    }
}
