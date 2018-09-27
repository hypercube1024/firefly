package test.utils;

import static org.hamcrest.Matchers.is;

import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;

import org.junit.Assert;
import org.junit.Test;

import com.firefly.utils.ConvertUtils;

public class TestConvertUtils {

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Test
    public void testConvertArray() throws Exception {
        Collection collection = new ArrayList();
        collection.add("arr1");
        collection.add("arr2");
        Method method = TestConvertUtils.class.getMethod("setArray", String[].class);
        Object obj = ConvertUtils.convert(collection, method.getParameterTypes()[0]);
        Integer ret = (Integer) method.invoke(this, obj);
        Assert.assertThat(ret, is(2));
        Assert.assertThat(((String[]) obj)[1], is("arr2"));
    }

    @Test
    public void testAutoConvertLong() {
        Long x = ConvertUtils.convert("10000000000", "");
        Assert.assertThat(x, is(10000000000L));

        x = ConvertUtils.convert("10000000000", long.class);
        Assert.assertThat(x, is(10000000000L));

        x = ConvertUtils.convert("10000000000", "long");
        Assert.assertThat(x, is(10000000000L));
    }

    @Test
    public void testAutoConvert() {
        Integer a = ConvertUtils.convert("20", 3);
        Assert.assertThat(a, is(20));

        a = ConvertUtils.convert("xxxxx", 10);
        Assert.assertThat(a, is(10));

        Boolean b = ConvertUtils.convert("true", false);
        Assert.assertThat(b, is(true));

        b = ConvertUtils.convert("false", true);
        Assert.assertThat(b, is(false));

        b = ConvertUtils.convert("xxxxx", true);
        Assert.assertThat(b, is(false));
    }

    public int setArray(String[] arr) {
//		log.debug(Arrays.toString(arr));
        return arr.length;
    }

    public static void main(String[] args) throws URISyntaxException {
        boolean b = ConvertUtils.convert("xxxx", true);
        System.out.println(b);

//		Map<Object,Object> map = new HashMap<Object, Object>();
//		map.put("key", "value");
//		System.out.println(LogFactory.class.getClassLoader().getResource("firefly-log.properties").toURI());
    }
}
