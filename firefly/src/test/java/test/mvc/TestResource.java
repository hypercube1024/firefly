package test.mvc;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Assert;
import org.junit.Test;

import test.server.IndexController;

import com.firefly.mvc.web.Resource;
import com.firefly.mvc.web.Resource.Result;
import com.firefly.mvc.web.support.ControllerMetaInfo;

public class TestResource {
	
	@Test
	public void testResource() throws NoSuchMethodException, SecurityException {
		IndexController controller = new IndexController();
		Resource resource = new Resource("utf-8");
		ControllerMetaInfo cm = new ControllerMetaInfo(controller, 
				IndexController.class.getMethod("index4", HttpServletRequest.class, HttpServletResponse.class));
		
		resource.add("/user/id-?-?", cm);
		resource.add("/user/id-?-?/?", cm);
		resource.add("/user/add", cm);
		resource.add("/document/_?/?", cm);
		
		resource.add("/shop/fruit/apple/?", cm);
		resource.add("/shop/fruit/banana", cm);
		resource.add("/file/info.txt", cm);
		
		Result ret = resource.match("/user/id-3344-2222/55555");
		Assert.assertThat(ret.getParams().length, is(3));
		Assert.assertThat(ret.getParams()[1], is("2222"));
		Assert.assertThat(ret.getParams()[2], is("55555"));
		
		ret = resource.match("/shop/fruit/banana");
		Assert.assertThat(ret.getParams(), nullValue());
		
		ret = resource.match("/hello");
		Assert.assertThat(ret, nullValue());
		
		ret = resource.match("/file/info.txt");
		Assert.assertThat(ret, notNullValue());
		
		ret = resource.match("/document/_pengpeng");
		Assert.assertThat(ret, nullValue());
		
		Assert.assertThat(resource.getEncoding(), is("utf-8"));
	}
}
