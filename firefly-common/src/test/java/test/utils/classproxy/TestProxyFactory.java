package test.utils.classproxy;

import com.firefly.utils.ReflectUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.lang.reflect.Field;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

/**
 * @author Pengtao Qiu
 */
@RunWith(Parameterized.class)
public class TestProxyFactory extends AbstractProxyFactoryTest {

    @Test
    public void testProxyMethod() throws Throwable {
        Foo foo = new Foo();
        ReflectUtils.MethodProxy proxy = r.proxyFactory.getMethodProxy(Foo.class.getMethod("setProperty", String.class, boolean.class));
        Assert.assertThat(proxy.invoke(foo, "proxy foo", true), nullValue());
        Assert.assertThat(foo.getName(), is("proxy foo"));
        Assert.assertThat(foo.isFailure(), is(true));

        proxy = r.proxyFactory.getMethodProxy(ReflectUtils.getGetterMethod(Foo.class, "name"));
        Assert.assertThat(proxy.invoke(foo), is("proxy foo"));

        proxy = r.proxyFactory.getMethodProxy(ReflectUtils.getGetterMethod(Foo.class, "failure"));
        Assert.assertThat(proxy.invoke(foo), is(true));

        proxy = r.proxyFactory.getMethodProxy(ReflectUtils.getSetterMethod(Foo.class, "price"));
        Assert.assertThat(proxy.invoke(foo, 35.5), nullValue());
        Assert.assertThat(foo.getPrice(), is(35.5));
    }

    @Test
    public void testArray() throws Throwable {
        int[] intArr = new int[5];
        Integer[] intArr2 = new Integer[10];

        ReflectUtils.ArrayProxy intArrProxy = r.proxyFactory.getArrayProxy(intArr.getClass());
        ReflectUtils.ArrayProxy intArr2Proxy = r.proxyFactory.getArrayProxy(intArr2.getClass());

        Assert.assertThat(intArrProxy.size(intArr), is(5));
        Assert.assertThat(intArr2Proxy.size(intArr2), is(10));

        intArrProxy.set(intArr, 0, 33);
        Assert.assertThat(intArrProxy.get(intArr, 0), is(33));

        intArr2Proxy.set(intArr2, intArr2.length - 1, 55);
        Assert.assertThat(intArr2Proxy.get(intArr2, 9), is(55));

        intArrProxy.set(intArr, 1, 23);
        Assert.assertThat(intArrProxy.get(intArr, 1), is(23));

        intArr2Proxy.set(intArr2, intArr2.length - 1, 65);
        Assert.assertThat(intArr2Proxy.get(intArr2, 9), is(65));
    }

    @Test
    public void testProxyField() throws Throwable {
        Foo foo = new Foo();
        Field num2 = Foo.class.getField("num2");
        Field info = Foo.class.getField("info");

        ReflectUtils.FieldProxy proxyNum2 = r.proxyFactory.getFieldProxy(num2);
        proxyNum2.set(foo, 30);
        Assert.assertThat(proxyNum2.get(foo), is(30));

        ReflectUtils.FieldProxy proxyInfo = r.proxyFactory.getFieldProxy(info);
        proxyInfo.set(foo, "test info 0");
        Assert.assertThat(proxyInfo.get(foo), is("test info 0"));

        ReflectUtils.setProperty(foo, "name", "hello");
        Assert.assertThat(ReflectUtils.getProperty(foo, "name"), is("hello"));


        Foo foo2 = new Foo();

        proxyNum2 = r.proxyFactory.getFieldProxy(num2);
        proxyNum2.set(foo2, 303);
        Assert.assertThat(proxyNum2.get(foo2), is(303));

        proxyInfo = r.proxyFactory.getFieldProxy(info);
        proxyInfo.set(foo2, "test info 03");
        Assert.assertThat(proxyInfo.get(foo2), is("test info 03"));
    }
}
