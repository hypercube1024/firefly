package test.server;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Part;

import com.firefly.annotation.Controller;
import com.firefly.annotation.PathVariable;
import com.firefly.annotation.RequestMapping;
import com.firefly.mvc.web.HttpMethod;
import com.firefly.mvc.web.View;
import com.firefly.mvc.web.view.RedirectView;
import com.firefly.mvc.web.view.TemplateView;
import com.firefly.mvc.web.view.TextView;
import com.firefly.utils.io.FileUtils;
import com.firefly.utils.io.LineReaderHandler;

@Controller
public class IndexController {

	@RequestMapping(value = "/index")
	public View index(HttpServletRequest request, HttpServletResponse response) {
		System.out.println("into /index " + Thread.currentThread().getName());
		HttpSession session = request.getSession();
		request.setAttribute("hello", session.getAttribute("name"));
		response.addCookie(new Cookie("test", "cookie_value"));
		Cookie cookie = new Cookie("myname", "xiaoqiu");
		cookie.setMaxAge(5 * 60);
		response.addCookie(cookie);
		return new TemplateView("/index.html");
	}
	
	@RequestMapping(value = "/index-close")
	public View indexShort(HttpServletRequest request, HttpServletResponse response) {
		response.setHeader("Connection", "close");
		return new TemplateView("/index.html");
	}
	
	@RequestMapping(value = "/add", method = HttpMethod.POST)
	public View add(HttpServletRequest request, HttpServletResponse response) {
		System.out.println("into /add");
		return new TextView(request.getParameter("content"));
	}
	
	@RequestMapping(value = "/add2", method = HttpMethod.POST)
	public View add2(HttpServletRequest request, HttpServletResponse response) {
		System.out.println("into /add2");
		return new TextView("test add 2");
	}

	@RequestMapping(value = "/login")
	public View test(HttpServletRequest request, HttpServletResponse response) {
		HttpSession session = request.getSession();
		session.setMaxInactiveInterval(15);
		String name = (String)session.getAttribute("name");
		if(name == null) {
			System.out.println("name is null");
			name = "Qiu Pengtao";
			session.setAttribute("name", name);
		}
		request.setAttribute("name", name);
		return new TemplateView("/test.html");
	}
	
	@RequestMapping(value = "/exit")
	public View exit(HttpServletRequest request, HttpServletResponse response) {
		request.getSession().invalidate();
		request.setAttribute("name", "exit");
		return new TemplateView("/test.html");
	}

	@RequestMapping(value = "/index2")
	public View index2(HttpServletRequest request,
			HttpServletResponse response) throws IOException {
		response.sendRedirect("index");
		return null;
	}

	@RequestMapping(value = "/index3")
	public View index3(HttpServletRequest request,
			HttpServletResponse response) throws IOException {
		response.sendRedirect(request.getContextPath()
				+ request.getServletPath() + "/index");
		return null;
	}
	
	@RequestMapping(value = "/testc")
	public View testOutContentLength(HttpServletRequest request, HttpServletResponse response) throws IOException {
		String msg = "<html><body>test Content-Length output</body></html>";
		response.setCharacterEncoding("UTF-8");
		response.setHeader("Content-Type", "text/html; charset=UTF-8");
		response.setHeader("Content-Length", String.valueOf(msg.getBytes("UTF-8").length));
		PrintWriter writer = response.getWriter();
		try {
			writer.print(msg);
		} finally {
			writer.close();
		}
		return null;
	}

	@RequestMapping(value = "/index4")
	public View index4(HttpServletRequest request,
			HttpServletResponse response) throws IOException {
		return new RedirectView("/index");
	}
	
	@RequestMapping(value = "/document/?/?")
	public View document(HttpServletRequest request, @PathVariable String[] args) {
		System.out.println(Arrays.toString(args));
		request.setAttribute("info", args);
		return new TemplateView("/index.html");
	}
	
	@RequestMapping(value = "/param")
	public View testParam(HttpServletRequest request) {
		Map<String, String[]> map = request.getParameterMap();
		return new TextView(Arrays.toString(map.get("b")));
	}
	
	@RequestMapping(value = "/big")
	public View testBigData(HttpServletRequest request) {
		final StringBuilder json = new StringBuilder();
		try {
			FileUtils.read(new File("/Users/qiupengtao/develop/jsontest.txt"), new LineReaderHandler(){

				@Override
				public void readline(String text, int num) {
					json.append(text);
					
				}}, "UTF-8");
		} catch (IOException e) {
			e.printStackTrace();
		}
		String ret = json.toString();
		return new TextView(ret);
	}
	
	@RequestMapping(value = "/error")
	public View testError(HttpServletRequest request) {
		System.out.println("test error");
		throw new RuntimeException("test error");
	}
	
	@RequestMapping(value = "/upload", method=HttpMethod.POST)
	public View upload(HttpServletRequest request) throws IOException, ServletException {
		System.out.println(">>>>>>>>> upload start");
		for(Part part : request.getParts()) {
			System.out.println(part.getName() + "|" + part.getSize());
		}
//		throw new RuntimeException("upload error");
		return new TextView("upload ok!");
	}

}
