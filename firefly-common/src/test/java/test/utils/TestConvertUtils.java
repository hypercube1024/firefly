package test.utils;

import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import static org.hamcrest.Matchers.*;
import org.junit.Assert;
import org.junit.Test;
import com.firefly.utils.ConvertUtils;
import com.firefly.utils.log.LogFactory;

public class TestConvertUtils {

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Test
	public void testConvertArray() throws Exception {
		Collection collection = new ArrayList();
		collection.add("arr1");
		collection.add("arr2");
		Method method = TestConvertUtils.class.getMethod("setArray", String[].class);
		Object obj = ConvertUtils.convert(collection, method.getParameterTypes()[0]);
		Integer ret = (Integer)method.invoke(this, obj);
		Assert.assertThat(ret, is(2));
		Assert.assertThat(((String[])obj)[1], is("arr2"));
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

	public int setArray(String[] arr) {
//		log.debug(Arrays.toString(arr));
		return arr.length;
	}

	public static void main(String[] args) throws URISyntaxException {
		System.out.println(LogFactory.class.getClassLoader().getResource("firefly-log.properties").toURI());
	}
}
