package test.ioc;

import com.firefly.core.ApplicationContext;
import com.firefly.core.XmlApplicationContext;
import com.firefly.core.support.exception.BeanDefinitionParsingException;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import test.mixed.Food;
import test.mixed.FoodService;
import test.mixed.FoodService2;

import static org.hamcrest.Matchers.is;

public class TestMixIoc {
	private static Logger log = LoggerFactory.getLogger("firefly-system");
	public static ApplicationContext applicationContext = new XmlApplicationContext("mixed-config.xml");

	@Test
	public void testInject() {
		FoodService2 foodService2 = applicationContext.getBean("foodService2");
		Food food = foodService2.getFood("apple");
		log.debug(food.getName());
		Assert.assertThat(food.getPrice(), is(5.3));
		
		FoodService foodService = applicationContext.getBean(FoodService.class);
		food = foodService.getFood("strawberry");
		log.debug(food.getName());
		Assert.assertThat(food.getPrice(), is(10.00));
	}

	@Test(expected = BeanDefinitionParsingException.class)
	public void testErrorConfig1() {
		new XmlApplicationContext("error-config1.xml");
	}

	@Test(expected = BeanDefinitionParsingException.class)
	public void testErrorConfig2() {
		new XmlApplicationContext("error-config2.xml");
	}

	@Test(expected = BeanDefinitionParsingException.class)
	public void testErrorConfig3() {
		new XmlApplicationContext("error-config3.xml");
	}

	@Test(expected = BeanDefinitionParsingException.class)
	public void testErrorConfig4() {
		new XmlApplicationContext("error-config4.xml");
	}
	
	@Test(expected = BeanDefinitionParsingException.class)
	public void testErrorConfig5() {
		new XmlApplicationContext("error-config5.xml");
	}
}
