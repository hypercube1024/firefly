package test.controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.firefly.annotation.Controller;
import com.firefly.annotation.HttpParam;
import com.firefly.annotation.RequestMapping;
import com.firefly.mvc.web.HttpMethod;
import com.firefly.mvc.web.View;
import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;

@Controller
public class HelloController {
	private static Log log = LogFactory.getInstance().getLog("firefly-system");

	@RequestMapping(value = "/hello")
	public String index(HttpServletRequest request) {
		request.setAttribute("hello", "你好 firefly!");
		return "/index.jsp";
	}

	@RequestMapping(value = "/hello/text", view = View.TEXT)
	public String text(HttpServletRequest request) {
		log.info("into text output >>>>>>>>>>>>>>>>>");
		return "文本输出";
	}
	
	@RequestMapping(value = "/hello/redirect", view = View.REDIRECT)
	public String hello5(HttpServletRequest request,
			HttpServletResponse response) {
		return "/hello";
	}

	@RequestMapping(value = "/book/value")
	public String bookValue(HttpServletRequest request, @HttpParam Book book) {
		request.setAttribute("book", book);
		return "/book.jsp";
	}

	@RequestMapping(value = "/book/create", method = HttpMethod.POST)
	public String createBook(@HttpParam("book") Book book) {
		return "/book.jsp";
	}

	@RequestMapping(value = "/book/json", method = HttpMethod.POST, view = View.JSON)
	public Object getBook(@HttpParam("book") Book book) {
		return book;
	}
}
