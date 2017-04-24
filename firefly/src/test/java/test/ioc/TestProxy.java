package test.ioc;

import com.firefly.core.ApplicationContext;
import com.firefly.core.XmlApplicationContext;
import org.junit.Assert;
import org.junit.Test;
import test.proxy.NameService;

import static org.hamcrest.Matchers.is;

/**
 * @author Pengtao Qiu
 */
public class TestProxy {

    public static ApplicationContext ctx = new XmlApplicationContext("aop-test.xml");

    @Test
    public void test() {
        NameService nameService = ctx.getBean(NameService.class);
        String name = nameService.getName("hello");
        System.out.println(name);
        Assert.assertThat(name, is("name: (p2,(p1,(female->p3,(p4->fuck you(female->p3,(p2,(p1,hello,p1),p2),p3),p4->fuck you),p3),p1),p2)"));
    }
}
