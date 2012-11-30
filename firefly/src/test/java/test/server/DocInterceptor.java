package test.server;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.firefly.annotation.Interceptor;
import com.firefly.mvc.web.HandlerChain;
import com.firefly.mvc.web.View;

@Interceptor(uri = "/document/*")
public class DocInterceptor {
	public View dispose(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) {
		System.out.println("start doc");
		View view = chain.doNext(request, response, chain);
		System.out.println("end doc");
		return view;
	}
}
