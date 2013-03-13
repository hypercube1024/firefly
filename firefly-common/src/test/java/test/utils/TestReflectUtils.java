package test.utils;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.junit.Assert;
import org.junit.Test;

import com.firefly.utils.ReflectUtils;
import com.firefly.utils.ReflectUtils.ProxyField;
import com.firefly.utils.ReflectUtils.ProxyMethod;

public class TestReflectUtils {

	@Test
	public void testGetterAndSetterMethod() {
		Assert.assertThat(ReflectUtils.getGetterMethod(Foo.class, "name").getName(), is("getName"));
		Assert.assertThat(ReflectUtils.getGetterMethod(Foo.class, "failure").getName(), is("isFailure"));
		
		Assert.assertThat(ReflectUtils.getSetterMethod(Foo.class, "name").getName(), is("setName"));
		Assert.assertThat(ReflectUtils.getSetterMethod(Foo.class, "failure").getName(), is("setFailure"));
	}
	
	@Test
	public void testProxyMethod() throws NoSuchMethodException, SecurityException, Throwable {
		Foo foo = new Foo();
		ProxyMethod proxy = ReflectUtils.getProxyMethod(Foo.class.getMethod("setProperty", String.class, boolean.class));
		Assert.assertThat(proxy.invoke(foo, "proxy foo", true), nullValue());
		Assert.assertThat(foo.getName(), is("proxy foo"));
		Assert.assertThat(foo.isFailure(), is(true));
		
		proxy = ReflectUtils.getProxyMethod(ReflectUtils.getGetterMethod(Foo.class, "name"));
		Assert.assertThat((String)proxy.invoke(foo), is("proxy foo"));
		
		proxy = ReflectUtils.getProxyMethod(ReflectUtils.getGetterMethod(Foo.class, "failure"));
		Assert.assertThat((Boolean)proxy.invoke(foo), is(true));
		
		proxy = ReflectUtils.getProxyMethod(ReflectUtils.getSetterMethod(Foo.class, "price"));
		Assert.assertThat(proxy.invoke(foo, 35.5), nullValue());
		Assert.assertThat(foo.getPrice(), is(35.5));
	}
	
	@Test
	public void testProxyField() throws Throwable {
		Foo foo = new Foo();
		Field num2 = Foo.class.getField("num2");
		Field info = Foo.class.getField("info");
		ProxyField proxyNum2 = ReflectUtils.getProxyField(num2);
		proxyNum2.set(foo, 30);
		Assert.assertThat((Integer)proxyNum2.get(foo), is(30));
		
		ProxyField proxyInfo = ReflectUtils.getProxyField(info);
		proxyInfo.set(foo, "test info 0");
		Assert.assertThat((String)proxyInfo.get(foo), is("test info 0"));
	}
	
	@Test
	public void testGetAndSet() throws Throwable {
		Foo foo = new Foo();
		ReflectUtils.set(foo, "price", 4.44);
		ReflectUtils.set(foo, "failure", true);
		ReflectUtils.set(foo, "name", "foo hello");
		
		Assert.assertThat((Double)ReflectUtils.get(foo, "price"), is(4.44));
		Assert.assertThat((Boolean)ReflectUtils.get(foo, "failure"), is(true));
		Assert.assertThat((String)ReflectUtils.get(foo, "name"), is("foo hello"));
	}
	
	public static void main2(String[] args) throws Throwable {
		Foo foo = new Foo();
		ProxyMethod proxy = ReflectUtils.getProxyMethod(ReflectUtils.getGetterMethod(Foo.class, "failure"));
		System.out.println(proxy.invoke(foo));
		
		Field field = Foo.class.getField("num2");
		System.out.println(field.getType());
		System.out.println(field.getName());
		Field info = Foo.class.getField("info");
		ProxyField proxyInfo = ReflectUtils.getProxyField(info);
		proxyInfo.set(foo, "test info 0");
		System.out.println(proxyInfo.get(foo));
//		System.out.println(ReflectUtils.createFieldGetterMethodCode(field));
//		System.out.println(ReflectUtils.createFieldGetterMethodCode(Foo.class.getField("info")));
//		System.out.println(ReflectUtils.createFieldSetterMethodCode(field));
//		System.out.println(ReflectUtils.createFieldSetterMethodCode(Foo.class.getField("info")));
	}
	
	public static void main(String[] args) throws Throwable {		
		int times = 1000 * 1000 * 1000;
		
		Foo foo = new Foo();
		Method method = Foo.class.getMethod("setProperty", String.class, boolean.class);
		ProxyMethod proxy = ReflectUtils.getProxyMethod(method);
		
		long start = System.currentTimeMillis();
		for (int i = 0; i < times; i++) { // 反射调用
			method.invoke(foo, "method a", true);
		}
		long end = System.currentTimeMillis();
		System.out.println(foo.getName() + " invoke: " + (end - start) + "ms");
		
		start = System.currentTimeMillis();
		for (int i = 0; i < times; i++) { // 字节码注入
			proxy.invoke(foo, "method b", true);
		}
		end = System.currentTimeMillis();
		System.out.println(foo.getName() + " invoke: " + (end - start) + "ms");
		
		start = System.currentTimeMillis();
		for (int i = 0; i < times; i++) { // 直接方法调用
			foo.setProperty("method c", true);
		}
		end = System.currentTimeMillis();
		System.out.println(foo.getName() + " invoke: " + (end - start) + "ms");
	}

	public static class Foo {
		private boolean failure;
		private String name;
		private int number;
		private double price;
		
		public int num2;
		public String info;

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
