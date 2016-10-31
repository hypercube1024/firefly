package test.controller;

import com.firefly.annotation.*;
import com.firefly.mvc.web.HttpMethod;
import com.firefly.mvc.web.View;
import com.firefly.mvc.web.view.JsonView;
import com.firefly.mvc.web.view.JspView;
import com.firefly.mvc.web.view.RedirectView;
import com.firefly.mvc.web.view.TextView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Controller
public class HelloController {
	private static Logger log = LoggerFactory.getLogger("firefly-system");

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
	public View hello5(HttpServletRequest request, HttpServletResponse response) {
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

	@RequestMapping(value = "/book/json2", method = HttpMethod.POST)
	public View getBookJson(Book book) {
		return new JsonView(book);
	}

	@RequestMapping(value = "/book/insert", method = HttpMethod.POST)
	public View postBook(@JsonBody Book book) {
		return new JsonView(book);
	}

	@RequestMapping(value = "/book/testMethod", method = { HttpMethod.GET, HttpMethod.POST })
	public View testMethod(@HttpParam("book") Book book) {
		return new JsonView(book);
	}
}
