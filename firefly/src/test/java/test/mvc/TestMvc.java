package test.mvc;

import static org.hamcrest.Matchers.*;

import org.junit.Assert;
import org.junit.Test;
import test.controller.Book;
import test.mixed.Food;
import test.mock.servlet.MockHttpServletRequest;
import test.mock.servlet.MockHttpServletResponse;
import com.firefly.mvc.web.DispatcherController;
import com.firefly.mvc.web.HttpMethod;
import com.firefly.mvc.web.servlet.HttpServletDispatcherController;
import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;

public class TestMvc {
	private static Log log = LogFactory.getInstance().getLog("firefly-system");
	private static DispatcherController dispatcherController = new HttpServletDispatcherController("firefly-mvc.xml", null);

	@Test
	public void testControllerHello() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setRequestURI("/firefly/app/hello");
		request.setServletPath("/app");
		request.setContextPath("/firefly");
		request.setMethod("GET");
		MockHttpServletResponse response = new MockHttpServletResponse();
		dispatcherController.dispatcher(request, response);
		Assert.assertThat(request.getAttribute("hello").toString(),
				is("你好 firefly!"));
	}

	@Test
	public void testBeanParamInject() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setRequestURI("/firefly/app/book/value");
		request.setServletPath("/app");
		request.setContextPath("/firefly");
		request.setMethod("GET");
		request.setParameter("text", "ddd");
		request.setParameter("id", "345");
		request.setParameter("price", "23.3");
		MockHttpServletResponse response = new MockHttpServletResponse();
		dispatcherController.dispatcher(request, response);
		Book book = (Book) request.getAttribute("book");
		Assert.assertThat(book.getText(), is("ddd"));
		Assert.assertThat(book.getPrice(), is(23.3));
		Assert.assertThat(book.getId(), is(345));
	}

	@Test
	public void testPostBeanParamInject() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setRequestURI("/firefly/app/book/create/");
		request.setServletPath("/app");
		request.setContextPath("/firefly");
		request.setMethod(HttpMethod.POST);
		request.setParameter("title", "good book");
		request.setParameter("text", "一本好书");
		request.setParameter("id", "330");
		request.setParameter("price", "79.9");
		request.setParameter("sell", "true");
		MockHttpServletResponse response = new MockHttpServletResponse();
		dispatcherController.dispatcher(request, response);
		Book book = (Book) request.getAttribute("book");
		Assert.assertThat(book.getText(), is("一本好书"));
		Assert.assertThat(book.getPrice(), is(79.9));
		Assert.assertThat(book.getId(), is(330));
		Assert.assertThat(book.getSell(), is(true));
	}

	@Test
	public void testResponseOutput() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setRequestURI("/firefly/app/hello/text");
		request.setServletPath("/app");
		request.setContextPath("/firefly");
		request.setMethod("GET");
		MockHttpServletResponse response = new MockHttpServletResponse();
		dispatcherController.dispatcher(request, response);
		Assert.assertThat(response.getAsString(), is("文本输出"));
	}

	@Test
	public void testJsonOutput() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setRequestURI("/firefly/app/book/json/");
		request.setServletPath("/app");
		request.setContextPath("/firefly");
		request.setMethod(HttpMethod.POST);
		request.setParameter("title", "good book");
		request.setParameter("text", "very good");
		request.setParameter("id", "331");
		request.setParameter("price", "10.0");
		request.setParameter("sell", "false");
		MockHttpServletResponse response = new MockHttpServletResponse();
		dispatcherController.dispatcher(request, response);
		log.info(response.getAsString());
		Assert.assertThat(
				response.getAsString().length(),
				greaterThan(10));
	}

	@Test
	public void testRedirect() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setRequestURI("/firefly/app/hello/redirect");
		request.setServletPath("/app");
		request.setContextPath("/firefly");
		request.setMethod("GET");
		MockHttpServletResponse response = new MockHttpServletResponse();
		dispatcherController.dispatcher(request, response);
		log.info(response.getHeader("Location"));
		Assert.assertThat(response.getHeader("Location"),
				is("/firefly/app/hello"));
	}

	@Test
	public void testInterceptor() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setRequestURI("/firefly/app/food");
		request.setServletPath("/app");
		request.setContextPath("/firefly");
		request.setMethod("GET");
		MockHttpServletResponse response = new MockHttpServletResponse();
		dispatcherController.dispatcher(request, response);
		log.info(request.getDispatcherTarget());
		Food food = (Food) request.getAttribute("fruit");
		Assert.assertThat(food.getName(), is("orange"));
		String returnView = (String) request.getAttribute("returnView");
		Assert.assertThat(returnView, is("/food.jsp"));
		String str2 = (String) request.getAttribute("str2");
		Assert.assertThat(str2, nullValue());

	}

	@Test
	public void testInterceptorReturn() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setRequestURI("/firefly/app/food");
		request.setServletPath("/app");
		request.setContextPath("/firefly");
		request.setMethod("GET");
		request.setParameter("fruit", "apple");
		MockHttpServletResponse response = new MockHttpServletResponse();
		dispatcherController.dispatcher(request, response);
		log.info(request.getDispatcherTarget());
		Food food = (Food) request.getAttribute("fruit");
		Assert.assertThat(food.getName(), is("apple"));
		Assert.assertThat(food.getPrice(), is(5.3));
	}

	@Test
	public void testInterceptorChain() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setRequestURI("/firefly/app/food/view1");
		request.setServletPath("/app");
		request.setContextPath("/firefly");
		request.setMethod("GET");
		MockHttpServletResponse response = new MockHttpServletResponse();
		dispatcherController.dispatcher(request, response);
		log.info(request.getDispatcherTarget());
		Assert.assertThat(request.getDispatcherTarget(),
				is("/WEB-INF/page/foodView1.jsp"));
		Assert.assertThat(request.getAttribute("into").toString(), is("2"));
	}

	@Test
	public void testInterceptorChainReturn1() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setRequestURI("/firefly/app/food/view1");
		request.setServletPath("/app");
		request.setContextPath("/firefly");
		request.setMethod("GET");
		request.setParameter("strawberry", "strawberry");
		MockHttpServletResponse response = new MockHttpServletResponse();
		dispatcherController.dispatcher(request, response);
		log.info(request.getDispatcherTarget());
		Food food = (Food) request.getAttribute("fruit");
		Assert.assertThat(food.getName(), is("strawberry"));
		Assert.assertThat(food.getPrice(), is(10.00));
		Assert.assertThat(request.getDispatcherTarget(),
				is("/WEB-INF/page/foodView.jsp"));

	}

	@Test
	public void testInterceptorChainReturn() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setRequestURI("/firefly/app/food/view1");
		request.setServletPath("/app");
		request.setContextPath("/firefly");
		request.setMethod("GET");
		request.setParameter("fruit", "apple");
		MockHttpServletResponse response = new MockHttpServletResponse();
		dispatcherController.dispatcher(request, response);
		Food food = (Food) request.getAttribute("fruit");
		Assert.assertThat(food.getName(), is("apple"));
		Assert.assertThat(food.getPrice(), is(5.3));
		Assert.assertThat(request.getDispatcherTarget(),
				is("/WEB-INF/page/food.jsp"));
	}
}
