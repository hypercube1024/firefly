package test.ioc;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;

import org.junit.Assert;
import org.junit.Test;

import test.component.AddService;
import test.component.FieldInject;
import test.component.MethodInject;
import test.component2.MethodInject2;

import com.firefly.core.ApplicationContext;
import com.firefly.core.XmlApplicationContext;


public class TestAnnotationIoc {
//	private static Logger log = LoggerFactory.getLogger(TestAnnotationIoc.class);
	public static ApplicationContext applicationContext = new XmlApplicationContext("annotation-config.xml");
	
	@Test
	public void testFieldInject() {
		FieldInject fieldInject = applicationContext.getBean("fieldInject");
		Assert.assertThat(fieldInject.add(5, 4), is(9));
		Assert.assertThat(fieldInject.add2(5, 4), is(9));

		fieldInject = applicationContext.getBean(FieldInject.class);
		Assert.assertThat(fieldInject.add(5, 4), is(9));
		Assert.assertThat(fieldInject.add2(5, 4), is(9));
	}

	@Test
	public void testMethodInject() {
		MethodInject m = applicationContext.getBean("methodInject");
		Assert.assertThat(m.add(5, 4), is(9));
	}

	@Test
	public void testMethodInject2() {
		MethodInject2 m = applicationContext.getBean("methodInject2");
		Assert.assertThat(m.add(5, 5), is(10));
		Assert.assertThat(m.getNum(), is(3));
	}

	@Test
	public void testSingle() {
		AddService t = applicationContext.getBean("addService");
		t.getI();
		t.getI();
		Assert.assertThat(t.getI(), greaterThan(0));
	}
	
}
