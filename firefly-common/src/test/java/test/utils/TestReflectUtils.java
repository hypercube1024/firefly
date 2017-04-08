package test.utils;

import com.firefly.utils.ReflectUtils;
import org.junit.Assert;
import org.junit.Test;

import static org.hamcrest.Matchers.is;

public class TestReflectUtils {

    @Test
    public void testGetterAndSetterMethod() {
        Assert.assertThat(ReflectUtils.getGetterMethod(Foo.class, "name").getName(), is("getName"));
        Assert.assertThat(ReflectUtils.getGetterMethod(Foo.class, "failure").getName(), is("isFailure"));

        Assert.assertThat(ReflectUtils.getSetterMethod(Foo.class, "name").getName(), is("setName"));
        Assert.assertThat(ReflectUtils.getSetterMethod(Foo.class, "failure").getName(), is("setFailure"));

        Assert.assertThat(ReflectUtils.getSetterMethod(Foo.class, "iPad").getName(), is("setiPad"));
        Assert.assertThat(ReflectUtils.getSetterMethod(Foo.class, "iPhone").getName(), is("setiPhone"));

        Assert.assertThat(ReflectUtils.getGetterMethod(Foo.class, "iPad").getName(), is("isiPad"));
        Assert.assertThat(ReflectUtils.getGetterMethod(Foo.class, "iPhone").getName(), is("getiPhone"));
    }

    @Test
    public void testGetAndSet() throws Throwable {
        Foo foo = new Foo();
        ReflectUtils.set(foo, "price", 4.44);
        ReflectUtils.set(foo, "failure", true);
        ReflectUtils.set(foo, "name", "foo hello");

        Assert.assertThat(ReflectUtils.get(foo, "price"), is(4.44));
        Assert.assertThat(ReflectUtils.get(foo, "failure"), is(true));
        Assert.assertThat(ReflectUtils.get(foo, "name"), is("foo hello"));
    }

    @Test
    public void testCopy() throws Throwable {
        Foo foo = new Foo();
        foo.setName("hello foo");
        foo.setPrice(3.3);
        foo.setNumber(40);

        Foo2 foo2 = new Foo2();
        foo2.setName("hello foo2");

        ReflectUtils.copy(foo2, foo);
        Assert.assertThat(foo.getName(), is("hello foo2"));
        Assert.assertThat(foo.getNumber(), is(40));
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
        private boolean failure;
        public String name;
        private int number;
        private double price;

        public int num2;
        public String info;

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
