package test.ioc;

import com.firefly.core.ApplicationContext;
import com.firefly.core.XmlApplicationContext;
import org.junit.Assert;
import org.junit.Test;
import test.component.FieldInject;
import test.component.MethodInject;
import test.component2.MethodInject2;

import static org.hamcrest.Matchers.is;


public class TestAnnotationIoc {

    private static ApplicationContext app = new XmlApplicationContext("annotation-config.xml");

    @Test
    public void testFieldInject() {
        FieldInject fieldInject = app.getBean("fieldInject");
        Assert.assertThat(fieldInject.add(5, 4), is(9));
        Assert.assertThat(fieldInject.add2(5, 4), is(9));

        fieldInject = app.getBean(FieldInject.class);
        Assert.assertThat(fieldInject.add(5, 4), is(9));
        Assert.assertThat(fieldInject.add2(5, 4), is(9));
    }

    @Test
    public void testMethodInject() {
        MethodInject m = app.getBean("methodInject");
        Assert.assertThat(m.add(5, 4), is(9));

        m = app.getBean(MethodInject.class);
        Assert.assertThat(m.add(5, 5), is(10));
    }

    @Test
    public void testMethodInject2() {
        MethodInject2 m = app.getBean("methodInject2");
        Assert.assertThat(m.add(5, 5), is(10));
        Assert.assertThat(m.getNum(), is(3));
        Assert.assertThat(true, is(m.isInitial()));
    }

}
