package com.fireflysource.common.reflection;


import org.junit.jupiter.api.Test;

import static com.fireflysource.common.reflection.ReflectionUtils.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TestReflectUtils {

    @Test
    void testGetterAndSetterMethod() {
        assertEquals("getName", getGetterMethod(Foo.class, "name").getName());
        assertEquals("isFailure", getGetterMethod(Foo.class, "failure").getName());

        assertEquals("setName", getSetterMethod(Foo.class, "name").getName());
        assertEquals("setFailure", getSetterMethod(Foo.class, "failure").getName());

        assertEquals("setiPad", getSetterMethod(Foo.class, "iPad").getName());
        assertEquals("setiPhone", getSetterMethod(Foo.class, "iPhone").getName());

        assertEquals("isiPad", getGetterMethod(Foo.class, "iPad").getName());
        assertEquals("getiPhone", getGetterMethod(Foo.class, "iPhone").getName());
    }

    @Test
    void testGetAndSet() throws Throwable {
        Foo foo = new Foo();
        set(foo, "price", 4.44);
        set(foo, "failure", true);
        set(foo, "name", "foo hello");

        assertEquals(4.44, get(foo, "price"));
        assertTrue((Boolean) get(foo, "failure"));
        assertEquals("foo hello", get(foo, "name"));
    }

    @Test
    void testCopy() {
        Foo foo = new Foo();
        foo.setName("hello foo");
        foo.setPrice(3.3);
        foo.setNumber(40);

        Foo2 foo2 = new Foo2();
        foo2.setName("hello foo2");

        copy(foo2, foo);
        assertEquals("hello foo2", foo.getName());
        assertEquals(40, foo.getNumber());
    }

    public static class Foo2 {
        private String name;
        private Integer number;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Integer getNumber() {
            return number;
        }

        public void setNumber(Integer number) {
            this.number = number;
        }

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
