package test.server.async;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.firefly.annotation.Interceptor;
import com.firefly.mvc.web.HandlerChain;
import com.firefly.mvc.web.View;

@Interceptor(uri = "/async/*")
public class AsyncInterceptor {
	public View dispose(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) {
		PrintWriter writer = null;
		try {
			writer = response.getWriter();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		writer.println("#start async interceptor");
		writer.flush();
		View view = chain.doNext(request, response, chain);
		writer.println("#end async interceptor");
		writer.flush();
		return view;
	}
}
