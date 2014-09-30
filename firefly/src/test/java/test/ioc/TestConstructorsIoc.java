package test.ioc;

import static org.hamcrest.Matchers.is;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import test.mixed.FoodRepository;
import test.mixed.FoodService;
import test.mixed.impl.FoodConstructorTestService;
import test.mixed.impl.FoodRepositoryImpl;

import com.firefly.core.ApplicationContext;
import com.firefly.core.XmlApplicationContext;
import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;

public class TestConstructorsIoc {
	
	private static Log log = LogFactory.getInstance().getLog("firefly-system");
	public static ApplicationContext applicationContext = new XmlApplicationContext("mixed-constructor.xml");
	
	@Test
	public void testXMLInject() {
		BeanTest b = applicationContext.getBean("constructorTestBean");
		log.info(b.toString());
		Assert.assertThat(b.getTest1(), is("fffff"));
		Assert.assertThat(b.getTest2(), is(4));
	}
	
	@Test
	public void testAnnotationInject() {
		FoodConstructorTestService service = applicationContext.getBean(FoodConstructorTestService.class);
		Assert.assertThat(service.getBeanTest().getTest1(), is("fffff"));
		Assert.assertThat(service.getBeanTest().getTest2(), is(4));
		
		Assert.assertThat(service.getFoodRepository().getFood().size(), is(3));
		log.info(service.getFoodRepository().getFood().toString());
	}
	
	public static class BeanTest {

		private String test1;
		private Integer test2;
	
		public BeanTest() {
		}
		
		public BeanTest(String test1) {
			this.test1 = test1;
		}
		
		public BeanTest(String test1, Integer test2) {
			this.test1 = test1;
			this.test2 = test2;
		}
	
		public BeanTest(Integer test2) {
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

		@Override
		public String toString() {
			return "BeanTest [test1=" + test1 + ", test2=" + test2 + "]";
		}
	
	}
	
	public static void main2(String[] args) {
		ApplicationContext applicationContext = new XmlApplicationContext("error-config4.xml");
		FoodService foodService = applicationContext.getBean("foodServiceErrorTest");
		System.out.println(foodService.getFood(null));
	}
	
	public static void main(String[] args) {
		System.out.println(BeanTest.class.getName());
		
		List<Constructor<?>> list = Arrays.asList(FoodRepositoryImpl.class.getConstructors());
		System.out.println(list.toString());
		
//		ApplicationContext applicationContext = new XmlApplicationContext("mixed-constructor.xml");
		FoodRepository foodRepository = applicationContext.getBean("foodRepository");
		System.out.println(foodRepository.getFood());
		
		BeanTest b = applicationContext.getBean("constructorTestBean");
		System.out.println(b.toString());
		
		FoodConstructorTestService service = applicationContext.getBean(FoodConstructorTestService.class);
		System.out.println(service.getFoodRepository().getFood().toString());
	}

	public static void main1(String[] args) throws Throwable {
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
