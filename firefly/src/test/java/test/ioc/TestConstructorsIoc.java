package test.ioc;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.firefly.core.ApplicationContext;
import com.firefly.core.XmlApplicationContext;
import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;

public class TestConstructorsIoc {
	
	private static Log log = LogFactory.getInstance().getLog("firefly-system");
	public static ApplicationContext applicationContext = new XmlApplicationContext("mixed-config.xml");
	
	public static class BeanTest {

		private String test1;
		private Integer test2;
	
		public BeanTest() {
		}
		
		public BeanTest(String test1) {
			super();
			this.test1 = test1;
		}
		
		public BeanTest(String test1, Integer test2) {
			super();
			this.test1 = test1;
			this.test2 = test2;
		}
	
		public BeanTest(Integer test2) {
			super();
			this.test2 = test2;
		}
	
	
		public String getTest1() {
			return test1;
		}
	
		public void setTest1(String test1) {
			this.test1 = test1;
		}
	
		public Integer getTest2() {
			return test2;
		}
	
		public void setTest2(Integer test2) {
			this.test2 = test2;
		}
	
	}

	public static void main(String[] args) throws Throwable {
		Object obj = new BeanTest();
		Constructor<?>[] constructors = obj.getClass().getConstructors();
		List<Constructor<?>> list = Arrays.asList(constructors);
		Collections.reverse(list);
		
		for (Constructor<?> constructor : list) {
			System.out.println(Arrays.toString(constructor.getParameterTypes()));
		}
		
		System.out.println(obj.getClass().getConstructor(new Class<?>[0]).getParameters().length);
		System.out.println(list.getClass().getName());
		
		BeanTest t = (BeanTest)obj.getClass().getConstructor(String.class, Integer.class).newInstance("ssss");
		System.out.println(t.getTest1());
	}
}
