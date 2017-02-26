package test.ioc;

import com.firefly.$;
import com.firefly.core.ApplicationContext;
import com.firefly.core.XmlApplicationContext;
import com.firefly.utils.exception.CommonRuntimeException;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import test.component3.CollectionService;
import test.component3.MapService;
import test.component3.Person;
import test.component3.PersonService;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;

public class TestXmlIoc {
    private static Logger log = LoggerFactory.getLogger("firefly-system");

    @Test(expected = CommonRuntimeException.class)
    public void testFileNotFound() {
        new XmlApplicationContext("ssss.xml");
    }

    @Test
    public void testDefaultApplication() {
        Person person = $.getBean("person");
        Assert.assertThat(person.getAge(), is(12));
    }

    @Test
    public void testXmlInject() {
        Person person = $.getBean("person");
        Assert.assertThat(person.getName(), is("Jack"));
        PersonService personService = $.getBean("personService");
        Assert.assertThat(true, is(personService.isInitial()));

        List<Object> l = personService.getTestList();
        Assert.assertThat(l.size(), greaterThan(0));
        int i = 0;
        for (Object p : l) {
            if (p instanceof Person) {
                person = (Person) p;
                i++;
                log.debug(person.getName());
            } else if (p instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<Object, Object> map = (Map<Object, Object>) p;
                log.info(map.toString());
                Assert.assertThat(map.entrySet().size(), greaterThan(0));
                Assert.assertThat(map.get(2.2), is(3.3));
            } else {
                log.debug(p.toString());
            }
        }
        Assert.assertThat(i, greaterThan(1));
    }

    @Test
    public void testXmlLinkedListInject() {
        CollectionService collectionService = $.getBean("collectionService");
        List<Object> list = collectionService.getList();
        Assert.assertThat(list.size(), greaterThan(0));
        log.debug(list.toString());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testListInject() {
        // list的值也是list
        CollectionService collectionService = $.getBean("collectionService2");
        List<Object> list = collectionService.getList();
        Assert.assertThat(list.size(), greaterThan(2));
        Set<String> set = (Set<String>) list.get(2);
        Assert.assertThat(set.size(), is(2));
        log.debug(set.toString());

        // set赋值
        Set<Integer> set1 = collectionService.getSet();
        Assert.assertThat(set1.size(), is(2));
        log.debug(set1.toString());
    }

    @Test
    public void testArrayInject() {
        CollectionService collectionService = $.getBean("collectionService3");
        String[] strArray = collectionService.getStrArray();
        Assert.assertThat(strArray.length, greaterThan(0));
        log.debug(Arrays.toString(strArray));

        collectionService = $.getBean("collectionService4");
        int[] intArray = collectionService.getIntArray();
        Assert.assertThat(intArray.length, greaterThan(0));
        log.debug(Arrays.toString(intArray));

        collectionService = $.getBean("collectionService5");
        Object[] obj = collectionService.getObjArray();
        Assert.assertThat(obj.length, greaterThan(0));
        Object[] obj2 = (Object[]) obj[3];
        Assert.assertThat(obj2.length, greaterThan(0));
        Assert.assertThat(obj2[1], is(10000000000L));
    }

    @Test(expected = ClassCastException.class)
    public void testIdTypeError() {
        ApplicationContext context = new XmlApplicationContext("firefly2.xml");
        CollectionService collectionService = context.getBean("collectionService");
        for (Integer i : collectionService.getSet()) {
            i++;
        }
    }

    @Test
    public void testMapInject() {
        MapService mapService = $.getBean("mapService");
        Map<Object, Object> map = mapService.getMap();
        for (Entry<Object, Object> entry : map.entrySet()) {
            log.info(entry.getKey() + "\t" + entry.getValue());
            if (entry.getKey().getClass().isArray()) {
                Object[] objects = (Object[]) entry.getKey();
                log.info("array key [{}]", Arrays.toString(objects));
                Assert.assertThat(objects.length, greaterThan(0));
            }
        }
        Assert.assertThat(map.get(1).toString(), is("www"));
    }
}
