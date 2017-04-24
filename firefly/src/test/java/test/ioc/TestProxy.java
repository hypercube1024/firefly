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
        System.out.println(nameService.getName("hello"));
        Assert.assertThat(nameService.getName("hello"), is("name: (p2,(p1,(p3,(p3,(p2,(p1,hello,p1),p2),p3),p3),p1),p2)"));
    }
}
