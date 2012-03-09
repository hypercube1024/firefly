package test.server;

import java.io.IOException;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.firefly.annotation.Controller;
import com.firefly.annotation.RequestMapping;
import com.firefly.mvc.web.HttpMethod;
import com.firefly.mvc.web.View;

@Controller
public class IndexController {

	@RequestMapping(value = "/index")
	public String index(HttpServletRequest request, HttpServletResponse response) {
		HttpSession session = request.getSession();
		request.setAttribute("hello", session.getAttribute("name"));
		response.addCookie(new Cookie("test", "cookie_value"));
		Cookie cookie = new Cookie("myname", "xiaoqiu");
		cookie.setMaxAge(5 * 60);
		response.addCookie(cookie);
		return "/index.html";
	}
	
	@RequestMapping(value = "/add", method = HttpMethod.POST)
	public String add(HttpServletRequest request, HttpServletResponse response) {
		return "/index.html";
	}

	@RequestMapping(value = "/login")
	public String test(HttpServletRequest request, HttpServletResponse response) {
		HttpSession session = request.getSession();
		session.setMaxInactiveInterval(15);
		String name = (String)session.getAttribute("name");
		if(name == null) {
			System.out.println("name is null");
			name = "Qiu Pengtao";
			session.setAttribute("name", name);
		}
		request.setAttribute("name", name);
		return "/test.html";
	}
	
	@RequestMapping(value = "/exit")
	public String exit(HttpServletRequest request, HttpServletResponse response) {
		request.getSession().invalidate();
		request.setAttribute("name", "exit");
		return "/test.html";
	}

	@RequestMapping(value = "/index2")
	public String index2(HttpServletRequest request,
			HttpServletResponse response) throws IOException {
		response.sendRedirect("index");
		return null;
	}

	@RequestMapping(value = "/index3")
	public String index3(HttpServletRequest request,
			HttpServletResponse response) throws IOException {
		response.sendRedirect(request.getContextPath()
				+ request.getServletPath() + "/index");
		return null;
	}

	@RequestMapping(value = "/index4", view = View.REDIRECT)
	public String index4(HttpServletRequest request,
			HttpServletResponse response) throws IOException {
		return "/index";
	}

}
