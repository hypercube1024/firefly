package test.server;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.firefly.annotation.Interceptor;
import com.firefly.mvc.web.HandlerChain;
import com.firefly.mvc.web.View;
import com.firefly.mvc.web.view.StaticFileView;

@Interceptor(uri = "/add*")
public class AddInterceptor {
	public View dispose(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) {
		HttpSession session = request.getSession();
		String name = (String)session.getAttribute("name");
		if(name == null) {
			return new StaticFileView("/index.html");
		}
		
		return chain.doNext(request, response, chain);
	}
}
