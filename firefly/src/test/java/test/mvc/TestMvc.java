package test.mvc;

import com.firefly.mvc.web.AnnotationWebContext;
import com.firefly.mvc.web.DispatcherController;
import com.firefly.mvc.web.HttpMethod;
import com.firefly.mvc.web.servlet.HttpServletDispatcherController;
import com.firefly.utils.json.Json;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import test.controller.Book;
import test.mixed.Food;
import test.mock.servlet.MockHttpServletRequest;
import test.mock.servlet.MockHttpServletResponse;

import static org.hamcrest.Matchers.*;

public class TestMvc {
	private static Logger log = LoggerFactory.getLogger("firefly-system");
	private static DispatcherController dispatcherController = new HttpServletDispatcherController(new AnnotationWebContext("firefly-mvc.xml"));
	
	public static void main(String[] args) {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setRequestURI("/firefly/app/book/testMethod/");
		request.setServletPath("/app");
		request.setContextPath("/firefly");
		request.setMethod(HttpMethod.GET);
		request.setParameter("title", "good book");
		request.setParameter("text", "一本好书");
		request.setParameter("id", "330");
		request.setParameter("price", "79.9");
		request.setParameter("sell", "true");
		MockHttpServletResponse response = new MockHttpServletResponse();
		dispatcherController.dispatch(request, response);
		
		System.out.println(response.getAsString());
		System.out.println(response.getHeader("Allow"));
	}
	
	@Test
	public void testAllowMethod() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setRequestURI("/firefly/app/book/testMethod/");
		request.setServletPath("/app");
		request.setContextPath("/firefly");
		request.setMethod(HttpMethod.PUT);
		request.setParameter("title", "good book");
		request.setParameter("text", "一本好书");
		request.setParameter("id", "330");
		request.setParameter("price", "79.9");
		request.setParameter("sell", "true");
		MockHttpServletResponse response = new MockHttpServletResponse();
		dispatcherController.dispatch(request, response);
		Assert.assertThat(response.getHeader("Allow"), is("POST,GET"));
//		System.out.println(response.getAsString());
//		System.out.println(response.getHeader("Allow"));
	}
	
	@Test
	public void testInterceptorChain() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setRequestURI("/firefly/app/food/view1");
		request.setServletPath("/app");
		request.setContextPath("/firefly");
		request.setMethod("GET");
		MockHttpServletResponse response = new MockHttpServletResponse();
		dispatcherController.dispatch(request, response);
		
		Food food = (Food)request.getAttribute("fruit0");
		Assert.assertThat(food.getName(), is("apple"));
		Assert.assertThat(food.getPrice(), is(8.0));
		
		food = (Food)request.getAttribute("fruit1");
		Assert.assertThat(food.getName(), is("ananas"));
		Assert.assertThat(food.getPrice(), is(4.99));
		
		food = Json.toObject(response.getAsString(), Food.class);
		Assert.assertThat(food.getName(), is("banana"));
		Assert.assertThat(food.getPrice(), is(3.99));
	}
	
	@Test
	public void testInterceptor() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setRequestURI("/firefly/app/food");
		request.setServletPath("/app");
		request.setContextPath("/firefly");
		request.setMethod("GET");
		MockHttpServletResponse response = new MockHttpServletResponse();
		dispatcherController.dispatch(request, response);
		
		Food food = (Food)request.getAttribute("fruit");
		Assert.assertThat(food.getName(), is("orange"));
		Assert.assertThat(food.getPrice(), is(3.5));
		
		food = (Food)request.getAttribute("fruit0");
		Assert.assertThat(food.getName(), is("apple"));
		Assert.assertThat(food.getPrice(), is(8.0));
		
		food = (Food)request.getAttribute("strawberry");
		Assert.assertThat(food.getName(), is("strawberry"));
		Assert.assertThat(food.getPrice(), is(10.0));
	}
	
	@Test
	public void testFoodAdd() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setRequestURI("/firefly/app/food/add");
		request.setServletPath("/app");
		request.setContextPath("/firefly");
		request.setMethod("POST");
		
		request.setParameter("name", "pineapple");
		request.setParameter("price", "79.9");
		
		MockHttpServletResponse response = new MockHttpServletResponse();
		dispatcherController.dispatch(request, response);
		
		Food food = (Food)request.getAttribute("foodFormInterceptor");
		Assert.assertThat(food.getName(), is("pineapple"));
		Assert.assertThat(food.getPrice(), is(79.9));
		
		Food food2 = (Food)request.getAttribute("foodForm");
		Assert.assertThat(food2.getName(), is("pineapple"));
		Assert.assertThat(food2.getPrice(), is(79.9));
	}
	
	@Test
	public void testNotFoundPage() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setRequestURI("/firefly/app/book/create00/");
		request.setServletPath("/app");
		request.setContextPath("/firefly");
		request.setMethod(HttpMethod.GET);
		request.setParameter("title", "good book");
		request.setParameter("text", "一本好书");
		request.setParameter("id", "330");
		request.setParameter("price", "79.9");
		request.setParameter("sell", "true");
		MockHttpServletResponse response = new MockHttpServletResponse();
		dispatcherController.dispatch(request, response);
		
		Assert.assertThat(response.getStatus(), is(404));
	}
	
	@Test
	public void testNotAllowMethod() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setRequestURI("/firefly/app/book/create/");
		request.setServletPath("/app");
		request.setContextPath("/firefly");
		request.setMethod(HttpMethod.GET);
		request.setParameter("title", "good book");
		request.setParameter("text", "一本好书");
		request.setParameter("id", "330");
		request.setParameter("price", "79.9");
		request.setParameter("sell", "true");
		MockHttpServletResponse response = new MockHttpServletResponse();
		dispatcherController.dispatch(request, response);
		
		Assert.assertThat(response.getHeader("Allow"), is("POST"));
		Assert.assertThat(request.getAttribute("book"), nullValue());
	}
	
	@Test
	public void testControllerHello() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setRequestURI("/firefly/app/hello");
		request.setServletPath("/app");
		request.setContextPath("/firefly");
		request.setMethod("GET");
		MockHttpServletResponse response = new MockHttpServletResponse();
		dispatcherController.dispatch(request, response);
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
		dispatcherController.dispatch(request, response);
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
		dispatcherController.dispatch(request, response);
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
		dispatcherController.dispatch(request, response);
		Assert.assertThat(response.getAsString(), is("文本输出"));
		
		request = new MockHttpServletRequest();
		request.setRequestURI("/firefly/app/hello/text-xo/333-444");
		request.setServletPath("/app");
		request.setContextPath("/firefly");
		request.setMethod("GET");
		response = new MockHttpServletResponse();
		dispatcherController.dispatch(request, response);
		Assert.assertThat(response.getAsString(), is("text-xo-333-444"));
		
		request = new MockHttpServletRequest();
		request.setRequestURI("/firefly/app/hello55555/");
		request.setServletPath("/app");
		request.setContextPath("/firefly");
		request.setMethod("GET");
		response = new MockHttpServletResponse();
		dispatcherController.dispatch(request, response);
		Assert.assertThat(response.getAsString(), is("text-55555"));
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
		dispatcherController.dispatch(request, response);
		log.info(response.getAsString());
		Assert.assertThat(response.getAsString().length(), greaterThan(10));
		Book book = Json.toObject(response.getAsString(), Book.class);
		Assert.assertThat(book.getId(), is(331));
		Assert.assertThat(book.getSell(), is(false));
		Assert.assertThat(book.getTitle(), is("good book"));
	}
	
	@Test
	public void testJsonOutput2() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setRequestURI("/firefly/app/book/json2");
		request.setServletPath("/app");
		request.setContextPath("/firefly");
		request.setMethod(HttpMethod.POST);
		request.setParameter("title", "good book aha");
		request.setParameter("text", "very good");
		request.setParameter("id", "2345");
		request.setParameter("price", "10.0");
		request.setParameter("sell", "false");
		MockHttpServletResponse response = new MockHttpServletResponse();
		dispatcherController.dispatch(request, response);
		log.info(response.getAsString());
		Assert.assertThat(response.getAsString().length(), greaterThan(10));
		Book book = Json.toObject(response.getAsString(), Book.class);
		Assert.assertThat(book.getId(), is(2345));
		Assert.assertThat(book.getSell(), is(false));
		Assert.assertThat(book.getTitle(), is("good book aha"));
	}
	
	@Test
	public void testPostJson() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setRequestURI("/firefly/app/book/insert");
		request.setServletPath("/app");
		request.setContextPath("/firefly");
		request.setMethod(HttpMethod.POST);
		
		Book post = new Book();
		post.setId(10);
		post.setPrice(3.3);
		post.setText("hello post json");
		request.setStringBody(Json.toJson(post));
		MockHttpServletResponse response = new MockHttpServletResponse();
		dispatcherController.dispatch(request, response);
		System.out.println(response.getAsString());
		
		Book book = Json.toObject(response.getAsString(), Book.class);
		Assert.assertThat(book.getId(), is(10));
		Assert.assertThat(book.getPrice(), is(3.3));
		Assert.assertThat(book.getText(), is("hello post json"));
	}

	@Test
	public void testRedirect() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setRequestURI("/firefly/app/hello/redirect");
		request.setServletPath("/app");
		request.setContextPath("/firefly");
		request.setMethod("GET");
		MockHttpServletResponse response = new MockHttpServletResponse();
		dispatcherController.dispatch(request, response);
		Assert.assertThat(response.getHeader("Location"), is("/firefly/app/hello"));
	}
}
