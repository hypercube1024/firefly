package test.mvc;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import org.junit.Assert;
import org.junit.Test;

import com.firefly.mvc.web.Resource;
import com.firefly.mvc.web.Resource.Result;

public class TestResource {
	
	@Test
	public void testResource() throws NoSuchMethodException, SecurityException {
		Resource resource = new Resource();
		resource.add("/user/id-?-?", null);
		resource.add("/user/id-?-?/?", null);
		resource.add("/user/add", null);
		
		resource.add("/shop/fruit/apple/?", null);
		resource.add("/shop/fruit/banana", null);
		
		Result ret = resource.match("/user/id-3344-2222/55555/");
		Assert.assertThat(ret.params.length, is(3));
		Assert.assertThat(ret.params[1], is("2222"));
		Assert.assertThat(ret.params[2], is("55555"));
		
		ret = resource.match("/shop/fruit/banana");
		Assert.assertThat(ret.params.length, is(0));
		
		ret = resource.match("/hello");
		Assert.assertThat(ret, nullValue());
	}
}
