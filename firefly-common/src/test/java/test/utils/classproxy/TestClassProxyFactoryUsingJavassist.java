package test.utils.classproxy;

import static org.hamcrest.Matchers.is;

import java.lang.reflect.Method;

import org.junit.Assert;
import org.junit.Test;

import com.firefly.utils.ReflectUtils.MethodProxy;
import com.firefly.utils.classproxy.ClassProxy;
import com.firefly.utils.classproxy.ClassProxyFactoryUsingJavassist;
import com.firefly.utils.classproxy.MethodFilter;

public class TestClassProxyFactoryUsingJavassist {

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
	
	@Test
	public void test() throws Throwable {
		Fee origin = new Fee();
		
		Fee fee = (Fee)ClassProxyFactoryUsingJavassist.INSTANCE.createProxy(origin, new ClassProxy(){

			@Override
			public Object intercept(MethodProxy handler, Object originalInstance, Object[] args) {
				System.out.println("intercept method 1: " + handler.method().getName() + "|" + originalInstance.getClass().getCanonicalName());
				if(handler.method().getName().equals("testInt")) {
					args[0] = 1;
				}
				Object ret = handler.invoke(originalInstance, args);
				System.out.println("intercept method 1 end...");
				if(handler.method().getName().equals("hello"))  {
					ret = ret + " intercept 1";
				}
				return ret;
			}}, null);
		System.out.println(fee.getClass().getCanonicalName());
		Assert.assertThat(fee.hello(), is("hello fee intercept 1"));
		Assert.assertThat(fee.testInt(25), is(1));
		
		Fee fee2 = (Fee)ClassProxyFactoryUsingJavassist.INSTANCE.createProxy(fee, new ClassProxy(){

			@Override
			public Object intercept(MethodProxy handler, Object originalInstance, Object[] args) {
				System.out.println("intercept method 2: " + handler.method().getName()  + "|" + originalInstance.getClass().getCanonicalName());
				if(handler.method().getName().equals("testInt")) {
					args[0] = 2;
				}
				Object ret = handler.invoke(originalInstance, args);
				System.out.println("intercept method 2 end...");
				
				if(handler.method().getName().equals("hello"))  {
					ret = ret + " intercept 2";
				}
				return ret;
			}}, null);
		System.out.println(fee2.getClass().getCanonicalName());
		Assert.assertThat(fee2.hello(), is("hello fee intercept 1 intercept 2"));
		Assert.assertThat(fee.testInt(25), is(1));
	}
	
	@Test
	public void testFilter() throws Throwable {
		Fee origin = new Fee();
		
		Fee fee = (Fee)ClassProxyFactoryUsingJavassist.INSTANCE.createProxy(origin, new ClassProxy(){

			@Override
			public Object intercept(MethodProxy handler, Object originalInstance, Object[] args) {
				System.out.println("filter method 1: " + handler.method().getName() + "|" + originalInstance.getClass().getCanonicalName());
				if(handler.method().getName().equals("testInt")) {
					args[0] = 1;
				}
				Object ret = handler.invoke(originalInstance, args);
				System.out.println("filter method 1 end...");
				if(handler.method().getName().equals("hello"))  {
					ret = ret + " filter 1";
				}
				return ret;
			}}, new MethodFilter(){

				@Override
				public boolean accept(Method method) {
					return !method.getName().equals("testInt");
				}});
		System.out.println(fee.getClass().getCanonicalName());
		Assert.assertThat(fee.hello(), is("hello fee filter 1"));
		Assert.assertThat(fee.testInt(25), is(25));
	}
	
	public static void main(String[] args) throws Throwable {
		new TestClassProxyFactoryUsingJavassist().test();
	}
}
