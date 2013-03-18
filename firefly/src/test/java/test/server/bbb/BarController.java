package test.server.bbb;

import javax.servlet.http.HttpServletRequest;

import com.firefly.annotation.Controller;
import com.firefly.annotation.RequestMapping;
import com.firefly.mvc.web.View;
import com.firefly.mvc.web.view.TemplateView;

@Controller
public class BarController {

	@RequestMapping("/bar")
	public View bar(HttpServletRequest request) {
		request.setAttribute("name", "hello bar!");
		return new TemplateView("/test.html");
	}
}
