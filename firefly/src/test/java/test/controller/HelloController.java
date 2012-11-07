package test.controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.firefly.annotation.Controller;
import com.firefly.annotation.HttpParam;
import com.firefly.annotation.PathVariable;
import com.firefly.annotation.RequestMapping;
import com.firefly.mvc.web.HttpMethod;
import com.firefly.mvc.web.View;
import com.firefly.mvc.web.view.JsonView;
import com.firefly.mvc.web.view.JspView;
import com.firefly.mvc.web.view.RedirectView;
import com.firefly.mvc.web.view.TextView;
import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;

@Controller
public class HelloController {
	private static Log log = LogFactory.getInstance().getLog("firefly-system");

	@RequestMapping(value = "/hello")
	public View index(HttpServletRequest request) {
		request.setAttribute("hello", "你好 firefly!");
		return new JspView("/index.jsp");
	}

	@RequestMapping(value = "/hello/text")
	public View text(HttpServletRequest request) {
		log.info("into text output >>>>>>>>>>>>>>>>>");
		return new TextView("文本输出");
	}
	
	@RequestMapping(value = "/hello/text-?/?-?")
	public View text2(HttpServletRequest request, @PathVariable String[] args) {
		return new TextView("text-" + args[0] + "-" + args[1] + "-" + args[2]);
	}
	
	@RequestMapping(value = "/hello?")
	public View text3(HttpServletRequest request, @PathVariable String[] args) {
		return new TextView("text-" + args[0]);
	}
	
	@RequestMapping(value = "/hello/redirect")
	public View hello5(HttpServletRequest request,
			HttpServletResponse response) {
		return new RedirectView("/hello");
	}

	@RequestMapping(value = "/book/value")
	public View bookValue(HttpServletRequest request, @HttpParam Book book) {
		request.setAttribute("book", book);
		return new JspView("/book.jsp");
	}

	@RequestMapping(value = "/book/create", method = HttpMethod.POST)
	public View createBook(@HttpParam("book") Book book) {
		return new JspView("/book.jsp");
	}

	@RequestMapping(value = "/book/json", method = HttpMethod.POST)
	public View getBook(@HttpParam("book") Book book) {
		return new JsonView(book);
	}
	
	@RequestMapping(value = "/book/testMethod", method = {HttpMethod.GET, HttpMethod.POST})
	public View testMethod(@HttpParam("book") Book book) {
		return new JsonView(book);
	}
}
